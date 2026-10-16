package com.deploytool.service.build;

import com.deploytool.common.DeployException;
import com.deploytool.model.BuildResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 调用本地 Maven 打包。核心坑点处理：
 * 1. 必须同时消费 stdout 与 stderr（各自独立线程），否则缓冲区写满导致进程假死；
 * 2. 通过 {@link #abort()} 强制销毁子进程实现终止；
 * 3. Windows 下用 cmd /c、类 Unix 用 sh -c 承载整条命令字符串。
 * 业务层，无 JavaFX 依赖。
 */
public class MavenBuildService implements BuildService {

    private volatile Process process;
    private final AtomicBoolean aborted = new AtomicBoolean(false);

    @Override
    public BuildResult build(Path projectDir, String command, Consumer<String> logSink) {
        aborted.set(false);
        File workDir = projectDir.toFile();
        if (!workDir.isDirectory()) {
            throw new DeployException("项目目录不存在：" + projectDir);
        }

        ProcessBuilder pb = new ProcessBuilder(wrapShellCommand(command));
        pb.directory(workDir);
        pb.redirectErrorStream(false); // 分别读取 stdout/stderr

        try {
            process = pb.start();
            Process running = process;

            Thread outThread = pipe(running.getInputStream(), logSink);
            Thread errThread = pipe(running.getErrorStream(), logSink);
            outThread.start();
            errThread.start();

            int exitCode = running.waitFor();
            outThread.join();
            errThread.join();

            if (aborted.get()) {
                return BuildResult.fail(exitCode);
            }
            BuildResult basic = new BuildResult(exitCode == 0, exitCode, null);
            if (!basic.isSuccess()) {
                return basic;
            }
            // 成功则识别产物
            return JarLocator.locate(projectDir)
                    .map(jar -> BuildResult.ok(exitCode, jar))
                    .orElseGet(() -> new BuildResult(true, exitCode, null));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DeployException("执行打包命令失败：" + e.getMessage(), e);
        } finally {
            process = null;
        }
    }

    @Override
    public void abort() {
        aborted.set(true);
        Process p = process;
        if (p != null) {
            p.destroyForcibly();
        }
    }

    /** 命令字符串按平台包装为可执行数组。 */
    private static String[] wrapShellCommand(String command) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new String[]{"cmd.exe", "/c", command};
        }
        return new String[]{"/bin/sh", "-c", command};
    }

    private Thread pipe(InputStream in, Consumer<String> sink) {
        Thread t = new Thread(() -> {
            Charset cs = Charset.defaultCharset();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, cs))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sink.accept(line);
                }
            } catch (IOException ignored) {
                // 进程被销毁时流读取异常属正常，忽略
            }
        });
        t.setDaemon(true);
        return t;
    }
}

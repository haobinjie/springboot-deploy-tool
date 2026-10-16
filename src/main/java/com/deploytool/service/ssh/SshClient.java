package com.deploytool.service.ssh;

import com.deploytool.common.DeployException;
import com.deploytool.model.ExecutionResult;
import com.deploytool.model.ServerConfig;
import com.deploytool.service.config.CryptoUtil;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * SSHJ 连接封装：负责认证、保持连接、执行远程命令并回显输出。
 * STOP/START 复用同一连接避免重复握手。业务层，无 JavaFX 依赖。
 */
public class SshClient implements AutoCloseable {

    private final net.schmizz.sshj.SSHClient ssh;
    private boolean connected;

    public SshClient() {
        this.ssh = new net.schmizz.sshj.SSHClient(new DefaultConfig());
    }

    public void connect(ServerConfig cfg) {
        try {
            // 骨架实现跳过主机密钥校验；正式版应改为 KnownHosts 严格校验
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.setConnectTimeout(15_000); // 毫秒
            ssh.setTimeout(30_000);        // 毫秒
            ssh.connect(cfg.getHost(), cfg.getPort());

            switch (cfg.getAuthType()) {
                case PASSWORD -> ssh.authPassword(cfg.getUsername(), CryptoUtil.decrypt(cfg.getPassword()));
                case KEY -> throw new DeployException("密钥登录将在 V1.1 提供");
            }
            connected = true;
        } catch (UserAuthException e) {
            throw new DeployException("SSH 认证失败，请检查账号/密码：" + e.getMessage(), e);
        } catch (IOException e) {
            throw new DeployException("SSH 连接失败：" + cfg.getHost() + ":" + cfg.getPort(), e);
        }
    }

    /**
     * 执行远程命令，逐行回调标准输出，返回聚合结果。
     * 阻塞调用，须在后台线程执行。stderr 由独立线程消费，避免缓冲区写满导致死锁。
     */
    public ExecutionResult exec(String command, Consumer<String> stdOutSink, int timeoutSec) {
        ensureConnected();
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(command);

            // 独立线程消费 stderr，与主线程消费 stdout 并行，防止管道缓冲区写满卡死
            Thread errThread = consumeStream(cmd.getErrorStream(), line -> {
                synchronized (stderr) {
                    stderr.append(line).append(System.lineSeparator());
                }
                if (stdOutSink != null) stdOutSink.accept("[stderr] " + line);
            });
            errThread.start();

            consumeStreamSync(cmd.getInputStream(), line -> {
                stdout.append(line).append(System.lineSeparator());
                if (stdOutSink != null) stdOutSink.accept(line);
            });

            cmd.join(timeoutSec, TimeUnit.SECONDS);
            errThread.join(2000);

            Integer exitStatus = cmd.getExitStatus();
            int exit = exitStatus != null ? exitStatus : -1;
            return new ExecutionResult(exit, stdout.toString(), stderr.toString());
        } catch (IOException e) {
            throw new DeployException("远程命令执行失败：" + command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeployException("远程命令执行被中断：" + command, e);
        }
    }

    private void consumeStreamSync(InputStream in, Consumer<String> sink) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sink.accept(line);
            }
        } catch (IOException ignored) {
            // 流关闭属正常结束
        }
    }

    private Thread consumeStream(InputStream in, Consumer<String> sink) {
        Thread t = new Thread(() -> consumeStreamSync(in, sink));
        t.setDaemon(true);
        return t;
    }

    public boolean isConnected() {
        return connected && ssh.isConnected();
    }

    /** 暴露底层连接给 SFTP 上传器复用。 */
    public net.schmizz.sshj.SSHClient raw() {
        ensureConnected();
        return ssh;
    }

    private void ensureConnected() {
        if (!connected) {
            throw new DeployException("SSH 尚未连接");
        }
    }

    @Override
    public void close() {
        try {
            if (ssh != null && ssh.isConnected()) {
                ssh.disconnect();
            }
        } catch (IOException ignored) {
            // 关闭异常不影响主流程
        } finally {
            connected = false;
        }
    }
}

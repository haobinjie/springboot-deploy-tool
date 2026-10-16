package com.deploytool.service.build;

import com.deploytool.common.DeployException;
import com.deploytool.model.BuildResult;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * 本地打包能力抽象。V1.0 实现为 {@link MavenBuildService}，Gradle 预留 V2.0。
 * 该方法为阻塞调用，必须由后台 Task 线程执行，严禁在 JavaFX App 线程直接调用。
 */
public interface BuildService {

    /**
     * @param projectDir 项目根目录（含 pom.xml）
     * @param command    打包命令，如 {@code mvn clean package -DskipTests}
     * @param logSink    逐行输出回调（stdout/stderr 合并），用于实时刷新日志面板
     * @return 打包结果（含自动识别的 jar 路径）
     */
    BuildResult build(Path projectDir, String command, Consumer<String> logSink) throws DeployException;

    /** 终止正在运行的打包进程（供 Task.cancel 触发）。 */
    void abort();
}

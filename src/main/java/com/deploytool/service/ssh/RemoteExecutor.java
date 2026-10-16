package com.deploytool.service.ssh;

import com.deploytool.model.ExecutionResult;
import com.deploytool.model.ServerConfig;

import java.util.function.Consumer;

/**
 * 远程脚本执行器：在已建立的 SSH 连接上执行 start/stop 脚本。
 * 执行前尝试赋予可执行权限（chmod +x），失败不影响主流程。
 * 业务层，无 JavaFX 依赖。
 */
public class RemoteExecutor {

    private final SshClient client;

    public RemoteExecutor(SshClient client) {
        this.client = client;
    }

    public ExecutionResult runStop(ServerConfig cfg, Consumer<String> logSink) {
        return runScript(cfg.getStopScript(), cfg, logSink);
    }

    public ExecutionResult runStart(ServerConfig cfg, Consumer<String> logSink) {
        return runScript(cfg.getStartScript(), cfg, logSink);
    }

    public ExecutionResult runScript(String scriptPath, ServerConfig cfg, Consumer<String> logSink) {
        if (scriptPath == null || scriptPath.isBlank()) {
            throw new com.deploytool.common.DeployException("脚本路径未配置");
        }
        // 保证可执行权限（幂等，失败忽略）
        client.exec("chmod +x " + scriptPath, null, cfg.getCommandTimeoutSec());
        return client.exec(scriptPath, logSink, cfg.getCommandTimeoutSec());
    }

    /** 连通性测试：执行一条极轻命令验证账号与网络。 */
    public boolean testConnection(ServerConfig cfg) {
        ExecutionResult r = client.exec("echo deploy-tool-ok", null, cfg.getCommandTimeoutSec());
        return r.isSuccess() && r.getStdout().contains("deploy-tool-ok");
    }
}

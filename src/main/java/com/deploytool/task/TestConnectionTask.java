package com.deploytool.task;

import com.deploytool.AppContext;
import com.deploytool.model.ServerConfig;
import com.deploytool.model.enums.LogCategory;
import com.deploytool.service.ssh.RemoteExecutor;
import com.deploytool.service.ssh.SshClient;
import javafx.concurrent.Task;

/**
 * SSH 连通性测试后台任务：建立连接并执行一条轻命令验证账号/网络。
 */
public class TestConnectionTask extends Task<Boolean> {

    private final AppContext ctx;
    private final ServerConfig cfg;

    public TestConnectionTask(AppContext ctx, ServerConfig cfg) {
        this.ctx = ctx;
        this.cfg = cfg;
    }

    @Override
    protected Boolean call() {
        updateMessage("正在连接 " + cfg.getHost() + ":" + cfg.getPort() + " …");
        try (SshClient client = new SshClient()) {
            client.connect(cfg);
            boolean ok = new RemoteExecutor(client).testConnection(cfg);
            if (ok) {
                ctx.logService().success(LogCategory.SYSTEM, "连接成功：" + cfg.getHost());
            } else {
                ctx.logService().error(LogCategory.SYSTEM, "连接校验未通过");
            }
            return ok;
        } catch (Exception e) {
            ctx.logService().error(LogCategory.SYSTEM, "连接失败：" + e.getMessage());
            return false;
        }
    }
}

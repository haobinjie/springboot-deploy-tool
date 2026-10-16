package com.deploytool.task;

import com.deploytool.AppContext;
import com.deploytool.model.ExecutionResult;
import com.deploytool.model.ServerConfig;
import com.deploytool.model.enums.LogCategory;
import com.deploytool.service.ssh.RemoteExecutor;
import com.deploytool.service.ssh.SshClient;
import javafx.concurrent.Task;

/**
 * 单条远程脚本执行后台任务（停止/启动），独立建立连接并回显输出。
 */
public class RemoteCmdTask extends Task<ExecutionResult> {

    public enum Op { STOP, START }

    private final AppContext ctx;
    private final ServerConfig cfg;
    private final Op op;
    private volatile SshClient client;

    public RemoteCmdTask(AppContext ctx, ServerConfig cfg, Op op) {
        this.ctx = ctx;
        this.cfg = cfg;
        this.op = op;
    }

    @Override
    protected ExecutionResult call() {
        updateMessage(op == Op.STOP ? "执行停止脚本…" : "执行启动脚本…");
        client = new SshClient();
        client.connect(cfg);
        try {
            RemoteExecutor executor = new RemoteExecutor(client);
            ctx.logService().info(LogCategory.REMOTE_SSH,
                    (op == Op.STOP ? "停止：" : "启动：")
                            + (op == Op.STOP ? cfg.getStopScript() : cfg.getStartScript()));
            ExecutionResult r = op == Op.STOP
                    ? executor.runStop(cfg, line -> ctx.logService().info(LogCategory.REMOTE_SSH, line))
                    : executor.runStart(cfg, line -> ctx.logService().info(LogCategory.REMOTE_SSH, line));
            if (r.isSuccess()) {
                ctx.logService().success(LogCategory.REMOTE_SSH, "脚本执行完成，退出码 0");
            } else {
                ctx.logService().error(LogCategory.REMOTE_SSH, "脚本执行失败，退出码 " + r.getExitCode());
            }
            return r;
        } finally {
            client.close();
        }
    }

    @Override
    protected void cancelled() {
        if (client != null) client.close();
    }

    @Override
    protected void failed() {
        Throwable e = getException();
        ctx.logService().error(LogCategory.REMOTE_SSH, "远程执行异常：" + (e == null ? "未知" : e.getMessage()));
    }
}

package com.deploytool.task;

import com.deploytool.AppContext;
import com.deploytool.model.BuildResult;
import com.deploytool.model.enums.LogCategory;
import com.deploytool.service.build.BuildService;
import javafx.concurrent.Task;

import java.nio.file.Path;

/**
 * 本地打包后台任务。call() 在后台线程执行 Maven，逐行输出经 LogService 广播；
 * 取消时调用 {@link BuildService#abort()} 强杀子进程。
 */
public class BuildTask extends Task<BuildResult> {

    private final AppContext ctx;
    private final Path projectDir;
    private final String command;
    private final BuildService buildService;

    public BuildTask(AppContext ctx, Path projectDir, String command) {
        this.ctx = ctx;
        this.projectDir = projectDir;
        this.command = command;
        this.buildService = ctx.newBuildService();
    }

    @Override
    protected BuildResult call() {
        updateMessage("开始打包：" + command);
        BuildResult result = buildService.build(projectDir, command, line -> {
            ctx.logService().info(LogCategory.BUILD_LOCAL, line);
        });
        if (result.isSuccess()) {
            ctx.logService().success(LogCategory.BUILD_LOCAL,
                    "打包成功，产物：" + (result.getJarPath() == null ? "未识别到jar" : result.getJarPath()));
        } else {
            ctx.logService().error(LogCategory.BUILD_LOCAL, "打包失败，退出码：" + result.getExitCode());
        }
        return result;
    }

    @Override
    protected void cancelled() {
        buildService.abort();
        ctx.logService().warn(LogCategory.BUILD_LOCAL, "打包已终止");
    }

    @Override
    protected void failed() {
        Throwable e = getException();
        ctx.logService().error(LogCategory.BUILD_LOCAL, "打包异常：" + (e == null ? "未知" : e.getMessage()));
    }
}

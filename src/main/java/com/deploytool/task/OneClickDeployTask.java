package com.deploytool.task;

import com.deploytool.AppContext;
import com.deploytool.common.DeployException;
import com.deploytool.model.BuildResult;
import com.deploytool.model.DeployOutcome;
import com.deploytool.model.ExecutionResult;
import com.deploytool.model.ServerConfig;
import com.deploytool.model.enums.DeployStep;
import com.deploytool.model.enums.LogCategory;
import com.deploytool.service.build.BuildService;
import com.deploytool.service.ssh.RemoteExecutor;
import com.deploytool.service.ssh.SftpUploader;
import com.deploytool.service.ssh.SshClient;
import javafx.concurrent.Task;

import java.nio.file.Path;

/**
 * 一键部署编排任务：打包 → 识别Jar → 上传 → 停止 → 启动。
 * 全流程在单条后台线程串行执行；任一步失败立即中断并返回失败步骤。
 * STOP/START/UPLOAD 复用同一条 SSH 连接。取消时中断当前 BuildService/关闭连接。
 */
public class OneClickDeployTask extends Task<DeployOutcome> {

    private final AppContext ctx;
    private final Path projectDir;
    private final String buildCommand;
    private final ServerConfig server;

    private BuildService buildService;
    private volatile SshClient sshClient;

    public OneClickDeployTask(AppContext ctx, Path projectDir, String buildCommand, ServerConfig server) {
        this.ctx = ctx;
        this.projectDir = projectDir;
        this.buildCommand = buildCommand;
        this.server = server;
    }

    @Override
    protected DeployOutcome call() {
        // 5 步，每步占 20% 进度
        updateProgress(0, 100);
        try {
            // 步骤1：本地打包
            currentStep = DeployStep.BUILD;
            stepBanner(DeployStep.BUILD, 1);
            buildService = ctx.newBuildService();
            BuildResult build = buildService.build(projectDir, buildCommand,
                    line -> ctx.logService().info(LogCategory.BUILD_LOCAL, line));
            if (isCancelled()) return cancelOutcome();
            if (!build.isSuccess()) {
                return fail(DeployStep.BUILD, "Maven 打包失败，退出码 " + build.getExitCode());
            }

            // 步骤2：识别 jar
            currentStep = DeployStep.LOCATE_JAR;
            stepBanner(DeployStep.LOCATE_JAR, 2);
            Path jar = build.getJarPath();
            if (jar == null) {
                return fail(DeployStep.LOCATE_JAR, "未在 target 目录识别到可部署的 jar 包");
            }
            updateProgress(20, 100);

            // 建立并复用 SSH 连接
            sshClient = new SshClient();
            sshClient.connect(server);

            // 步骤3：SFTP 上传
            currentStep = DeployStep.UPLOAD;
            stepBanner(DeployStep.UPLOAD, 3);
            String remoteName = jar.getFileName().toString();
            SftpUploader uploader = new SftpUploader(sshClient,
                    ctx.configService().getGlobalConfig().getUploadBufferSizeKb());
            String remotePath = remoteJoin(server.getRemoteDir(), remoteName);
            if (uploader.remoteExists(remotePath)) {
                ctx.logService().warn(LogCategory.UPLOAD_SFTP, "远程已存在同名 jar，将覆盖：" + remotePath);
            }
            uploader.upload(jar, server.getRemoteDir(), remoteName, p -> { /* 一键模式合并进度 */ });
            if (isCancelled()) return cancelOutcome();
            ctx.logService().success(LogCategory.UPLOAD_SFTP, "上传完成：" + remoteName);
            updateProgress(40, 100);

            // 步骤4：执行停止脚本（可选，失败不阻断启动）
            currentStep = DeployStep.STOP;
            stepBanner(DeployStep.STOP, 4);
            RemoteExecutor executor = new RemoteExecutor(sshClient);
            if (notBlank(server.getStopScript())) {
                try {
                    executor.runStop(server, line -> ctx.logService().info(LogCategory.REMOTE_SSH, line));
                } catch (DeployException e) {
                    ctx.logService().warn(LogCategory.REMOTE_SSH, "停止脚本执行异常（继续启动）：" + e.getMessage());
                }
            } else {
                ctx.logService().info(LogCategory.REMOTE_SSH, "未配置停止脚本，跳过");
            }
            updateProgress(60, 100);

            // 步骤5：执行启动脚本
            currentStep = DeployStep.START;
            stepBanner(DeployStep.START, 5);
            if (!notBlank(server.getStartScript())) {
                return fail(DeployStep.START, "未配置启动脚本");
            }
            ExecutionResult start = executor.runStart(server,
                    line -> ctx.logService().info(LogCategory.REMOTE_SSH, line));
            if (!start.isSuccess()) {
                return fail(DeployStep.START, "启动脚本执行失败，退出码 " + start.getExitCode());
            }
            updateProgress(100, 100);
            ctx.logService().success(LogCategory.SYSTEM, "一键部署完成 ✅");
            return DeployOutcome.success();

        } catch (DeployException e) {
            return fail(currentStep, e.getMessage());
        } catch (Exception e) {
            return fail(currentStep, "未知错误：" + e.getMessage());
        } finally {
            closeQuietly();
        }
    }

    private volatile DeployStep currentStep = DeployStep.BUILD;

    private void stepBanner(DeployStep step, int index) {
        String msg = String.format("[%d/5] %s", index, step.label());
        updateMessage(msg);
        ctx.logService().info(LogCategory.SYSTEM, "==== " + msg + " ====");
    }

    private DeployOutcome fail(DeployStep step, String message) {
        ctx.logService().error(LogCategory.SYSTEM, "流程在【" + step.label() + "】失败：" + message);
        return DeployOutcome.failure(step, message);
    }

    private DeployOutcome cancelOutcome() {
        return fail(currentStep == null ? DeployStep.BUILD : currentStep, "用户取消");
    }

    @Override
    protected void cancelled() {
        if (buildService != null) buildService.abort();
        closeQuietly();
        ctx.logService().warn(LogCategory.SYSTEM, "一键部署已取消");
    }

    private void closeQuietly() {
        if (sshClient != null) {
            sshClient.close();
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String remoteJoin(String dir, String name) {
        if (dir == null || dir.isEmpty()) return name;
        return dir.endsWith("/") ? dir + name : dir + "/" + name;
    }
}

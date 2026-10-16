package com.deploytool.task;

import com.deploytool.AppContext;
import com.deploytool.model.ServerConfig;
import com.deploytool.model.enums.LogCategory;
import com.deploytool.service.ssh.SftpUploader;
import com.deploytool.service.ssh.SshClient;
import javafx.concurrent.Task;

import java.nio.file.Path;

/**
 * SFTP 上传后台任务。独立建立/关闭 SSH 连接，进度经 updateProgress 回流 UI 进度条。
 */
public class UploadTask extends Task<Void> {

    private final AppContext ctx;
    private final ServerConfig cfg;
    private final Path localJar;
    private final String remoteFileName;

    private volatile SshClient client;

    public UploadTask(AppContext ctx, ServerConfig cfg, Path localJar) {
        this.ctx = ctx;
        this.cfg = cfg;
        this.localJar = localJar;
        this.remoteFileName = localJar.getFileName().toString();
    }

    @Override
    protected Void call() throws Exception {
        updateProgress(0, 1);
        client = new SshClient();
        client.connect(cfg);
        try {
            SftpUploader uploader = new SftpUploader(client,
                    ctx.configService().getGlobalConfig().getUploadBufferSizeKb());

            ctx.logService().info(LogCategory.UPLOAD_SFTP, "开始上传 " + remoteFileName + " → " + cfg.getRemoteDir());
            uploader.upload(localJar, cfg.getRemoteDir(), remoteFileName, progress -> {
                updateProgress(progress.getTransferredBytes(), progress.getTotalBytes());
                updateMessage(String.format("上传中 %d%%  (%.1f KB/s)",
                        progress.percent(), progress.getBytesPerSecond() / 1024));
            });
            updateProgress(1, 1);
            ctx.logService().success(LogCategory.UPLOAD_SFTP, "上传完成：" + remoteFileName);
            return null;
        } finally {
            client.close();
        }
    }

    @Override
    protected void cancelled() {
        if (client != null) client.close();
        ctx.logService().warn(LogCategory.UPLOAD_SFTP, "上传已取消");
    }

    @Override
    protected void failed() {
        Throwable e = getException();
        ctx.logService().error(LogCategory.UPLOAD_SFTP, "上传失败：" + (e == null ? "未知" : e.getMessage()));
    }
}

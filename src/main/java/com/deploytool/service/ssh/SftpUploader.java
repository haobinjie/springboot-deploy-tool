package com.deploytool.service.ssh;

import com.deploytool.common.DeployException;
import com.deploytool.model.UploadProgress;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.function.Consumer;

/**
 * SFTP 上传器：复用 {@link SshClient} 的连接，分块写入并回调进度。
 * 阻塞调用，须在后台线程执行。业务层，无 JavaFX 依赖。
 */
public class SftpUploader {

    private final SSHClient ssh;
    private final int bufferBytes;

    public SftpUploader(SshClient client, int bufferSizeKb) {
        this.ssh = client.raw();
        this.bufferBytes = Math.max(4, bufferSizeKb) * 1024;
    }

    /** 探测远程是否已存在同名文件（覆盖确认用）。 */
    public boolean remoteExists(String remotePath) {
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            return sftp.statExistence(remotePath) != null;
        } catch (IOException e) {
            throw new DeployException("探测远程文件失败：" + remotePath, e);
        }
    }

    /**
     * 上传本地 jar 到远程目录。
     * @param progressSink 进度回调（已传字节、总字节、速度）
     */
    public void upload(Path localJar, String remoteDir, String remoteFileName,
                       Consumer<UploadProgress> progressSink) {
        String remotePath = joinPath(remoteDir, remoteFileName);
        long total;
        try {
            total = Files.size(localJar);
        } catch (IOException e) {
            throw new DeployException("读取本地 Jar 失败：" + localJar, e);
        }

        try (SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.mkdirs(remoteDir);
            long start = System.currentTimeMillis();
            try (InputStream in = new BufferedInputStream(Files.newInputStream(localJar));
                 RemoteFile rf = sftp.open(remotePath,
                         EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC))) {
                byte[] buf = new byte[bufferBytes];
                long transferred = 0;
                int n;
                while ((n = in.read(buf)) > 0) {
                    rf.write(transferred, buf, 0, n);
                    transferred += n;
                    if (progressSink != null) {
                        double elapsedSec = Math.max(0.001, (System.currentTimeMillis() - start) / 1000.0);
                        progressSink.accept(new UploadProgress(transferred, total, transferred / elapsedSec));
                    }
                }
            }
        } catch (IOException e) {
            throw new DeployException("SFTP 上传失败：" + remotePath, e);
        }
    }

    private static String joinPath(String dir, String name) {
        if (dir == null || dir.isEmpty()) return name;
        return dir.endsWith("/") ? dir + name : dir + "/" + name;
    }
}

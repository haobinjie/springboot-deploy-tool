package com.deploytool.model;

/**
 * SFTP 上传进度快照，供 UI 进度条与速度展示。
 */
public class UploadProgress {

    private final long transferredBytes;
    private final long totalBytes;
    private final double bytesPerSecond;

    public UploadProgress(long transferredBytes, long totalBytes, double bytesPerSecond) {
        this.transferredBytes = transferredBytes;
        this.totalBytes = totalBytes;
        this.bytesPerSecond = bytesPerSecond;
    }

    public long getTransferredBytes() { return transferredBytes; }
    public long getTotalBytes() { return totalBytes; }
    public double getBytesPerSecond() { return bytesPerSecond; }

    /** 0.0 ~ 1.0，totalBytes<=0 时返回 0。 */
    public double ratio() {
        return totalBytes > 0 ? (double) transferredBytes / totalBytes : 0d;
    }

    public int percent() {
        return (int) Math.round(ratio() * 100);
    }
}

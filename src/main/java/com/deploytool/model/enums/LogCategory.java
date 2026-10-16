package com.deploytool.model.enums;

/**
 * 日志来源分类，对应日志面板四个区域标签。
 */
public enum LogCategory {
    BUILD_LOCAL("本地打包日志"),
    UPLOAD_SFTP("SFTP上传日志"),
    REMOTE_SSH("远程SSH执行日志"),
    SYSTEM("系统日志");

    private final String label;

    LogCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

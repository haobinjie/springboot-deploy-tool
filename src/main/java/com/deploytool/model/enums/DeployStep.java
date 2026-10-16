package com.deploytool.model.enums;

/**
 * 一键部署流程阶段，用于状态机与失败定位。
 */
public enum DeployStep {
    BUILD("本地打包"),
    LOCATE_JAR("识别Jar包"),
    UPLOAD("SFTP上传"),
    STOP("执行停止脚本"),
    START("执行启动脚本");

    private final String label;

    DeployStep(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

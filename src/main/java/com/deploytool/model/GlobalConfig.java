package com.deploytool.model;

import com.deploytool.model.enums.BuildType;

/**
 * global.json 结构：全局偏好设置。纯 DTO，无 JavaFX 依赖。
 */
public class GlobalConfig {

    private String version = "1.0";
    private String projectDir = "";                       // 上次选择的项目根目录
    private String buildCommand = "mvn clean package -DskipTests"; // 默认打包命令
    private BuildType buildType = BuildType.MAVEN;        // V2.0 预留 GRADLE
    private boolean autoDetectJar = true;                 // 打包后自动扫描 target
    private boolean logColorEnabled = true;
    private int uploadBufferSizeKb = 32;                  // SFTP 上传缓冲粒度
    private String aesKeyRef = "local";                   // V1.1 加密密钥来源标识

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getProjectDir() { return projectDir; }
    public void setProjectDir(String projectDir) { this.projectDir = projectDir; }

    public String getBuildCommand() { return buildCommand; }
    public void setBuildCommand(String buildCommand) { this.buildCommand = buildCommand; }

    public BuildType getBuildType() { return buildType; }
    public void setBuildType(BuildType buildType) { this.buildType = buildType; }

    public boolean isAutoDetectJar() { return autoDetectJar; }
    public void setAutoDetectJar(boolean autoDetectJar) { this.autoDetectJar = autoDetectJar; }

    public boolean isLogColorEnabled() { return logColorEnabled; }
    public void setLogColorEnabled(boolean logColorEnabled) { this.logColorEnabled = logColorEnabled; }

    public int getUploadBufferSizeKb() { return uploadBufferSizeKb; }
    public void setUploadBufferSizeKb(int uploadBufferSizeKb) { this.uploadBufferSizeKb = uploadBufferSizeKb; }

    public String getAesKeyRef() { return aesKeyRef; }
    public void setAesKeyRef(String aesKeyRef) { this.aesKeyRef = aesKeyRef; }
}

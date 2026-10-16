package com.deploytool.model;

import com.deploytool.model.enums.AuthType;

/**
 * 单套服务器环境配置（对应 servers.json 中 servers[] 元素）。
 * 纯 DTO，无 JavaFX 依赖。字段与 {@code servers.json} 一一对应。
 */
public class ServerConfig {

    private String id;                 // UUID 主键，重命名不影响引用
    private String name;               // 展示名
    private String host;
    private int port = 22;
    private AuthType authType = AuthType.PASSWORD;
    private String username;
    private String password;           // V1.0 明文；V1.1 起带 "AES:" 前缀密文
    private String privateKeyPath = "";
    private String passphrase = "";
    private String remoteDir;          // Jar 上传目标目录
    private String startScript;
    private String stopScript;
    private int commandTimeoutSec = 60;
    private String remark = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public AuthType getAuthType() { return authType; }
    public void setAuthType(AuthType authType) { this.authType = authType; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }

    public String getPassphrase() { return passphrase; }
    public void setPassphrase(String passphrase) { this.passphrase = passphrase; }

    public String getRemoteDir() { return remoteDir; }
    public void setRemoteDir(String remoteDir) { this.remoteDir = remoteDir; }

    public String getStartScript() { return startScript; }
    public void setStartScript(String startScript) { this.startScript = startScript; }

    public String getStopScript() { return stopScript; }
    public void setStopScript(String stopScript) { this.stopScript = stopScript; }

    public int getCommandTimeoutSec() { return commandTimeoutSec; }
    public void setCommandTimeoutSec(int commandTimeoutSec) { this.commandTimeoutSec = commandTimeoutSec; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    /** 服务器地址展示（下拉框文本）。 */
    public String display() {
        return String.format("%s (%s:%d)", name, host, port);
    }

    @Override
    public String toString() {
        return display();
    }
}

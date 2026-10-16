package com.deploytool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * servers.json 顶层容器，承载版本迁移字段与选中项引用。
 */
public class ServerConfigStore {

    private String version = "1.0";
    private String selectedServerId;
    private List<ServerConfig> servers = new ArrayList<>();

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getSelectedServerId() { return selectedServerId; }
    public void setSelectedServerId(String selectedServerId) { this.selectedServerId = selectedServerId; }

    public List<ServerConfig> getServers() { return servers; }
    public void setServers(List<ServerConfig> servers) { this.servers = servers; }
}

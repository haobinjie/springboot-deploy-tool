package com.deploytool.service.config;

import com.deploytool.common.DeployException;
import com.deploytool.common.JsonUtil;
import com.deploytool.model.GlobalConfig;
import com.deploytool.model.ServerConfig;
import com.deploytool.model.ServerConfigStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * 配置持久化服务：加载/保存 servers.json 与 global.json，管理服务器 CRUD。
 * 配置文件位于程序运行目录下的 config/ 目录（exe 同目录）。业务层，无 JavaFX 依赖。
 */
public class ConfigService {

    private static final String CONFIG_DIR = "config";
    private static final String SERVERS_FILE = "servers.json";
    private static final String GLOBAL_FILE = "global.json";

    private final Path configDir;
    private ServerConfigStore serverStore = new ServerConfigStore();
    private GlobalConfig globalConfig = new GlobalConfig();

    public ConfigService() {
        this(resolveProgramDir().resolve(CONFIG_DIR));
    }

    public ConfigService(Path configDir) {
        this.configDir = configDir;
    }

    /**
     * 解析“程序所在目录”，使配置文件位置不受启动时工作目录影响：
     * <ul>
     *   <li>打包为 jpackage app-image 时：类从 &lt;imageRoot&gt;/app/*.jar 加载，向上取到 exe 同级的 imageRoot；</li>
     *   <li>开发模式（从 target/classes 目录加载）：退回 user.dir（项目根目录），与仓库内 config/ 一致。</li>
     * </ul>
     */
    private static Path resolveProgramDir() {
        try {
            Path codeLocation = Paths.get(ConfigService.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isDirectory(codeLocation)) {
                // 开发：classes 目录，使用工作目录（项目根）
                return Paths.get(System.getProperty("user.dir"));
            }
            // 打包：jar 位于 imageRoot/app/xxx.jar，配置放 imageRoot（与 exe 同级）
            Path appDir = codeLocation.getParent();
            Path imageRoot = appDir == null ? null : appDir.getParent();
            return imageRoot != null ? imageRoot : Paths.get(System.getProperty("user.dir"));
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.dir"));
        }
    }

    /** 软件启动时调用：确保目录存在并加载配置（缺失则初始化默认值）。 */
    public void load() {
        try {
            Files.createDirectories(configDir);
            Path servers = configDir.resolve(SERVERS_FILE);
            if (Files.exists(servers)) {
                serverStore = JsonUtil.read(servers, ServerConfigStore.class);
            }
            Path global = configDir.resolve(GLOBAL_FILE);
            if (Files.exists(global)) {
                globalConfig = JsonUtil.read(global, GlobalConfig.class);
            }
        } catch (IOException e) {
            throw new DeployException("加载配置文件失败：" + e.getMessage(), e);
        }
    }

    public void saveServers() {
        write(configDir.resolve(SERVERS_FILE), serverStore);
    }

    public void saveGlobal() {
        write(configDir.resolve(GLOBAL_FILE), globalConfig);
    }

    private void write(Path file, Object value) {
        try {
            Files.createDirectories(configDir);
            JsonUtil.write(file, value);
        } catch (IOException e) {
            throw new DeployException("保存配置文件失败：" + file, e);
        }
    }

    // ===== 服务器 CRUD =====

    public java.util.List<ServerConfig> listServers() {
        return serverStore.getServers();
    }

    public Optional<ServerConfig> findSelected() {
        String id = serverStore.getSelectedServerId();
        return serverStore.getServers().stream()
                .filter(s -> s.getId() != null && s.getId().equals(id))
                .findFirst();
    }

    public void select(String id) {
        serverStore.setSelectedServerId(id);
        saveServers();
    }

    /** 新增或更新（按 id 判断）；自动补齐 UUID。返回持久化后的对象。 */
    public ServerConfig upsert(ServerConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
            serverStore.getServers().add(config);
        } else {
            serverStore.getServers().removeIf(s -> s.getId().equals(config.getId()));
            serverStore.getServers().add(config);
        }
        saveServers();
        return config;
    }

    public void delete(String id) {
        serverStore.getServers().removeIf(s -> s.getId().equals(id));
        if (id != null && id.equals(serverStore.getSelectedServerId())) {
            serverStore.setSelectedServerId(null);
        }
        saveServers();
    }

    public GlobalConfig getGlobalConfig() { return globalConfig; }
    public ServerConfigStore getServerStore() { return serverStore; }
    public Path getConfigDir() { return configDir; }
}

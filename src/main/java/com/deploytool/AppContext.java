package com.deploytool;

import com.deploytool.service.build.BuildService;
import com.deploytool.service.build.MavenBuildService;
import com.deploytool.service.config.ConfigService;
import com.deploytool.service.log.LogService;

/**
 * 应用级依赖容器（轻量手动装配）：集中持有各 Service，供 Controller/Task 获取。
 * 保持 Service 单实例，便于配置读写与日志订阅统一管理。
 */
public class AppContext {

    private final ConfigService configService;
    private final LogService logService;

    public AppContext(ConfigService configService, LogService logService) {
        this.configService = configService;
        this.logService = logService;
    }

    public static AppContext createDefault() {
        ConfigService cfg = new ConfigService();
        cfg.load();
        return new AppContext(cfg, LogService.getInstance());
    }

    public ConfigService configService() { return configService; }
    public LogService logService() { return logService; }

    /** 每次打包新建，避免进程状态在多次构建间串用。 */
    public BuildService newBuildService() {
        return new MavenBuildService();
    }
}

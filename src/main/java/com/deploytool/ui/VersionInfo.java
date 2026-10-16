package com.deploytool.ui;

import java.io.InputStream;
import java.util.Properties;

/**
 * 版本信息读取：从 classpath 的 app-version.properties 加载，缺失时给默认值。
 */
public final class VersionInfo {

    private static final String FALLBACK = "1.0.0";
    private static volatile String cached;

    private VersionInfo() {
    }

    public static String current() {
        if (cached != null) return cached;
        try (InputStream in = VersionInfo.class.getResourceAsStream("/app-version.properties")) {
            Properties p = new Properties();
            if (in != null) {
                p.load(in);
                cached = p.getProperty("version", FALLBACK);
            } else {
                cached = FALLBACK;
            }
        } catch (Exception e) {
            cached = FALLBACK;
        }
        return cached;
    }
}

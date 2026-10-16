package com.deploytool;

/**
 * 非模块化打包（fat-jar）主入口。
 * 独立于 App（继承 Application），避免 "JavaFX runtime components are missing" 检查，
 * 通过普通 main 启动，供 shade 生成的 jar 与 jpackage 使用。
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}

package com.deploytool.model;

import java.nio.file.Path;

/**
 * 本地打包结果。
 */
public class BuildResult {

    private final boolean success;
    private final int exitCode;
    private final Path jarPath;   // 识别到的 jar（未识别则为 null）

    public BuildResult(boolean success, int exitCode, Path jarPath) {
        this.success = success;
        this.exitCode = exitCode;
        this.jarPath = jarPath;
    }

    public static BuildResult ok(int exitCode, Path jarPath) {
        return new BuildResult(true, exitCode, jarPath);
    }

    public static BuildResult fail(int exitCode) {
        return new BuildResult(false, exitCode, null);
    }

    public boolean isSuccess() { return success; }
    public int getExitCode() { return exitCode; }
    public Path getJarPath() { return jarPath; }
}

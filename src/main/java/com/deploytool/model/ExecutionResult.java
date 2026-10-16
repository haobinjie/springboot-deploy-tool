package com.deploytool.model;

/**
 * 远程命令执行结果。
 */
public class ExecutionResult {

    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public ExecutionResult(int exitCode, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public int getExitCode() { return exitCode; }
    public String getStdout() { return stdout; }
    public String getStderr() { return stderr; }
    public boolean isSuccess() { return exitCode == 0; }
}

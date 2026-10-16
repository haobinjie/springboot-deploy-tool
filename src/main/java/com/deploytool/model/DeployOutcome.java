package com.deploytool.model;

import com.deploytool.model.enums.DeployStep;

/**
 * 一键部署最终结果。
 */
public class DeployOutcome {

    private final boolean success;
    private final DeployStep failedStep;   // 成功时为 null
    private final String errorMessage;

    private DeployOutcome(boolean success, DeployStep failedStep, String errorMessage) {
        this.success = success;
        this.failedStep = failedStep;
        this.errorMessage = errorMessage;
    }

    public static DeployOutcome success() {
        return new DeployOutcome(true, null, null);
    }

    public static DeployOutcome failure(DeployStep step, String message) {
        return new DeployOutcome(false, step, message);
    }

    public boolean isSuccess() { return success; }
    public DeployStep getFailedStep() { return failedStep; }
    public String getErrorMessage() { return errorMessage; }
}

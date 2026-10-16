package com.deploytool.common;

/**
 * 轻量级业务异常，携带面向用户的友好提示。
 */
public class DeployException extends RuntimeException {

    public DeployException(String message) {
        super(message);
    }

    public DeployException(String message, Throwable cause) {
        super(message, cause);
    }
}

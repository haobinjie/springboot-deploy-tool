package com.deploytool.common;

import java.util.function.Consumer;

/**
 * 统一返回封装：成功携带 value，失败携带错误信息与原始异常。
 * 供 Controller 层在不抛异常的前提下判断流程结果。
 */
public final class Result<T> {

    private final boolean success;
    private final T value;
    private final String errorMessage;
    private final Throwable cause;

    private Result(boolean success, T value, String errorMessage, Throwable cause) {
        this.success = success;
        this.value = value;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    public static <T> Result<T> ok(T value) {
        return new Result<>(true, value, null, null);
    }

    public static <T> Result<T> fail(String message, Throwable cause) {
        return new Result<>(false, null, message, cause);
    }

    public static <T> Result<T> fail(String message) {
        return fail(message, null);
    }

    public boolean isSuccess() { return success; }
    public T getValue() { return value; }
    public String getErrorMessage() { return errorMessage; }
    public Throwable getCause() { return cause; }

    public Result<T> ifSuccess(Consumer<T> action) {
        if (success && value != null) action.accept(value);
        return this;
    }
}

package com.deploytool.service.log;

import com.deploytool.model.enums.LogCategory;
import com.deploytool.model.enums.LogLevel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 日志聚合服务（业务层，无 JavaFX 依赖）。
 * 通过订阅者模式把 {@link LogEvent} 广播给监听方；UI 侧订阅后自行用
 * Platform.runLater 渲染，实现业务层与界面解耦、可脱离 GUI 单测。
 */
public class LogService {

    private static final LogService INSTANCE = new LogService();

    private final List<Consumer<LogEvent>> listeners = new CopyOnWriteArrayList<>();

    private LogService() {
    }

    public static LogService getInstance() {
        return INSTANCE;
    }

    /** 订阅日志事件（返回的取消句柄可用于退订）。 */
    public Runnable subscribe(Consumer<LogEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void info(LogCategory category, String message) {
        emit(category, LogLevel.INFO, message);
    }

    public void success(LogCategory category, String message) {
        emit(category, LogLevel.SUCCESS, message);
    }

    public void warn(LogCategory category, String message) {
        emit(category, LogLevel.WARN, message);
    }

    public void error(LogCategory category, String message) {
        emit(category, LogLevel.ERROR, message);
    }

    public void emit(LogCategory category, LogLevel level, String message) {
        LogEvent event = new LogEvent(category, level, System.currentTimeMillis(), message);
        for (Consumer<LogEvent> l : listeners) {
            try {
                l.accept(event);
            } catch (Exception ignored) {
                // 单个监听器异常不得影响其他订阅者与主流程
            }
        }
    }
}

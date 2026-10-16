package com.deploytool.service.log;

import com.deploytool.model.enums.LogCategory;
import com.deploytool.model.enums.LogLevel;

/**
 * 不可变日志事件，业务层通过监听器广播给 UI（UI 侧用 Platform.runLater 渲染）。
 */
public final class LogEvent {

    private final LogCategory category;
    private final LogLevel level;
    private final long timestamp;
    private final String message;

    public LogEvent(LogCategory category, LogLevel level, long timestamp, String message) {
        this.category = category;
        this.level = level;
        this.timestamp = timestamp;
        this.message = message;
    }

    public LogCategory getCategory() { return category; }
    public LogLevel getLevel() { return level; }
    public long getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
}

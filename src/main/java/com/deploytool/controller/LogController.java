package com.deploytool.controller;

import com.deploytool.AppContext;
import com.deploytool.ui.component.LogPanel;
import javafx.application.Platform;

/**
 * 日志控制器：订阅业务层 LogService，把事件安全切回 FX 线程渲染到 LogPanel。
 * 这是"业务层不碰 UI、UI 不阻塞"之间的桥接点。
 */
public class LogController {

    private final LogPanel logPanel;
    private Runnable unsubscribe;

    public LogController(AppContext ctx, LogPanel logPanel) {
        this.logPanel = logPanel;
        // 业务线程 emit → 这里在任意线程收到，必须 Platform.runLater 更新 UI
        this.unsubscribe = ctx.logService().subscribe(event ->
                Platform.runLater(() -> this.logPanel.append(event)));
    }

    public LogPanel getLogPanel() {
        return logPanel;
    }

    /** 窗口关闭时退订，避免监听器泄漏。 */
    public void dispose() {
        if (unsubscribe != null) {
            unsubscribe.run();
            unsubscribe = null;
        }
    }
}

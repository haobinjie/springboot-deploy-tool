package com.deploytool.task;

import javafx.concurrent.Task;
import javafx.concurrent.Worker;

/**
 * 在后台守护线程上启动 JavaFX Task。
 * Task.call() 运行于该线程；UI 更新通过 updateMessage/updateProgress
 * 及 Task 的 onSucceeded/onFailed 回调（自动切回 FX 线程）完成。
 */
public final class TaskRunner {

    private TaskRunner() {
    }

    public static Thread run(Task<?> task) {
        Thread t = new Thread(task, "fx-task");
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * 在任务到达终态（成功/失败/取消）时执行回调。
     * 回调在 FX 线程触发（stateProperty 变更由 Worker 内部投递到 FX 线程）。
     */
    public static void onFinished(Task<?> task, Runnable action) {
        task.stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED
                    || newState == Worker.State.FAILED
                    || newState == Worker.State.CANCELLED) {
                action.run();
            }
        });
    }
}

package com.deploytool.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 后台任务线程池：打包/上传/SSH 等阻塞 IO 统一在此执行，严禁占用 JavaFX App 线程。
 */
public final class ThreadPools {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    private static final ExecutorService BG = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "deploy-bg-" + SEQ.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    private ThreadPools() {
    }

    public static ExecutorService io() {
        return BG;
    }
}

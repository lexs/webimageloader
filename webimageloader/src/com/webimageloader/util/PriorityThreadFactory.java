package com.webimageloader.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Process;

public class PriorityThreadFactory implements ThreadFactory {
    private String name;
    private int priority;

    private AtomicInteger count = new AtomicInteger(1);

    public PriorityThreadFactory(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, name + " #" + count.getAndIncrement()) {
            @Override
            public void run() {
                Process.setThreadPriority(priority);

                super.run();
            }
        };
    }
}

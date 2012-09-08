package com.webimageloader.loader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.webimageloader.util.PriorityThreadFactory;

public abstract class SimpleBackgroundLoader extends BackgroundLoader {
    public SimpleBackgroundLoader(String name, int priority, int threadCount) {
        super(createExecutor(name, priority, threadCount));
    }

    private static ExecutorService createExecutor(String name, int priority, int threadCount) {
        return Executors.newFixedThreadPool(threadCount, new PriorityThreadFactory(name, priority));
    }
}

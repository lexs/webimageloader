package com.webimageloader.concurrent;

import com.webimageloader.loader.LoaderRequest;
import com.webimageloader.loader.LoaderWork;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ExecutorHelper {
    private ExecutorService executor;

    public ExecutorHelper(ExecutorService executor) {
        this.executor = executor;
    }

    public void run(LoaderRequest request, LoaderWork.Manager manager, ListenerFuture.Task task) {
        Future<?> future = executor.submit(new ListenerFuture(task, manager));
        manager.addFuture(future);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}

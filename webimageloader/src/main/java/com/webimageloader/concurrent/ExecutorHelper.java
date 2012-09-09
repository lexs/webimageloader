package com.webimageloader.concurrent;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.webimageloader.loader.LoaderRequest;
import com.webimageloader.loader.Loader.Listener;

public class ExecutorHelper {
    private Map<LoaderRequest, Future<?>> futures;

    private ExecutorService executor;

    public ExecutorHelper(ExecutorService executor) {
        this.executor = executor;

        futures = Collections.synchronizedMap(new WeakHashMap<LoaderRequest, Future<?>>());
    }

    public void run(LoaderRequest request, Listener listener, ListenerFuture.Task task) {
        Future<?> future = executor.submit(new ListenerFuture(task, listener));
        futures.put(request, future);
    }

    public void cancel(LoaderRequest request) {
        Future<?> future = futures.remove(request);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}

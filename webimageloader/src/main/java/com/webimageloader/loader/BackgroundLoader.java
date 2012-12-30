package com.webimageloader.loader;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.webimageloader.util.ListenerFuture;

public abstract class BackgroundLoader implements Loader, Closeable {
    private ExecutorService executor;

    public BackgroundLoader(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public final void load(final LoaderWork.Manager manager, final LoaderRequest request) {
        run(manager, new ListenerFuture.Task() {
            @Override
            public void run() throws Exception {
                loadInBackground(manager, request);
            }
        });
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    protected void run(LoaderWork.Manager manager, ListenerFuture.Task task) {
        Future<?> future = executor.submit(new ListenerFuture(task, manager));
        manager.addFuture(future);
    }

    protected abstract void loadInBackground(LoaderWork.Manager manager, LoaderRequest request) throws Exception;
}

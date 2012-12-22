package com.webimageloader.loader;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;

import com.webimageloader.concurrent.ExecutorHelper;
import com.webimageloader.concurrent.ListenerFuture;

public abstract class BackgroundLoader implements Loader, Closeable {
    private ExecutorHelper executorHelper;

    public BackgroundLoader(ExecutorService executor) {
        executorHelper = new ExecutorHelper(executor);
    }

    @Override
    public final void load(LoaderWork.Manager manager, final LoaderRequest request) {
        executorHelper.run(request, manager, new ListenerFuture.Task() {
            @Override
            public void run(LoaderWork.Manager manager) throws Exception {
                loadInBackground(manager, request);
            }
        });
    }

    @Override
    public final void cancel(LoaderRequest request) {
    }

    @Override
    public void close() {
        executorHelper.shutdown();
    }

    protected void run(LoaderRequest request, LoaderWork.Manager manager, ListenerFuture.Task task) {
        executorHelper.run(request, manager, task);
    }

    protected abstract void loadInBackground(LoaderWork.Manager manager, LoaderRequest request) throws Exception;
}

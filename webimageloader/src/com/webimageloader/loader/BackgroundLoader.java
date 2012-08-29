package com.webimageloader.loader;

import java.io.Closeable;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import com.webimageloader.concurrent.ExecutorHelper;
import com.webimageloader.concurrent.ListenerFuture;

public abstract class BackgroundLoader implements Loader, Closeable {
    private ExecutorHelper executorHelper;

    public BackgroundLoader(int workerThreadCount) {
        executorHelper = new ExecutorHelper(createExecutor(workerThreadCount));
    }

    @Override
    public final void load(final LoaderRequest request, final Iterator<Loader> chain, Listener listener) {
        executorHelper.run(request, listener, new ListenerFuture.Task() {
            @Override
            public void run(Listener listener) throws Exception {
                loadInBackground(request, chain, listener);
            }
        });
    }

    @Override
    public final void cancel(LoaderRequest request) {
        executorHelper.cancel(request);
    }

    @Override
    public void close() {
        executorHelper.shutdown();
    }

    protected void run(LoaderRequest request, Listener listener, ListenerFuture.Task task) {
        executorHelper.run(request, listener, task);
    }

    protected abstract ExecutorService createExecutor(int workerThreads);
    protected abstract void loadInBackground(LoaderRequest request, Iterator<Loader> chain, Listener listener) throws Exception;
}

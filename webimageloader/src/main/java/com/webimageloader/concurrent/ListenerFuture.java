package com.webimageloader.concurrent;

import com.webimageloader.loader.LoaderWork;

public class ListenerFuture implements Runnable {
    public interface Task {
        void run(LoaderWork.Manager manager) throws Exception;
    }

    private Task task;
    private LoaderWork.Manager manager;

    public ListenerFuture(Task task, LoaderWork.Manager manager) {
        this.task = task;
        this.manager = manager;
    }

    @Override
    public void run() {
        try {
            task.run(manager);
        } catch (Throwable t) {
            manager.deliverError(t);
        }
    }
}

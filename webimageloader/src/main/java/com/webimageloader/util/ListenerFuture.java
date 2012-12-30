package com.webimageloader.util;

import com.webimageloader.loader.LoaderWork;

public class ListenerFuture implements Runnable {
    public interface Task {
        void run() throws Exception;
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
            task.run();
        } catch (Throwable t) {
            manager.deliverError(t);
        }
    }
}

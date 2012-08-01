package com.webimageloader.concurrent;

import com.webimageloader.loader.Loader.Listener;

public class ListenerFuture implements Runnable {
    public interface Task {
        void run(Listener listener) throws Exception;
    }

    private Task task;
    private Listener listener;

    public ListenerFuture(Task task, Listener listener) {
        this.task = task;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            task.run(listener);
        } catch (Exception e) {
            listener.onError(e);
        }
    }
}

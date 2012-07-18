package se.alexanderblom.imageloader.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import se.alexanderblom.imageloader.loader.Loader.Listener;
import android.util.Log;

public class CallbackFuture extends FutureTask<Void> {
    private static final String TAG = "CallbackFuture";

    public interface Task {
        void run(Listener listener) throws Exception;
    }

    private Listener listener;

    public CallbackFuture(Task task, Listener listener) {
        super(new WrapperCallable(task, listener));

        this.listener = listener;
    }

    @Override
    protected void done() {
        // Don't call callback if we were cancelled
        if (isCancelled()) {
            return;
        }

        try {
            // Test for possible exceptions
            get();
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            listener.onError(t);
        } catch (InterruptedException e) {
            // Should not be able to happend
            Log.e(TAG, "get() was interrupted", e);
        }
    }

    private static class WrapperCallable implements Callable<Void> {
        private Task task;
        private Listener listener;

        public WrapperCallable(Task task, Listener listener) {
            this.listener = listener;
            this.task = task;
        }

        @Override
        public Void call() throws Exception {
            task.run(listener);

            return null;
        }
    }
}
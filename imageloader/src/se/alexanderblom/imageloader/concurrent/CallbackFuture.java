package se.alexanderblom.imageloader.concurrent;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import se.alexanderblom.imageloader.loader.Loader.Listener;
import android.graphics.Bitmap;
import android.util.Log;

public class CallbackFuture extends FutureTask<Void> {
    private static final String TAG = "CallbackFuture";

    public abstract static class Task implements Callable<Void> {
        private Listener listener;

        public abstract void run() throws Exception;

        public final void deliverResult(InputStream is) {
            listener.onStreamLoaded(is);
        }

        public final void deliverResult(Bitmap b) {
            listener.onBitmapLoaded(b);
        }

        @Override
        public final Void call() throws Exception {
            run();

            return null;
        }
    }

    private Listener listener;

    public CallbackFuture(Task task, Listener listener) {
        super(task);

        this.listener = listener;
        task.listener = listener;
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
}
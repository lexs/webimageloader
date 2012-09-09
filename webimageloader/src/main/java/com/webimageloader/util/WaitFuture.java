package com.webimageloader.util;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import android.graphics.Bitmap;

public class WaitFuture extends FutureTask<Bitmap> {
    public WaitFuture() {
        super(new EmptyCallable());
    }

    @Override
    public void set(Bitmap v) {
        super.set(v);
    }

    @Override
    public void setException(Throwable t) {
        super.setException(t);
    }

    private static class EmptyCallable implements Callable<Bitmap> {
        @Override
        public Bitmap call() throws Exception {
            return null;
        }
    }
}

package com.webimageloader;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.webimageloader.loader.LoaderManager;
import com.webimageloader.loader.MemoryCache;
import com.webimageloader.transformation.Transformation;
import com.webimageloader.util.AbstractImageLoader;
import com.webimageloader.util.WaitFuture;

class ImageLoaderImpl extends AbstractImageLoader {
    private LoaderManager loaderManager;
    private HandlerManager handlerManager;

    ImageLoaderImpl(LoaderManager loaderManager) {
        this.loaderManager = loaderManager;

        handlerManager = new HandlerManager();
    }

    @Override
    public MemoryCache.DebugInfo getMemoryCacheInfo() {
        MemoryCache memoryCache = loaderManager.getMemoryCache();

        if (memoryCache != null) {
            return memoryCache.getDebugInfo();
        } else {
            return null;
        }
    }

    @Override
    public MemoryCache getMemoryCache() {
        return loaderManager.getMemoryCache();
    }

    @Override
    public Bitmap loadBlocking(Request request) throws IOException {
        final WaitFuture future = new WaitFuture();

        Bitmap b = loadInternal(null, request, new LoaderManager.Listener() {
            @Override
            public void onLoaded(Bitmap b) {
                future.set(b);
            }

            @Override
            public void onError(Throwable t) {
                future.setException(t);
            }
        });

        if (b != null) {
            return b;
        }

        boolean interrupted = false;

        try {
            while (true) {
                try {
                    return future.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();

                    // Rethrow as original exception if possible
                    if (cause instanceof IOException) {
                        throw (IOException) cause;
                    } else {
                        throw new IOException("Failed to fetch image", e.getCause());
                    }
                } catch (InterruptedException e) {
                    // Fall through and retry
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void preload(Request request) {
        loadInternal(null, request, null);
    }

    public <T> Bitmap load(T tag, Request request, Listener<T> listener) {
        return loadInternal(tag, request, handlerManager.getListener(tag, listener));
    }

    @Override
    public <T> void cancel(T tag) {
        handlerManager.cancel(tag);
        loaderManager.cancel(tag);
    }

    private Bitmap loadInternal(Object tag, Request request, LoaderManager.Listener listener) {
        return loaderManager.load(tag, request.toLoaderRequest(), listener);
    }

    @Override
    public void destroy() {
        loaderManager.close();
    }

    private static class HandlerManager {
        private Handler handler;

        public HandlerManager() {
            handler = new Handler(Looper.getMainLooper());
        }

        public <T> LoaderManager.Listener getListener(T tag, Listener<T> listener) {
            if (tag != null) {
                // It's possible there is already a callback in progress for this tag
                // so we'll remove it
                handler.removeCallbacksAndMessages(tag);

                return new TagListener<T>(tag, listener);
            } else {
                return new TagListener<T>(listener);
            }
        }

        public void cancel(Object tag) {
            handler.removeCallbacksAndMessages(tag);
        }

        private class TagListener<T> implements LoaderManager.Listener {
            private Listener<T> listener;
            private WeakReference<T> reference;

            public TagListener(Listener<T> listener) {
                this.listener = listener;
            }

            public TagListener(T tag, Listener<T> listener) {
                this.listener = listener;
                this.reference = new WeakReference<T>(tag);
            }

            @Override
            public void onLoaded(final Bitmap b) {
                final T tag = getTag();

                post(tag, new Runnable() {
                    @Override
                    public void run() {
                        listener.onSuccess(tag, b);
                    }
                });
            }

            @Override
            public void onError(final Throwable t) {
                final T tag = getTag();

                post(tag, new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(tag, t);
                    }
                });
            }

            private T getTag() {
                T tag = null;

                if (reference != null) {
                    tag = reference.get();
                    if (tag == null) {
                        throw new RuntimeException("Listener called but tag was GC'ed");
                    }
                }

                return tag;
            }

            private void post(T tag, Runnable r) {
                Message m = Message.obtain(handler, r);
                m.obj = tag;
                handler.sendMessage(m);
            }
        }
    }
}

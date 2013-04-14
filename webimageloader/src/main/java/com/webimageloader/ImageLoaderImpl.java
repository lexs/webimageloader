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
        return loadBlocking(request, null);
    }

    @Override
    public Bitmap loadBlocking(Request request, final ProgressListener progressListener) throws IOException {
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

            @Override
            public void onProgress(float value) {
                if (progressListener != null) {
                    progressListener.onProgress(value);
                }
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

    @Override
    public <T> Bitmap load(T tag, Request request, Listener<T> listener) {
        return load(tag, request, listener, null);
    }

    @Override
    public <T> Bitmap load(T tag, Request request, Listener<T> listener, ProgressListener progressListener) {
        return loadInternal(tag, request, handlerManager.getListener(tag, listener, progressListener));
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

        public <T> LoaderManager.Listener getListener(T tag, Listener<T> listener, ProgressListener progressListener) {
            if (tag != null) {
                // It's possible there is already a callback in progress for this tag
                // so we'll remove it
                handler.removeCallbacksAndMessages(tag);

                return new TagListener<T>(tag, listener, progressListener);
            } else {
                return new TagListener<T>(listener, progressListener);
            }
        }

        public void cancel(Object tag) {
            handler.removeCallbacksAndMessages(tag);
        }

        private class TagListener<T> implements LoaderManager.Listener {
            private WeakReference<T> reference;
            private Listener<T> listener;
            private ProgressListener progressListener;

            public TagListener(Listener<T> listener, ProgressListener progressListener) {
                this.listener = listener;
                this.progressListener = progressListener;
            }

            public TagListener(T tag, Listener<T> listener, ProgressListener progressListener) {
                this.reference = new WeakReference<T>(tag);
                this.listener = listener;
                this.progressListener = progressListener;
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

            @Override
            public void onProgress(final float value) {
                if (progressListener == null) {
                    return;
                }

                post(getTag(), new Runnable() {
                    @Override
                    public void run() {
                        progressListener.onProgress(value);
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

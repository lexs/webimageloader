package com.webimageloader;

import java.io.File;
import java.io.IOException;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import com.webimageloader.content.ContentURLStreamHandler;
import com.webimageloader.loader.DiskLoader;
import com.webimageloader.loader.LoaderManager;
import com.webimageloader.loader.MemoryCache;
import com.webimageloader.loader.NetworkLoader;
import com.webimageloader.loader.LoaderRequest;
import com.webimageloader.transformation.Transformation;
import com.webimageloader.util.WaitFuture;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class ImageLoader {
    private static final String TAG = "ImageLoader";

    private static final LoaderManager.Listener EMPTY_LISTENER = new LoaderManager.Listener() {
        @Override
        public void onLoaded(Bitmap b) {}

        @Override
        public void onError(Throwable t) {}
    };

    public interface Listener<T> {
        void onSuccess(T tag, Bitmap b);
        void onError(T tag, Throwable t);
    }

    private LoaderManager loaderManager;
    private HandlerManager handlerManager;

    private ImageLoader(LoaderManager loaderManager) {
        this.loaderManager = loaderManager;

        handlerManager = new HandlerManager();
    }

    /**
     * Get memory cache debug info
     *
     * @return debug info or null if not available
     */
    public MemoryCache.DebugInfo getMemoryCacheInfo() {
        MemoryCache memoryCache = loaderManager.getMemoryCache();

        if (memoryCache != null) {
            return memoryCache.getDebugInfo();
        } else {
            return null;
        }
    }

    public MemoryCache getMemoryCache() {
        return loaderManager.getMemoryCache();
    }

    public Bitmap loadSynchronously(String url) throws IOException {
        final WaitFuture future = new WaitFuture();

        Request request = new Request(url);
        Bitmap b = load(new Object(), request, new LoaderManager.Listener() {
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
            throw new IOException("Interruped while fetching image", e);
        }
    }

    public void preload(String url) {
        load(new Object(), new Request(url), EMPTY_LISTENER);
    }

    public <T> Bitmap load(T tag, String url, Listener<T> listener) {
        return load(tag, url, null, listener);
    }

    /**
     *
     * @param tag
     * @param url
     * @param transformation
     * @param listener
     * @return the bitmap if the url was in memory cache
     */
    public <T> Bitmap load(T tag, String url, Transformation transformation, Listener<T> listener) {
        LoaderRequest request = new LoaderRequest(url, transformation);
        return load(tag, request, listener);
    }

    public <T> Bitmap load(T tag, Request request,  Listener<T> listener) {
        return load(tag, request.toLoaderRequest(), listener);
    }

    private <T> Bitmap load(T tag, LoaderRequest request,  Listener<T> listener) {
        return load(tag, request, handlerManager.getListener(tag, listener));
    }

    private Bitmap load(Object tag, Request request, LoaderManager.Listener listener) {
        return load(tag, request.toLoaderRequest(), listener);
    }

    private Bitmap load(Object tag, LoaderRequest request, LoaderManager.Listener listener) {
        return loaderManager.load(tag, request, listener);
    }

    public void destroy() {
        loaderManager.close();
    }

    private static class HandlerManager {
        private Handler handler;

        public HandlerManager() {
            handler = new Handler(Looper.getMainLooper());
        }

        public <T> LoaderManager.Listener getListener(T tag, Listener<T> listener) {
            // It's possible there is already a callback in progress for this tag
            // so we'll remove it
            handler.removeCallbacksAndMessages(tag);

            return new TagListener<T>(tag, listener);
        }

        private class TagListener<T> implements LoaderManager.Listener {
            private T tag;
            private Listener<T> listener;

            public TagListener(T tag, Listener<T> listener) {
                this.tag = tag;
                this.listener = listener;
            }

            @Override
            public void onLoaded(final Bitmap b) {
                handler.postAtTime(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSuccess(tag, b);
                    }
                }, tag, 0);
            }

            @Override
            public void onError(final Throwable t) {
                handler.postAtTime(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(tag, t);
                    }
                }, tag, 0);
            }

        }
    }

    public static class Builder {
        private HashMap<String, URLStreamHandler> streamHandlers;

        private DiskLoader diskLoader;
        private MemoryCache memoryCache;

        private int connectionTimeout;
        private int readTimeout;

        public Builder() {
            streamHandlers = new HashMap<String, URLStreamHandler>();
        }

        public Builder enableDiskCache(File cacheDir, int maxSize) {
            try {
                diskLoader = DiskLoader.open(cacheDir, 10 * 1024 * 1024);
            } catch (IOException e) {
                Log.e(TAG, "Disk cache not available", e);
            }

            return this;
        }

        public Builder enableMemoryCache(int maxSize) {
            memoryCache = new MemoryCache(maxSize);

            return this;
        }

        public Builder supportResources(ContentResolver resolver) {
            URLStreamHandler handler = new ContentURLStreamHandler(resolver);
            streamHandlers.put(ContentResolver.SCHEME_CONTENT, handler);
            streamHandlers.put(ContentResolver.SCHEME_FILE, handler);
            streamHandlers.put(ContentResolver.SCHEME_ANDROID_RESOURCE, handler);

            return this;
        }

        public Builder addURLSchemeHandler(String scheme, URLStreamHandler handler) {
            streamHandlers.put(scheme, handler);

            return this;
        }

        public Builder setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;

            return this;
        }

        public Builder setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;

            return this;
        }

        public ImageLoader build() {
            NetworkLoader networkLoader = new NetworkLoader(streamHandlers, connectionTimeout, readTimeout);
            LoaderManager loaderManager = new LoaderManager(memoryCache, diskLoader, networkLoader);

            return new ImageLoader(loaderManager);
        }
    }
}

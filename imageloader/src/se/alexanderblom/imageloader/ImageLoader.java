package se.alexanderblom.imageloader;

import java.io.File;
import java.io.IOException;
import java.net.URLStreamHandler;
import java.util.concurrent.ExecutionException;

import se.alexanderblom.imageloader.content.ContentURLStreamHandler;
import se.alexanderblom.imageloader.loader.DiskLoader;
import se.alexanderblom.imageloader.loader.LoaderManager;
import se.alexanderblom.imageloader.loader.MemoryCache;
import se.alexanderblom.imageloader.loader.NetworkLoader;
import se.alexanderblom.imageloader.transformation.Transformation;
import se.alexanderblom.imageloader.util.WaitFuture;
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
            throw new IOException("Failed to fetch image", e.getCause());
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
        Request request = new Request(url, transformation);
        return load(tag, request, listener);
    }

    private <T> Bitmap load(T tag, Request request,  Listener<T> listener) {
        return load(tag, request, handlerManager.getListener(tag, listener));
    }

    private Bitmap load(Object tag, Request request, LoaderManager.Listener listener) {
        Bitmap b = loaderManager.getBitmap(request);
        if (b != null) {
            return b;
        }

        loaderManager.load(tag, request, listener);

        return null;
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
        private NetworkLoader networkLoader;
        private DiskLoader diskLoader;
        private MemoryCache memoryCache;

        public Builder() {
            networkLoader = new NetworkLoader();
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
            networkLoader.registerStreamHandler(ContentResolver.SCHEME_CONTENT, handler);
            networkLoader.registerStreamHandler(ContentResolver.SCHEME_FILE, handler);
            networkLoader.registerStreamHandler(ContentResolver.SCHEME_ANDROID_RESOURCE, handler);

            return this;
        }

        public Builder addURLSchemeHandler(String scheme, URLStreamHandler handler) {
            networkLoader.registerStreamHandler(scheme, handler);

            return this;
        }

        public ImageLoader build() {
            LoaderManager loaderManager = new LoaderManager(memoryCache, diskLoader, networkLoader);

            return new ImageLoader(loaderManager);
        }
    }
}

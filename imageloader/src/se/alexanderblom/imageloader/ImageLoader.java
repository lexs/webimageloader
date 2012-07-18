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

    private static final Listener<Object> EMPTY_LISTENER = new Listener<Object>() {
        @Override
        public void onSuccess(Object tag, Bitmap b) {}
        @Override
        public void onError(Object tag, Throwable t) {}
    };

    public interface Listener<T> {
        void onSuccess(T tag, Bitmap b);
        void onError(T tag, Throwable t);
    }

    private Handler handler;

    private LoaderManager loaderManager;
    private MemoryCache memoryCache;

    private ImageLoader(MemoryCache memoryCache, LoaderManager loaderManager) {
        this.memoryCache = memoryCache;
        this.loaderManager = loaderManager;

        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Get memory cache debug info
     *
     * @return debug info or null if not available
     */
    public MemoryCache.DebugInfo getMemoryCacheInfo() {
        if (memoryCache != null) {
            return memoryCache.getDebugInfo();
        } else {
            return null;
        }
    }

    public Bitmap loadSynchronously(String url) throws IOException {
        final WaitFuture future = new WaitFuture();

        Bitmap b = load(new Object(), url, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                future.set(b);
            }

            @Override
            public void onError(Object tag, Throwable t) {
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
        load(new Object(), url, EMPTY_LISTENER);
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

    private <T> Bitmap load(final T tag, final Request request, final Listener<T> listener) {
        if (memoryCache != null) {
            Bitmap b = memoryCache.get(request);
            if (b != null) {
                return b;
            }
        }

        loaderManager.load(tag, request, new LoaderManager.Listener() {
            @Override
            public void onLoaded(final Bitmap b) {
                if (memoryCache != null) {
                    memoryCache.set(request, b);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSuccess(tag, b);
                    }
                });
            }

            @Override
            public void onError(final Throwable t) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(tag, t);
                    }
                });
            }
        });

        return null;
    }

    public void destroy() {
        loaderManager.close();
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
            LoaderManager loaderManager = new LoaderManager(diskLoader, networkLoader);

            return new ImageLoader(memoryCache, loaderManager);
        }
    }
}

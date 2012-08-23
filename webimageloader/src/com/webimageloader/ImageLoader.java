package com.webimageloader;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.webimageloader.content.ContentURLStreamHandler;
import com.webimageloader.ext.ImageHelper;
import com.webimageloader.loader.DiskLoader;
import com.webimageloader.loader.LoaderManager;
import com.webimageloader.loader.MemoryCache;
import com.webimageloader.loader.NetworkLoader;
import com.webimageloader.transformation.Transformation;
import com.webimageloader.util.WaitFuture;

/**
 * This is the main class of WebImageLoader which can be constructed using a
 * {@link Builder}. It's often more convenient to use the provided
 * {@link ImageHelper} to load images.
 * <p>
 * It's safe to call the methods on this class from any thread. However, callbacks
 * will always be done on the UI thread.
 *
 * @author Alexander Blom <alexanderblom.se>
 */
public class ImageLoader {
    private static final String TAG = "ImageLoader";

    private static final LoaderManager.Listener EMPTY_LISTENER = new LoaderManager.Listener() {
        @Override
        public void onLoaded(Bitmap b) {}

        @Override
        public void onError(Throwable t) {}
    };

    /**
     * Listener for a request which will always be called on the main thread of
     * the application. You should try to avoid keeping a reference to the tag,
     * for example by declaring this as a static inner class and then using the
     * passed tag when handling the callbacks. This allows us to properly let
     * the tag be GC'ed.
     *
     * @author Alexander Blom <alexanderblom.se>
     *
     * @param <T> the tag class
     */
    public interface Listener<T> {
        /**
         * Called if the request succeeded
         *
         * @param tag the tag which was passed in
         * @param b the resulting bitmap
         */
        void onSuccess(T tag, Bitmap b);

        /**
         * Called if the request failed
         *
         * @param tag the tag which was passed in
         * @param t the reason the request failed
         */
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

    /**
     * Get the memory cache
     *
     * @return memory cache or null if not available
     */
    public MemoryCache getMemoryCache() {
        return loaderManager.getMemoryCache();
    }

    /**
     * Load the specified request blocking the calling thread.
     *
     * @param url the url to load
     * @return the bitmap
     * @throws IOException if the load failed
     *
     * @see #loadBlocking(Request)
     */
    public Bitmap loadBlocking(String url) throws IOException {
        return loadBlocking(new Request(url));
    }

    /**
     * Load the specified request blocking the calling thread.
     *
     * @param url the url to load
     * @param transformation can be null
     * @return the bitmap
     * @throws IOException if the load failed
     *
     * @see #loadBlocking(Request)
     */
    public Bitmap loadBlocking(String url, Transformation transformation) throws IOException {
        return loadBlocking(new Request(url).withTransformation(transformation));
    }

    /**
     * Load the specified request blocking the calling thread.
     *
     * @param request the request to load
     * @return the bitmap
     * @throws IOException if the load failed
     */
    public Bitmap loadBlocking(Request request) throws IOException {
        final WaitFuture future = new WaitFuture();

        Bitmap b = load(null, request, new LoaderManager.Listener() {
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

    /**
     * Used to prime the file and memory cache. It's safe to later call load
     * with the same request, it will automatically be reused.
     *
     * @param url which resource to get
     *
     * @see #preload(Request)
     */
    public void preload(String url) {
        preload(new Request(url));
    }

    /**
     * Used to prime the file and memory cache. It's safe to later call load
     * with the same request, it will automatically be reused.
     *
     * @param url which resource to get
     * @param transformation can be null
     *
     * @see #preload(Request)
     */
    public void preload(String url, Transformation transformation) {
        preload(new Request(url).withTransformation(transformation));
    }

    /**
     * Used to prime the file and memory cache. It's safe to later call load
     * with the same request, it will automatically be reused.
     *
     * @param request the request to preload
     */
    public void preload(Request request) {
        load(null, request, EMPTY_LISTENER);
    }

    /**
     * Load an image from an url with the given listener. Previously pending
     * request for this tag will be automatically cancelled.
     *
     * @param tag used to determine when we this request should be cancelled
     * @param url which resource to get
     * @param listener called when the request has finished or failed
     * @return the bitmap if it was already loaded
     *
     * @see #load(Object, Request, Listener)
     */
    public <T> Bitmap load(T tag, String url, Listener<T> listener) {
        return load(tag, new Request(url), listener);
    }


    /**
     * Load an image from an url with the given listener. Previously pending
     * request for this tag will be automatically cancelled.
     *
     * @param tag used to determine when we this request should be cancelled
     * @param url which resource to get
     * @param transformation can be null
     * @param listener called when the request has finished or failed
     * @return the bitmap if it was already loaded
     *
     * @see #load(Object, Request, Listener)
     */
    public <T> Bitmap load(T tag, String url, Transformation transformation, Listener<T> listener) {
        return load(tag, new Request(url).withTransformation(transformation), listener);
    }

    /**
     * Load an image from an url with the given listener. Previously pending
     * request for this tag will be automatically cancelled.
     *
     * @param tag used to determine when we this request should be cancelled
     * @param request what to to fetch
     * @param listener called when the request has finished or failed
     * @return the bitmap if it was already loaded
     */
    public <T> Bitmap load(T tag, Request request,  Listener<T> listener) {
        return load(tag, request, handlerManager.getListener(tag, listener));
    }

    /**
     * Cancel any pending requests for this tag.
     *
     * @param tag the tag
     */
    public <T> void cancel(T tag) {
        handlerManager.cancel(tag);
        loaderManager.cancel(tag);
    }

    private Bitmap load(Object tag, Request request, LoaderManager.Listener listener) {
        return loaderManager.load(tag, request.toLoaderRequest(), listener);
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

            if (tag == null) {
                return new SimpleTagListener<T>(listener);
            } else {
                return new TagListener<T>(tag, listener);
            }
        }

        public void cancel(Object tag) {
            handler.removeCallbacksAndMessages(tag);
        }
        
        private class SimpleTagListener<T> implements LoaderManager.Listener {
            private Listener<T> listener;

            public SimpleTagListener(Listener<T> listener) {
                this.listener = listener;
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
            
            protected T getTag() {
                return null;
            }

            private void post(T tag, Runnable r) {
                Message m = Message.obtain(handler, r);
                m.obj = tag;
                handler.sendMessage(m);
            }
        }

        private class TagListener<T> extends SimpleTagListener<T> {
            private WeakReference<T> reference;

            public TagListener(T tag, Listener<T> listener) {
                super(listener);
                
                this.reference = new WeakReference<T>(tag);
            }

            @Override
            protected T getTag() {
                T tag = reference.get();
                if (tag == null) {
                    throw new RuntimeException("Listener called but tag was GC'ed");
                }
                
                return tag;
            }
        }
    }

    /**
     * Builder class used to construct a {@link ImageLoader}.
     *
     * @author Alexander Blom <alexanderblom.se>
     */
    public static class Builder {
        public static final long MAX_AGE_INFINITY = Constants.MAX_AGE_INFINITY;
        
        private Context context;

        private HashMap<String, URLStreamHandler> streamHandlers;

        private DiskLoader diskLoader;
        private MemoryCache memoryCache;

        private int connectionTimeout = Constants.DEFAULT_CONNECTION_TIMEOUT;
        private int readTimeout = Constants.DEFAULT_READ_TIMEOUT;
        
        private long defaultMaxAge = Constants.DEFAULT_MAX_AGE;
        private long forcedMaxAge = Constants.MAX_AGE_NOT_FORCED;

        /**
         * Create a new builder
         * @param context the context
         */
        public Builder(Context context) {
            this.context = context.getApplicationContext();

            streamHandlers = new HashMap<String, URLStreamHandler>();
        }

        /**
         * Enable the disk cache
         * @param cacheDir cache location
         * @param maxSize max size of the cache
         * @return
         */
        public Builder enableDiskCache(File cacheDir, int maxSize) {
            try {
                diskLoader = DiskLoader.open(cacheDir, maxSize);
            } catch (IOException e) {
                Log.e(TAG, "Disk cache not available", e);
            }

            return this;
        }

        /**
         * Enable the memory cache
         * @param maxSize max size of the cache
         * @return this builder
         */
        public Builder enableMemoryCache(int maxSize) {
            memoryCache = new MemoryCache(maxSize);

            return this;
        }

        /**
         * Add a URL scheme handler
         * @param scheme the scheme to handle
         * @param handler the handler
         * @return this builder
         *
         * @see URLStreamHandler
         */
        public Builder addURLSchemeHandler(String scheme, URLStreamHandler handler) {
            streamHandlers.put(scheme, handler);

            return this;
        }

        /**
         * Set connection timeout, by default 10 seconds
         * @param connectionTimeout the connection timeout
         * @return this builder
         *
         * @see URLConnection#setConnectTimeout(int)
         */
        public Builder setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;

            return this;
        }


        /**
         * Set read timeout, by default 15 seconds
         * @param readTimeout the read timeout
         * @return this builder
         *
         * @see URLConnection#setReadTimeout(int)
         */
        public Builder setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;

            return this;
        }

        /**
         * Set what max-age to use when a response doesn't have one set
         * @param maxAge default max-age, 0 means infinity
         * @return this builder
         */
        public Builder setDefaultCacheMaxAge(long maxAge) {
            this.defaultMaxAge = maxAge;
            
            return this;
        }
        
        /**
         * Override max-age and expires headers
         * @param maxAge max-age to use for all requests, 0 means infinity
         * @return this builder
         */
        public Builder setCacheMaxAge(long maxAge) {
            this.forcedMaxAge = maxAge;

            return this;
        }

        /**
         * Build the {@link ImageLoader} from the settings in this builder
         * @return a {@link ImageLoader}
         */
        public ImageLoader build() {
            URLStreamHandler handler = new ContentURLStreamHandler(context.getContentResolver());
            streamHandlers.put(ContentResolver.SCHEME_CONTENT, handler);
            streamHandlers.put(ContentResolver.SCHEME_FILE, handler);
            streamHandlers.put(ContentResolver.SCHEME_ANDROID_RESOURCE, handler);

            NetworkLoader networkLoader = new NetworkLoader(streamHandlers, connectionTimeout, readTimeout, defaultMaxAge, forcedMaxAge);
            LoaderManager loaderManager = new LoaderManager(memoryCache, diskLoader, networkLoader);

            return new ImageLoader(loaderManager);
        }
    }

    /**
     * Handles logging for all {@link ImageLoader} instances
     *
     * @author Alexander Blom <alexanderblom.se>
     */
    public static class Logger {
        public static boolean DEBUG = false;
        public static boolean VERBOSE = false;

        /**
         * Log both debug and verbose messages
         */
        public static void logAll() {
            DEBUG = true;
            VERBOSE = true;
        }

        private Logger() {}
    }
}

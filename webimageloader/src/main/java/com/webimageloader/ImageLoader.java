package com.webimageloader;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.webimageloader.content.ContentURLStreamHandler;
import com.webimageloader.loader.DiskLoader;
import com.webimageloader.loader.LoaderManager;
import com.webimageloader.loader.MemoryCache;
import com.webimageloader.loader.NetworkLoader;
import com.webimageloader.transformation.Transformation;

import java.io.File;
import java.io.IOException;
import java.net.URLStreamHandler;

/**
 * This is the main class of WebImageLoader which can be constructed using a
 * {@link Builder}. It's often more convenient to use the provided
 * {@link com.webimageloader.ext.ImageHelper} to load images.
 * <p>
 * It's safe to call the methods on this class from any thread. However, callbacks
 * will always be done on the UI thread.
 *
 * @author Alexander Blom <alexanderblom.se>
 */
public interface ImageLoader {
    static final String TAG = "ImageLoader";

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

    @Deprecated
    MemoryCache.DebugInfo getMemoryCacheInfo();

    /**
     * Get the memory cache
     *
     * @return memory cache or null if not available
     */
    MemoryCache getMemoryCache();

    /**
     * @deprecated use #loadBlocking(Request) instead
     */
    @Deprecated
    Bitmap loadBlocking(String url) throws IOException;

    /**
     * @deprecated use #loadBlocking(Request) instead
     */
    @Deprecated
    Bitmap loadBlocking(String url, Transformation transformation) throws IOException;

    /**
     * Load the specified request blocking the calling thread.
     *
     * @param request the request to load
     * @return the bitmap
     * @throws IOException if the load failed
     */
    Bitmap loadBlocking(Request request) throws IOException;

    /**
     * @deprecated use #preload(Request) instead
     */
    @Deprecated
    void preload(String url);

    /**
     * @deprecated use #preload(Request) instead
     */
    @Deprecated
    void preload(String url, Transformation transformation);

    /**
     * Used to prime the file and memory cache. It's safe to later call load
     * with the same request, it will automatically be reused.
     *
     * @param request the request to preload
     */
    void preload(Request request);

    /**
     * @deprecated use #load(Object, Request, Listener) instead
     */
    @Deprecated
    <T> Bitmap load(T tag, String url, Listener<T> listener);

    /**
     * @deprecated use #load(Object, Request, Listener) instead
     */
    @Deprecated
    <T> Bitmap load(T tag, String url, Transformation transformation, Listener<T> listener);

    /**
     * Load an image from an url with the given listener. Previously pending
     * request for this tag will be automatically cancelled.
     *
     * @param tag used to determine when we this request should be cancelled
     * @param request what to to fetch
     * @param listener called when the request has finished or failed
     * @return the bitmap if it was already loaded
     */
    <T> Bitmap load(T tag, Request request, Listener<T> listener);

    /**
     * Cancel any pending requests for this tag.
     *
     * @param tag the tag
     */
    <T> void cancel(T tag);

    void destroy();

    /**
     * Builder class used to construct a {@link com.webimageloader.ImageLoader}.
     *
     * @author Alexander Blom <alexanderblom.se>
     */
    public static class Builder {
        public static final long MAX_AGE_INFINITY = Constants.MAX_AGE_INFINITY;

        private Context context;

        private NetworkLoader.Builder networkBuilder;

        private DiskLoader diskLoader;
        private MemoryCache memoryCache;

        /**
         * Create a new builder
         * @param context the context
         */
        public Builder(Context context) {
            this.context = context.getApplicationContext();

            networkBuilder = new NetworkLoader.Builder();
        }

        /**
         * Enable the disk cache
         * @param cacheDir cache location
         * @param maxSize max size of the cache
         * @return this builder
         */
        public Builder enableDiskCache(File cacheDir, int maxSize) {
            return enableDiskCache(cacheDir, maxSize, Constants.DEFAULT_DISK_THREADS);
        }

        /**
         * Enable the disk cache
         * @param cacheDir cache location
         * @param maxSize max size of the cache
         * @param threadCount number of threads
         * @return this builder
         */
        public Builder enableDiskCache(File cacheDir, int maxSize, int threadCount) {
            try {
                diskLoader = DiskLoader.open(cacheDir, maxSize, threadCount);
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
         * Set the number of threads to be used for downloading images
         * @param count thread count
         * @return this builder
         */
        public Builder setNetworkThreadCount(int count) {
            networkBuilder.setThreadCount(count);

            return this;
        }

        /**
         * Add a URL scheme handler
         * @param scheme the scheme to handle
         * @param handler the handler
         * @return this builder
         *
         * @see java.net.URLStreamHandler
         */
        public Builder addURLSchemeHandler(String scheme, URLStreamHandler handler) {
            networkBuilder.addURLSchemeHandler(scheme, handler);

            return this;
        }

        /**
         * Set a connection handler
         * @param handler handler to handle connections
         * @return this builder
         */
        public Builder setConnectionHandler(ConnectionHandler handler) {
            networkBuilder.setConnectionHandler(handler);

            return this;
        }

        /**
         * Set connection timeout, by default 10 seconds
         * @param connectionTimeout the connection timeout
         * @return this builder
         *
         * @see java.net.URLConnection#setConnectTimeout(int)
         */
        public Builder setConnectionTimeout(int connectionTimeout) {
            networkBuilder.setConnectionTimeout(connectionTimeout);

            return this;
        }


        /**
         * Set read timeout, by default 15 seconds
         * @param readTimeout the read timeout
         * @return this builder
         *
         * @see java.net.URLConnection#setReadTimeout(int)
         */
        public Builder setReadTimeout(int readTimeout) {
            networkBuilder.setReadTimeout(readTimeout);

            return this;
        }

        /**
         * Set what max-age to use when a response doesn't have one set
         * @param maxAge default max-age, 0 means infinity
         * @return this builder
         */
        public Builder setDefaultCacheMaxAge(long maxAge) {
            networkBuilder.setDefaultCacheMaxAge(maxAge);

            return this;
        }

        /**
         * Override max-age and expires headers
         * @param maxAge max-age to use for all requests, 0 means infinity
         * @return this builder
         */
        public Builder setCacheMaxAge(long maxAge) {
            networkBuilder.setCacheMaxAge(maxAge);

            return this;
        }

        /**
         * Build the {@link com.webimageloader.ImageLoader} from the settings in this builder
         * @return a {@link com.webimageloader.ImageLoader}
         */
        public ImageLoader build() {
            URLStreamHandler handler = new ContentURLStreamHandler(context.getContentResolver());
            networkBuilder.addURLSchemeHandler(ContentResolver.SCHEME_CONTENT, handler);
            networkBuilder.addURLSchemeHandler(ContentResolver.SCHEME_FILE, handler);
            networkBuilder.addURLSchemeHandler(ContentResolver.SCHEME_ANDROID_RESOURCE, handler);

            NetworkLoader networkLoader = new NetworkLoader(networkBuilder);
            LoaderManager loaderManager = new LoaderManager(memoryCache, diskLoader, networkLoader);

            return new ImageLoaderImpl(loaderManager);
        }
    }

    /**
     * Handles logging for all {@link com.webimageloader.ImageLoader} instances
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

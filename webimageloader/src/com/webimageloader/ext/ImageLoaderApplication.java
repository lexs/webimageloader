package com.webimageloader.ext;

import java.io.File;

import com.webimageloader.ImageLoader;
import com.webimageloader.loader.MemoryCache;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.util.Log;

public class ImageLoaderApplication extends Application {
    private static final String TAG = "ImageLoaderApplication";

    public static final String IMAGE_LOADER_SERVICE = "image_loader";

    private static final int MEMORY_DIVIDER = 8;
    private static final int DISK_CACHE_SIZE = 10 * 1024 * 1024;

    private ImageLoader imageLoader;

    @Override
    public void onCreate() {
        super.onCreate();

        imageLoader = getBuilder().build();
    }

    @Override
    public Object getSystemService(String name) {
        if (IMAGE_LOADER_SERVICE.equals(name)) {
            return imageLoader;
        } else {
            return super.getSystemService(name);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        imageLoader.destroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        MemoryCache memoryCache = imageLoader.getMemoryCache();
        if (memoryCache != null) {
            Log.d(TAG, "onLowMemory() called, eviciting all bitmaps");
            memoryCache.evictAll();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        MemoryCache memoryCache = imageLoader.getMemoryCache();
        if (memoryCache == null) {
            return;
        }

        if (level >= TRIM_MEMORY_MODERATE) {
            // Nearing middle of list of cached background apps
            // Evict our entire bitmap cache
            Log.d(TAG, "onTrimMemory(), level>=TRIM_MEMORY_MODERATE called, eviciting all bitmaps");
            memoryCache.evictAll();
        } else if (level >= TRIM_MEMORY_BACKGROUND) {
            // Entering list of cached background apps
            // Evict oldest half of our bitmap cache
            Log.d(TAG, "onTrimMemory(), level>=TRIM_MEMORY_BACKGROUND called, evicing half of all bitmaps");
            memoryCache.trimToSize(memoryCache.size() / 2);
        }
    }

    public static ImageLoader getLoader(Context context) {
        return (ImageLoader) context.getApplicationContext().getSystemService(IMAGE_LOADER_SERVICE);
    }

    protected ImageLoader.Builder getBuilder() {
        // Get memory class of this device, exceeding this amount will throw an
        // OutOfMemory exception.
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        int memClass = am.getMemoryClass();

        // Use part of the available memory for memory cache.
        final int memoryCacheSize = 1024 * 1024 * memClass / MEMORY_DIVIDER;

        Log.d(TAG, "Using memory cache of size: " + humanReadableByteCount(memoryCacheSize, false));
        Log.d(TAG, "Using disk cache of size: " + humanReadableByteCount(DISK_CACHE_SIZE, false));

        File cacheDir = new File(getExternalCacheDir(), "images");
        return new ImageLoader.Builder()
                .enableDiskCache(cacheDir, DISK_CACHE_SIZE)
                .enableMemoryCache(memoryCacheSize)
                .supportResources(getContentResolver());
    }

    // http://stackoverflow.com/a/3758880/253583
    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}

package com.webimageloader.loader;

import com.webimageloader.ImageLoader.Logger;
import com.webimageloader.util.Android;
import com.webimageloader.util.LruCache;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.util.Log;

public class MemoryCache {
    private static final String TAG = "MemoryLoader";

    public static class Entry {
        public final Bitmap bitmap;
        public final Metadata metadata;

        private Entry(Bitmap bitmap, Metadata metadata) {
            this.bitmap = bitmap;
            this.metadata = metadata;
        }
    }

    public static class DebugInfo {
        public final int hitCount;
        public final int missCount;
        public final int putCount;
        public final int evictionCount;

        private DebugInfo(int hitCount, int missCount, int putCount, int evictionCount) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.putCount = putCount;
            this.evictionCount = evictionCount;
        }
    }

    private LruCache<String, Entry> cache;

    public MemoryCache(int maxSize) {
        cache = new BitmapCache(maxSize);
    }

    public int size() {
        return cache.size();
    }

    public int maxSize() {
        return cache.maxSize();
    }

    public void trimToSize(int maxSize) {
        cache.trimToSize(maxSize);
    }

    public void evictAll() {
        cache.evictAll();
    }

    public Entry get(LoaderRequest request) {
        Entry entry = cache.get(request.getCacheKey());
        if (entry != null) {
            if (Logger.VERBOSE) Log.v(TAG, "Loaded " + request + " from memory");
        }

        return entry;
    }

    public void put(LoaderRequest request, Bitmap b, Metadata metadata) {
        // Add the bitmap to the cache if we can fit at least six images of this size,
        // this way we avoid caching large images that will evict all other entries
        if (sizeOf(b) < cache.maxSize() / 6) {
            cache.put(request.getCacheKey(), new Entry(b, metadata));
        }
    }

    public DebugInfo getDebugInfo() {
        return new DebugInfo(cache.hitCount(), cache.missCount(), cache.putCount(), cache.evictionCount());
    }

    @TargetApi(12)
    private static int sizeOf(Bitmap b) {
        if (Android.isAPI(12)) {
            return b.getByteCount();
        } else {
            return b.getRowBytes() * b.getHeight();
        }
    }

    private static class BitmapCache extends LruCache<String, Entry> {
        public BitmapCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Entry value) {
            Bitmap b = value.bitmap;

            return MemoryCache.sizeOf(b);
        }
    }
}

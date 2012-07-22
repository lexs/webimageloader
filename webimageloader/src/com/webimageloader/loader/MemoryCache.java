package com.webimageloader.loader;

import com.webimageloader.Request;
import com.webimageloader.util.Android;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class MemoryCache {
    private static final String TAG = "MemoryLoader";

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

    private LruCache<String, Bitmap> cache;

    public MemoryCache(int maxSize) {
        cache = new Cache(maxSize);
    }

    public Bitmap get(Request request) {
        Bitmap b = cache.get(request.getCacheKey());
        if (b != null) {
            Log.v(TAG, "Loaded " + request + " from memory");
        }

        return b;
    }

    public void set(Request request, Bitmap b) {
        cache.put(request.getCacheKey(), b);
    }

    public DebugInfo getDebugInfo() {
        return new DebugInfo(cache.hitCount(), cache.missCount(), cache.putCount(), cache.evictionCount());
    }

    private static class Cache extends LruCache<String, Bitmap> {
        public Cache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            if (Android.isAPI(12)) {
                return value.getByteCount();
            } else {
                return value.getRowBytes() * value.getHeight();
            }
        }
    }
}

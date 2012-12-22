package com.webimageloader.loader;

public class MemoryLoader implements Loader {
    private MemoryCache cache;

    public MemoryLoader(MemoryCache cache) {
        this.cache = cache;
    }

    @Override
    public void load(LoaderWork.Manager manager, LoaderRequest request) {
        MemoryCache.Entry entry = cache.get(request);
        if (entry != null) {
            manager.deliverBitmap(entry.bitmap, entry.metadata);
        } else {
            // We don't want to cache the image we get back
            // so just pass the same listener
            manager.next(request);
        }
    }

    @Override
    public void cancel(LoaderRequest request) {
        // We can't cancel anything
    }
}

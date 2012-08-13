package com.webimageloader.loader;

import java.util.Iterator;

public class MemoryLoader implements Loader {
    private MemoryCache cache;

    public MemoryLoader(MemoryCache cache) {
        this.cache = cache;
    }

    @Override
    public void load(LoaderRequest request, Iterator<Loader> chain, Listener listener) {
        MemoryCache.Entry entry = cache.get(request);
        if (entry != null) {
            listener.onBitmapLoaded(entry.bitmap, entry.metadata);
        } else {
            // We don't want to cache the image we get back
            // so just pass the same listener
            chain.next().load(request, chain, listener);
        }
    }

    @Override
    public void cancel(LoaderRequest request) {
        // We can't cancel anything
    }
}

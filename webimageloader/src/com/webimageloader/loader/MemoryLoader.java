package com.webimageloader.loader;

import java.util.Iterator;

import android.graphics.Bitmap;


public class MemoryLoader implements Loader {
    private MemoryCache cache;

    public MemoryLoader(MemoryCache cache) {
        this.cache = cache;
    }

    @Override
    public void load(LoaderRequest request, Iterator<Loader> chain, Listener listener) {
        Bitmap b = cache.get(request);
        if (b != null) {
            // TODO: Store metadata in cache
            listener.onBitmapLoaded(b, new Metadata("image/jpeg", 0, 0, null));
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

package com.webimageloader.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.webimageloader.transformation.Transformation;

import android.graphics.Bitmap;

public class LoaderManager {
    private MemoryCache memoryCache;

    private DiskLoader diskLoader;
    private NetworkLoader networkLoader;
    private TransformingLoader transformingLoader;
    private MemoryLoader memoryLoader;

    private List<Loader> standardChain;

    private PendingRequests pendingRequests;

    public interface Listener {
        void onLoaded(Bitmap b);
        void onError(Throwable t);
    }

    public LoaderManager(MemoryCache memoryCache, DiskLoader diskLoader, NetworkLoader networkLoader) {
        this.memoryCache = memoryCache;
        this.diskLoader = diskLoader;
        this.networkLoader = networkLoader;

        transformingLoader = new TransformingLoader();
        if (memoryCache != null) {
            memoryLoader = new MemoryLoader(memoryCache);
        }

        standardChain = new ArrayList<Loader>();
        add(standardChain, diskLoader);
        standardChain.add(networkLoader);

        // Ensure the standard chain is not modified and is safe to iterate
        // over in multiple threads
        standardChain = Collections.unmodifiableList(standardChain);

        pendingRequests = new PendingRequests(memoryCache, standardChain);
    }

    public MemoryCache getMemoryCache() {
        return memoryCache;
    }

    public Bitmap getBitmap(Object tag, LoaderRequest request) {
        return pendingRequests.getBitmap(tag, request);
    }

    public Bitmap load(Object tag, LoaderRequest request, final Listener listener) {
        Bitmap b = pendingRequests.getBitmap(tag, request);
        if (b != null) {
            return b;
        }

        Loader.Listener l = pendingRequests.addRequest(tag, request, listener);

        // A request is already pending, don't load anything
        if (l == null) {
            return null;
        }

        List<Loader> chain = standardChain;

        Transformation transformation = request.getTransformation();
        if (transformation != null) {
            // Use special chain with transformation
            ArrayList<Loader> loaderChain = new ArrayList<Loader>();
            add(loaderChain, diskLoader);
            add(loaderChain, transformingLoader);
            add(loaderChain, memoryLoader);
            add(loaderChain, diskLoader);
            add(loaderChain, networkLoader);

            chain = loaderChain;
        }

        Iterator<Loader> it = chain.iterator();
        it.next().load(request, it, l);

        return null;
    }

    public void close() {
        if (diskLoader != null) {
            diskLoader.close();
        }
    }

    private static <T> void add(List<T> list, T item) {
        if (item != null) {
            list.add(item);
        }
    }
}

package com.webimageloader.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.webimageloader.transformation.Transformation;

import android.graphics.Bitmap;

public class LoaderManager {
    private static final LoaderManager.Listener EMPTY_LISTENER = new LoaderManager.Listener() {
        @Override
        public void onLoaded(Bitmap b) {}

        @Override
        public void onError(Throwable t) {}
    };
    
    private MemoryCache memoryCache;

    private DiskLoader diskLoader;
    private TransformingLoader transformingLoader;
    private MemoryLoader memoryLoader;

    private List<Loader> standardChain;
    private List<Loader> transformationChain;

    private PendingRequests pendingRequests;

    public interface Listener {
        void onLoaded(Bitmap b);
        void onError(Throwable t);
    }

    public LoaderManager(MemoryCache memoryCache, DiskLoader diskLoader, NetworkLoader networkLoader) {
        this.memoryCache = memoryCache;
        this.diskLoader = diskLoader;

        transformingLoader = new TransformingLoader();
        if (memoryCache != null) {
            memoryLoader = new MemoryLoader(memoryCache);
        }

        // Create standard chain
        standardChain = new ArrayList<Loader>();
        add(standardChain, diskLoader);
        add(standardChain, networkLoader);

        // Create transformation chain
        transformationChain = new ArrayList<Loader>();
        add(transformationChain, diskLoader);
        add(transformationChain, transformingLoader);
        add(transformationChain, memoryLoader);
        add(transformationChain, diskLoader);
        add(transformationChain, networkLoader);

        // Ensure the chains are not modified and is safe to iterate
        // over in multiple threads
        standardChain = Collections.unmodifiableList(standardChain);
        transformationChain = Collections.unmodifiableList(transformationChain);

        pendingRequests = new PendingRequests(memoryCache, standardChain);
    }

    public MemoryCache getMemoryCache() {
        return memoryCache;
    }

    public Bitmap getBitmap(Object tag, LoaderRequest request) {
        return pendingRequests.getBitmap(tag, request);
    }

    public Bitmap load(Object tag, LoaderRequest request, Listener listener) {
        Bitmap b = pendingRequests.getBitmap(tag, request);
        if (b != null) {
            return b;
        }
        
        // Send an empty listener instead of null
        if (listener == null) {
            listener = EMPTY_LISTENER;
        }

        LoaderWork work = pendingRequests.addRequest(tag, request, listener);

        // A request is already pending, don't load anything
        if (work == null) {
            return null;
        }

        // Use different chains depending if we have a transformation or not
        Transformation t = request.getTransformation();
        List<Loader> chain = t == null ? standardChain : transformationChain;

        work.start(chain, request);

        return null;
    }

    public void cancel(Object tag) {
        pendingRequests.cancel(tag);
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

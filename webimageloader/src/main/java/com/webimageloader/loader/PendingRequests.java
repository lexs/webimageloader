package com.webimageloader.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.webimageloader.ImageLoader.Logger;
import com.webimageloader.util.BitmapUtils;
import com.webimageloader.util.InputSupplier;

import android.graphics.Bitmap;
import android.util.Log;

public class PendingRequests {
    private static final String TAG = "PendingRequests";

    private MemoryCache memoryCache;

    // Don't remove tags at all, this means both of these should be weakhashmaps
    //
    private Map<Object, PendingListeners> pendingTags;
    private Map<LoaderRequest, PendingListeners> pendingRequests;

    public PendingRequests(MemoryCache memoryCache) {
        this.memoryCache = memoryCache;

        // Use WeakHashMap to ensure tags can be GC'd
        pendingTags = new WeakHashMap<Object, PendingListeners>();
        pendingRequests = new HashMap<LoaderRequest, PendingListeners>();
    }

    public synchronized Bitmap getBitmap(Object tag, LoaderRequest request) {
        if (memoryCache != null) {
            MemoryCache.Entry entry = memoryCache.get(request);
            if (entry != null) {
                // We got this bitmap, cancel old pending work
                cancelPotentialWork(tag);
                return entry.bitmap;
            }
        }

        return null;
    }

    public synchronized LoaderWork addRequest(Object tag, LoaderRequest request, LoaderManager.Listener listener) {
        if (tag != null && stillPending(tag, request)) {
            return null;
        }

        if (tag != null) {
            cancelPotentialWork(tag);

        }

        PendingListeners listeners = pendingRequests.get(request);
        LoaderWork work = null;

        if (listeners == null) {
            work = new LoaderWork(new RequestListener(request));

            listeners = new PendingListeners(request, tag, listener, work);
            pendingRequests.put(request, listeners);
        } else {
            if (Logger.VERBOSE) Log.v(TAG, "Reusing request: " + request);
            listeners.add(tag, listener);
        }

        if (tag != null) {
            pendingTags.put(tag, listeners);
        }

        return work;
    }

    public synchronized void cancel(Object tag) {
        cancelPotentialWork(tag);
    }

    protected synchronized void deliverResult(LoaderRequest request, Bitmap b, Metadata metadata) {
        PendingListeners listeners = removeRequest(request);
        if (listeners != null) {
            saveToMemoryCache(request, b, metadata);

            listeners.deliverResult(b);
        }
    }

    protected synchronized void deliverError(LoaderRequest request, Throwable t) {
        PendingListeners listeners = removeRequest(request);
        if (listeners != null) {
            listeners.deliverError(t);
        }
    }

    private PendingListeners removeRequest(LoaderRequest request) {
        PendingListeners listeners = pendingRequests.remove(request);
        if (listeners == null) {
            if (Logger.VERBOSE) Log.v(TAG, "Request no longer pending: " + request);
        } else {
            pendingTags.keySet().removeAll(listeners.getTags());
        }

        return listeners;
    }

    private void cancelPotentialWork(Object tag) {
        PendingListeners listeners = pendingTags.remove(tag);
        if (listeners == null) {
            return;
        }

        listeners.remove(tag);
        if (listeners.isEmpty()) {
            LoaderRequest request = listeners.getRequest();
            pendingRequests.remove(request);
            listeners.cancel();
        }
    }

    private void saveToMemoryCache(LoaderRequest request, Bitmap b, Metadata metadata) {
        if (memoryCache != null) {
            memoryCache.put(request, b, metadata);
        }
    }

    private boolean stillPending(Object tag, LoaderRequest request) {
        PendingListeners listeners = pendingTags.get(tag);

        return listeners != null && request.equals(listeners.getRequest());
     }

    private class RequestListener implements Loader.Listener {
        private LoaderRequest request;

        public RequestListener(LoaderRequest request) {
            this.request = request;
        }

        @Override
        public void onStreamLoaded(InputSupplier input, Metadata metadata) {
            try {
                InputStream is = input.getInput();
                
                try {
                    Bitmap b = BitmapUtils.decodeStream(is);
                    
                    onBitmapLoaded(b, metadata);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                onError(e);
            }
        }

        @Override
        public void onBitmapLoaded(Bitmap b, Metadata metadata) {
            deliverResult(request, b, metadata);
        }

        @Override
        public void onNotModified(Metadata metadata) {
            // Nothing changed, we don't need to notify any listeners
            memoryCache.updateMetadata(request, metadata);
        }

        @Override
        public void onError(Throwable t) {
            deliverError(request, t);
        }
    }

    private static class PendingListeners {
        private LoaderRequest request;
        private Map<Object, LoaderManager.Listener> listeners;
        private List<LoaderManager.Listener> extraListeners;
        private LoaderWork work;

        public PendingListeners(LoaderRequest request, Object tag, LoaderManager.Listener listener, LoaderWork work) {
            this.request = request;
            this.work = work;

            // Use a WeakHashMap to ensure tags can be GC'd, also use 1 a initial
            // capacity as we expect a low number of listeners per request
            listeners = new WeakHashMap<Object, LoaderManager.Listener>(1);
            extraListeners = new ArrayList<LoaderManager.Listener>(1);

            add(tag, listener);
        }

        public void add(Object tag, LoaderManager.Listener listener) {
            if (tag == null) {
                extraListeners.add(listener);
            } else {
                listeners.put(tag, listener);
            }
        }

        public void remove(Object tag) {
            listeners.remove(tag);
        }

        public boolean isEmpty() {
            return listeners.isEmpty() && extraListeners.isEmpty();
        }

        public void cancel() {
            work.cancel();
        }

        public LoaderRequest getRequest() {
            return request;
        }

        public Set<Object> getTags() {
            return listeners.keySet();
        }

        public void deliverResult(Bitmap b) {
            for (LoaderManager.Listener listener : listeners.values()) {
                listener.onLoaded(b);
            }

            for (LoaderManager.Listener listener : extraListeners) {
                listener.onLoaded(b);
            }
        }

        public void deliverError(Throwable t) {
            for (LoaderManager.Listener listener : listeners.values()) {
                listener.onError(t);
            }

            for (LoaderManager.Listener listener : extraListeners) {
                listener.onError(t);
            }
        }
    }
}

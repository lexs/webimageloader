package com.webimageloader.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import com.webimageloader.util.BitmapUtils;

import android.graphics.Bitmap;
import android.util.Log;

public class PendingRequests {
    private static final String TAG = "PendingRequests";

    private MemoryCache memoryCache;
    private List<Loader> loaders;

    private WeakHashMap<Object, LoaderRequest> pendingsTags;
    private WeakHashMap<LoaderRequest, PendingListeners> pendingsRequests;


    public PendingRequests(MemoryCache memoryCache, List<Loader> loaders) {
        this.memoryCache = memoryCache;
        this.loaders = loaders;

        pendingsTags = new WeakHashMap<Object, LoaderRequest>();
        pendingsRequests = new WeakHashMap<LoaderRequest, PendingListeners>();
    }

    public synchronized Bitmap getBitmap(Object tag, LoaderRequest request) {
        if (memoryCache != null) {
            Bitmap b = memoryCache.get(request);
            if (b != null) {
                // We got this bitmap, cancel old pending work
                cancelPotentialWork(tag);
                return b;
            }
        }

        return null;
    }

    public synchronized Loader.Listener addRequest(Object tag, LoaderRequest request, LoaderManager.Listener listener) {
        if (stillPending(tag, request)) {
            return null;
        }

        cancelPotentialWork(tag);

        pendingsTags.put(tag, request);

        PendingListeners listeners = pendingsRequests.get(request);
        if (listeners == null) {
            listeners = new PendingListeners(tag, listener);
            pendingsRequests.put(request, listeners);

            return new RequestListener(request);
        } else {
            Log.v(TAG, "Reusing request: " + request);
            listeners.add(tag, listener);

            return null;
        }
    }

    protected synchronized void deliverResult(LoaderRequest request, Bitmap b) {
        PendingListeners listeners = removeRequest(request);
        if (listeners != null) {
            saveToMemoryCache(request, b);

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
        PendingListeners listeners = pendingsRequests.remove(request);
        if (listeners == null) {
            Log.v(TAG, "Request no longer pending: " + request);
        } else {
            filterTagsForRequest(listeners, request);
            pendingsTags.keySet().removeAll(listeners.getTags());
        }

        return listeners;
    }

    private void cancelPotentialWork(Object tag) {
        LoaderRequest request = pendingsTags.remove(tag);
        if (request == null) {
            return;
        }

        // TODO: Why can this be null
        PendingListeners listeners = pendingsRequests.get(request);
        if (!listeners.remove(tag)) {
            pendingsRequests.remove(request);

            for (Loader loader : loaders) {
                loader.cancel(request);
            }
        }
    }

    /**
     * Remove tags not pending for this request
     */
    private void filterTagsForRequest(PendingListeners listeners, LoaderRequest request) {
        // Tags pending for this request
        Set<Object> tags = listeners.getTags();

        for (Iterator<Object> it = tags.iterator(); it.hasNext(); ) {
            Object tag = it.next();

            // Check if tag is still pending
            if (!stillPending(tag, request)) {
                it.remove();
            }
        }
    }

    private void saveToMemoryCache(LoaderRequest request, Bitmap b) {
        if (memoryCache != null) {
            memoryCache.set(request, b);
        }
    }

    private boolean stillPending(Object tag, LoaderRequest request) {
        return request.equals(pendingsTags.get(tag));
     }

    private class RequestListener implements Loader.Listener {
        private LoaderRequest request;

        public RequestListener(LoaderRequest request) {
            this.request = request;
        }

        @Override
        public void onStreamLoaded(InputStream is, Metadata metadata) {
            Bitmap b = BitmapUtils.decodeStream(is);

            if (b != null) {
                onBitmapLoaded(b, metadata);
            } else {
                onError(new IOException("Failed to create bitmap, decodeStream() returned null"));
            }
        }

        @Override
        public void onBitmapLoaded(Bitmap b, Metadata metadata) {
            deliverResult(request, b);
        }

        @Override
        public void onNotModified(Metadata metadata) {
            // Nothing changed, we don't need to notify any listeners
        }

        @Override
        public void onError(Throwable t) {
            deliverError(request, t);
        }
    }

    private static class PendingListeners {
        private WeakHashMap<Object, LoaderManager.Listener> listeners;

        public PendingListeners(Object tag, LoaderManager.Listener listener) {
            listeners = new WeakHashMap<Object, LoaderManager.Listener>();

            add(tag, listener);
        }

        public void add(Object tag, LoaderManager.Listener listener) {
            listeners.put(tag, listener);
        }

        /**
         * Remove a listener
         * @return true if this task is still pending
         */
        public boolean remove(Object tag) {
            listeners.remove(tag);

            if (listeners.isEmpty()) {
                return false;
            } else {
                return true;
            }
        }

        public Set<Object> getTags() {
            return listeners.keySet();
        }

        public void deliverResult(Bitmap b) {
            for (LoaderManager.Listener listener : listeners.values()) {
                listener.onLoaded(b);
            }
        }

        public void deliverError(Throwable t) {
            for (LoaderManager.Listener listener : listeners.values()) {
                listener.onError(t);
            }
        }
    }
}

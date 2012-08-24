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
    private List<Loader> loaders;

    private Map<Object, LoaderRequest> pendingsTags;
    private Map<LoaderRequest, PendingListeners> pendingsRequests;

    public PendingRequests(MemoryCache memoryCache, List<Loader> loaders) {
        this.memoryCache = memoryCache;
        this.loaders = loaders;

        // Use WeakHashMap to ensure tags can be GC'd
        pendingsTags = new WeakHashMap<Object, LoaderRequest>();
        pendingsRequests = new HashMap<LoaderRequest, PendingListeners>();
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

    public synchronized Loader.Listener addRequest(Object tag, LoaderRequest request, LoaderManager.Listener listener) {
        if (tag != null && stillPending(tag, request)) {
            return null;
        }

        if (tag != null) {
            cancelPotentialWork(tag);
            pendingsTags.put(tag, request);
        }

        PendingListeners listeners = pendingsRequests.get(request);
        if (listeners == null) {
            listeners = new PendingListeners(tag, listener);
            pendingsRequests.put(request, listeners);

            return new RequestListener(request);
        } else {
            if (Logger.VERBOSE) Log.v(TAG, "Reusing request: " + request);
            listeners.add(tag, listener);

            return null;
        }
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
        PendingListeners listeners = pendingsRequests.remove(request);
        if (listeners == null) {
            if (Logger.VERBOSE) Log.v(TAG, "Request no longer pending: " + request);
        } else {
            pendingsTags.keySet().removeAll(listeners.getTags());
        }

        return listeners;
    }

    private void cancelPotentialWork(Object tag) {
        LoaderRequest request = pendingsTags.remove(tag);
        if (request == null) {
            return;
        }

        PendingListeners listeners = pendingsRequests.get(request);
        if (!listeners.remove(tag)) {
            pendingsRequests.remove(request);

            for (Loader loader : loaders) {
                loader.cancel(request);
            }
        }
    }

    private void saveToMemoryCache(LoaderRequest request, Bitmap b, Metadata metadata) {
        if (memoryCache != null) {
            memoryCache.put(request, b, metadata);
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
        public void onStreamLoaded(InputSupplier input, Metadata metadata) {
            try {
                InputStream is = input.getInput();
                
                try {
                    Bitmap b = BitmapUtils.decodeStream(is);
    
                    if (b != null) {
                        onBitmapLoaded(b, metadata);
                    } else {
                        throw new IOException("Failed to create bitmap, decodeStream() returned null");
                    }
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
            // TODO: We should probably update the memory cache
        }

        @Override
        public void onError(Throwable t) {
            deliverError(request, t);
        }
    }

    private static class PendingListeners {
        private Map<Object, LoaderManager.Listener> listeners;
        private List<LoaderManager.Listener> extraListeners;

        public PendingListeners(Object tag, LoaderManager.Listener listener) {
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

        /**
         * Remove a listener
         * @return true if this task is still pending
         */
        public boolean remove(Object tag) {
            listeners.remove(tag);

            if (listeners.isEmpty() && extraListeners.isEmpty()) {
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

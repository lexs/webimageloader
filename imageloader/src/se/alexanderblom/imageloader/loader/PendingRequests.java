package se.alexanderblom.imageloader.loader;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

import se.alexanderblom.imageloader.Request;
import se.alexanderblom.imageloader.util.BitmapUtils;
import android.graphics.Bitmap;
import android.util.Log;

public class PendingRequests {
    private static final String TAG = "PendingRequests";

    private WeakHashMap<Object, Request> pendingsTags;
    private WeakHashMap<Request, PendingListeners> pendingsRequests;

    private MemoryCache memoryCache;

    public PendingRequests(MemoryCache memoryCache) {
        this.memoryCache = memoryCache;

        pendingsTags = new WeakHashMap<Object, Request>();
        pendingsRequests = new WeakHashMap<Request, PendingListeners>();
    }

    public synchronized Loader.Listener addRequest(Object tag, Request request, LoaderManager.Listener listener) {
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

    private void cancelPotentialWork(Object tag) {
        Request request = pendingsTags.remove(tag);
        if (request == null) {
            return;
        }

        PendingListeners listeners = pendingsRequests.get(request);
        if (!listeners.remove(tag)) {
            pendingsRequests.remove(request);
            // TODO: Actually cancel request
        }
    }

    private synchronized void deliverResult(Request request, Bitmap b) {
        PendingListeners listeners = pendingsRequests.remove(request);
        if (listeners == null) {
            Log.v(TAG, "Request no longer pending: " + request);
            return;
        }

        saveToMemoryCache(request, b);

        filterTagsForRequest(listeners, request);
        listeners.deliverResult(b);
        pendingsTags.keySet().removeAll(listeners.getTags());
    }

    private synchronized void deliverError(Request request, Throwable t) {
        PendingListeners listeners = pendingsRequests.get(request);
        if (listeners == null) {
            Log.v(TAG, "Request no longer pending: " + request);
            return;
        }

        filterTagsForRequest(listeners, request);
        listeners.deliverError(t);
        pendingsTags.keySet().removeAll(listeners.getTags());
    }

    /**
     * Remove tags not pending for this request
     */
    private void filterTagsForRequest(PendingListeners listeners, Request request) {
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

    private void saveToMemoryCache(Request request, Bitmap b) {
        if (memoryCache != null) {
            memoryCache.set(request, b);
        }
    }

    private boolean stillPending(Object tag, Request request) {
        return request.equals(pendingsTags.get(tag));
     }

    private class RequestListener implements Loader.Listener {
        private Request request;

        public RequestListener(Request request) {
            this.request = request;
        }

        @Override
        public void onStreamLoaded(InputStream is) {
            Bitmap b = BitmapUtils.decodeStream(is);
            onBitmapLoaded(b);
        }

        @Override
        public void onBitmapLoaded(Bitmap b) {
            deliverResult(request, b);
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

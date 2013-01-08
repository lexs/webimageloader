package com.webimageloader.ext;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import com.webimageloader.ImageLoader;
import com.webimageloader.Request;
import com.webimageloader.loader.MemoryCache;
import com.webimageloader.transformation.Transformation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ListImageLoader implements ImageLoader {
    private static final String TAG = "ListImageLoader";

    private static final int MESSAGE_DISPATCH_REQUESTS = 1;
    private static final int DISPATCH_DELAY = 550;

    private ImageLoader imageLoader;
    private Map<Object, RequestEntry<?>> requests;

    private boolean fingerUp = true;
    private int lastScrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
    private Handler handler;

    public ListImageLoader(ImageLoader imageLoader, AbsListView listView) {
        this.imageLoader = imageLoader;

        // We don't use weak keys here because this loader should have the same lifetime
        // as the tags themselves
        // accessOrder = true as we want tags in last put order
        requests = new LinkedHashMap<Object, RequestEntry<?>>(16, 0.75f, true);

        listView.setOnScrollListener(new ScrollManager());
        listView.setOnTouchListener(new FingerTracker());

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                dispatchRequests();

                return true;
            }
        });
    }

    @Override
    public MemoryCache.DebugInfo getMemoryCacheInfo() {
        return imageLoader.getMemoryCacheInfo();
    }

    @Override
    public MemoryCache getMemoryCache() {
        return imageLoader.getMemoryCache();
    }

    @Override
    public Bitmap loadBlocking(String url) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bitmap loadBlocking(String url, Transformation transformation) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bitmap loadBlocking(Request request) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void preload(String url) {
        preload(new Request(url));
    }

    @Override
    public void preload(String url, Transformation transformation) {
        preload(new Request(url, transformation));
    }

    @Override
    public void preload(Request request) {
        imageLoader.preload(request);
    }

    @Override
    public <T> Bitmap load(T tag, String url, Listener<T> listener) {
        return load(tag, new Request(url), listener);
    }

    @Override
    public <T> Bitmap load(T tag, String url, Transformation transformation, Listener<T> listener) {
        return load(tag, new Request(url, transformation), listener);
    }

    @Override
    public <T> Bitmap load(T tag, Request request, Listener<T> listener) {
        if (lastScrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            Bitmap b = imageLoader.get(request);
            if (b != null) {
                // Bitmap in memory cache, cancel possible other requests for this tag
                requests.remove(tag);
                imageLoader.cancel(tag);
            } else {
                // Not in cache, cancel previous fetches and queue this request
                imageLoader.cancel(tag);
                requests.put(tag, new RequestEntry<T>(tag, request, listener));
            }

            return b;
        } else {
            // It's possible we have a pending request for this
            requests.remove(tag);

            return imageLoader.load(tag, request, listener);
        }
    }

    @Override
    public Bitmap get(Request request) {
        return imageLoader.get(request);
    }

    @Override
    public <T> void cancel(T tag) {
        imageLoader.cancel(tag);
    }

    @Override
    public void destroy() {
        imageLoader.destroy();
    }

    private void dispatchRequests() {
        if (requests.isEmpty()) {
            return;
        }

        for (RequestEntry<?> entry : requests.values()) {
            entry.load(imageLoader);
        }

        if (Logger.VERBOSE) Log.d(TAG, "Dispatched " + requests.size() + " requests");

        requests.clear();
    }

    private static class RequestEntry<T> {
        private final T tag;
        private final Request request;
        private final Listener<T> listener;

        private RequestEntry(T tag, Request request, Listener<T> listener) {
            this.tag = tag;
            this.request = request;
            this.listener = listener;
        }

        public void load(ImageLoader imageLoader) {
            Bitmap b = imageLoader.load(tag, request, listener);
            if (b != null) {
                listener.onSuccess(tag, b);
            }
        }
    }

    private class ScrollManager implements AbsListView.OnScrollListener {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            boolean stoppedFling = lastScrollState == SCROLL_STATE_FLING &&
                    scrollState != SCROLL_STATE_FLING;

            if (stoppedFling) {
                handler.removeMessages(MESSAGE_DISPATCH_REQUESTS);

                // Delay loading if the finger is down as this may mean they are just
                // continuing their fling
                int delay = fingerUp ? 0 : DISPATCH_DELAY;
                handler.sendEmptyMessageDelayed(MESSAGE_DISPATCH_REQUESTS, delay);
            } else if (scrollState == SCROLL_STATE_FLING) {
                handler.removeMessages(MESSAGE_DISPATCH_REQUESTS);
            }

            lastScrollState = scrollState;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
    }

    private class FingerTracker implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            fingerUp = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;

            return false;
        }
    }
}

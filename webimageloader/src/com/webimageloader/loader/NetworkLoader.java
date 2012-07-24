package com.webimageloader.loader;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.webimageloader.concurrent.ExecutorHelper;
import com.webimageloader.concurrent.ListenerFuture;
import com.webimageloader.util.Android;
import com.webimageloader.util.PriorityThreadFactory;

import android.os.Process;
import android.util.Log;

public class NetworkLoader implements Loader {
    private static final String TAG = "NetworkLoader";

    private Map<String, URLStreamHandler> streamHandlers;
    private int connectTimeout;
    private int readTimeout;

    private ExecutorHelper executorHelper;

    public NetworkLoader(Map<String, URLStreamHandler> streamHandlers, int connectionTimeout, int readTimeout) {
        this.streamHandlers = Collections.unmodifiableMap(streamHandlers);
        this.connectTimeout = connectionTimeout;
        this.readTimeout = readTimeout;

        ExecutorService executor = Executors.newFixedThreadPool(2, new PriorityThreadFactory("Network", Process.THREAD_PRIORITY_BACKGROUND));
        executorHelper = new ExecutorHelper(executor);
    }

    @Override
    public void load(LoaderRequest request, Iterator<Loader> chain, Listener listener) {
        ListenerFuture.Task task = new DownloadTask(request);
        executorHelper.run(request, listener, task);
    }

    @Override
    public void cancel(LoaderRequest request) {
        executorHelper.cancel(request);
    }

    /**
     * Workaround for bug pre-Froyo, see here for more info:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     */
    private static void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (!Android.isAPI(8)) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private static String getProtocol(String url) {
        int i = url.indexOf(':');
        return i == -1 ? null : url.substring(0, i);
    }

    private URLStreamHandler getURLStreamHandler(String protocol) {
        return streamHandlers.get(protocol);
    }

    private class DownloadTask implements ListenerFuture.Task {
        private LoaderRequest request;

        public DownloadTask(LoaderRequest request) {
            this.request = request;
        }

        @Override
        public void run(Listener listener) throws Exception {
            String url = request.getUrl();

            disableConnectionReuseIfNecessary();

            String protocol = getProtocol(url);
            URLStreamHandler streamHandler = getURLStreamHandler(protocol);

            URLConnection urlConnection = new URL(null, url, streamHandler).openConnection();

            if (connectTimeout > 0) {
                urlConnection.setConnectTimeout(connectTimeout);
            }

            if (readTimeout > 0) {
                urlConnection.setReadTimeout(readTimeout);
            }

            InputStream is = urlConnection.getInputStream();

            Log.v(TAG, "Loaded " + request + " from network");

            try {
                listener.onStreamLoaded(is);
            } finally {
                is.close();
            }
        }
    }
}

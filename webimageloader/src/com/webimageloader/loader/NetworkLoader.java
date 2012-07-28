package com.webimageloader.loader;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Process;
import android.util.Log;

import com.webimageloader.util.Android;
import com.webimageloader.util.PriorityThreadFactory;

public class NetworkLoader extends BackgroundLoader {
    private static final String TAG = "NetworkLoader";

    private Map<String, URLStreamHandler> streamHandlers;
    private int connectTimeout;
    private int readTimeout;

    public NetworkLoader(Map<String, URLStreamHandler> streamHandlers, int connectionTimeout, int readTimeout) {
        this.streamHandlers = Collections.unmodifiableMap(streamHandlers);
        this.connectTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    @Override
    protected ExecutorService createExecutor() {
        return Executors.newFixedThreadPool(2, new PriorityThreadFactory("Network", Process.THREAD_PRIORITY_BACKGROUND));
    }

    @Override
    protected void loadInBackground(LoaderRequest request, Iterator<Loader> chain, Listener listener) throws Exception {
        String url = request.getUrl();

        disableConnectionReuseIfNecessary();

        String protocol = getProtocol(url);
        URLStreamHandler streamHandler = getURLStreamHandler(protocol);

        HttpURLConnection urlConnection = (HttpURLConnection) new URL(null, url, streamHandler).openConnection();

        if (connectTimeout > 0) {
            urlConnection.setConnectTimeout(connectTimeout);
        }

        if (readTimeout > 0) {
            urlConnection.setReadTimeout(readTimeout);
        }

        long modifiedSince = request.getModifiedSince();
        if (modifiedSince != 0) {
            urlConnection.setIfModifiedSince(modifiedSince);
        }

        String contentType = urlConnection.getContentType();
        long lastModified = urlConnection.getLastModified();
        // TODO: Use cache-control: max-age instead
        long expires = urlConnection.getExpiration();

        Metadata metadata = new Metadata(contentType, lastModified, expires);

        if (modifiedSince != 0 && urlConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            Log.v(TAG, request + " was not modified since last fetch");

            listener.onStreamLoaded(null, metadata);
        } else {
            InputStream is = urlConnection.getInputStream();
            Log.v(TAG, "Loaded " + request + " from network");

            try {
                listener.onStreamLoaded(is, metadata);
            } finally {
                is.close();
            }
        }
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
}

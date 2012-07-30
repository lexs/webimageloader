package com.webimageloader.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.webimageloader.util.Android;
import com.webimageloader.util.HeaderParser;
import com.webimageloader.util.PriorityThreadFactory;

public class NetworkLoader extends BackgroundLoader {
    private static final String TAG = "NetworkLoader";

    private static final long DEFAULT_MAX_AGE = 3 * 24 * 60 * 60 * 1000; // Three days

    private Map<String, URLStreamHandler> streamHandlers;
    private int connectTimeout;
    private int readTimeout;
    private long forceMaxAge;

    public NetworkLoader(Map<String, URLStreamHandler> streamHandlers, int connectionTimeout, int readTimeout, long maxAge) {
        this.streamHandlers = Collections.unmodifiableMap(streamHandlers);
        this.connectTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.forceMaxAge = maxAge;
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

        URLConnection urlConnection = new URL(null, url, streamHandler).openConnection();

        if (connectTimeout > 0) {
            urlConnection.setConnectTimeout(connectTimeout);
        }

        if (readTimeout > 0) {
            urlConnection.setReadTimeout(readTimeout);
        }

        Metadata metadata = request.getMetadata();
        if (metadata != null) {
            // We have some information available
            long modifiedSince = metadata.getLastModified();
            if (modifiedSince != 0) {
                urlConnection.setIfModifiedSince(modifiedSince);
            }

            String etag = metadata.getEtag();
            if (!TextUtils.isEmpty(etag)) {
                urlConnection.addRequestProperty("If-None-Match", etag);
            }
        }

        String contentType = urlConnection.getContentType();
        long lastModified = urlConnection.getLastModified();
        long expires = getExpires(urlConnection);
        String etag = urlConnection.getHeaderField("ETag");

        if (forceMaxAge != 0) {
            expires = forceMaxAge;
        }

        if (expires == 0) {
            expires = System.currentTimeMillis() + DEFAULT_MAX_AGE;
        }

        // Update metadata
        metadata = new Metadata(contentType, lastModified, expires, etag);

        if (getResponseCode(urlConnection) == HttpURLConnection.HTTP_NOT_MODIFIED) {
            Log.v(TAG, request + " was not modified since last fetch");

            listener.onNotModified(metadata);
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

    private int getResponseCode(URLConnection urlConnection) throws IOException {
        // We can't assume we have a HttpUrlConnection as resources uses a custom subclass
        if (urlConnection instanceof HttpURLConnection) {
            return ((HttpURLConnection) urlConnection).getResponseCode();
        } else {
            return -1;
        }
    }

    private long getExpires(URLConnection urlConnection) {
        if (forceMaxAge > 0) {
            return System.currentTimeMillis() + forceMaxAge;
        }

        // Prefer "max-age" before "expires"
        long maxAge = HeaderParser.getMaxAge(urlConnection);
        if (maxAge > 0) {
            return System.currentTimeMillis() + maxAge * 1000;
        }

        long expires = urlConnection.getExpiration();
        if (expires > 0) {
            return expires;
        }

        // Use default
        return System.currentTimeMillis() + DEFAULT_MAX_AGE;
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

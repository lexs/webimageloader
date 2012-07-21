package se.alexanderblom.imageloader.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import se.alexanderblom.imageloader.Request;
import se.alexanderblom.imageloader.concurrent.ListenerFuture;
import se.alexanderblom.imageloader.util.Android;
import se.alexanderblom.imageloader.util.PriorityThreadFactory;
import android.net.Uri;
import android.os.Process;
import android.util.Log;

public class NetworkLoader implements Loader {
    private static final String TAG = "NetworkLoader";

    private HashMap<String, URLStreamHandler> streamHandlers;

    private ExecutorService executor;

    public NetworkLoader() {
        streamHandlers = new HashMap<String, URLStreamHandler>();
        executor = Executors.newFixedThreadPool(2, new PriorityThreadFactory(Process.THREAD_PRIORITY_BACKGROUND));
    }

    @Override
    public void load(Request request, Iterator<Loader> chain, Listener listener) {
        ListenerFuture.Task task = new DownloadTask(request);
        executor.submit(new ListenerFuture(task, listener));
    }

    public void registerStreamHandler(String scheme, URLStreamHandler handler) {
        streamHandlers.put(scheme, handler);
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
        Uri uri = Uri.parse(url);
        return uri.getScheme();
    }

    private URLStreamHandler getURLStreamHandler(String protocol) {
        return streamHandlers.get(protocol);
    }

    private class DownloadTask implements ListenerFuture.Task {
        private Request request;

        public DownloadTask(Request request) {
            this.request = request;
        }

        @Override
        public void run(Listener listener) throws Exception {
            String url = request.getUrl();

            disableConnectionReuseIfNecessary();

            String protocol = getProtocol(url);
            URLStreamHandler streamHandler = getURLStreamHandler(protocol);

            URLConnection urlConnection = new URL(null, url, streamHandler).openConnection();

            if (urlConnection instanceof HttpURLConnection) {
                // If we are getting this from http, ensure we got a 200
                HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;

                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode != 200) {
                    throw new IOException("Server error: " + responseCode);
                }
            }

            Log.v(TAG, "Loaded " + request + " from network");

            InputStream is = urlConnection.getInputStream();
            try {
                listener.onStreamLoaded(is);
            } finally {
                is.close();
            }
        }
    }
}

package se.alexanderblom.imageloader.loader;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import se.alexanderblom.imageloader.Request;
import se.alexanderblom.imageloader.concurrent.ExecutorHelper;
import se.alexanderblom.imageloader.concurrent.ListenerFuture;
import se.alexanderblom.imageloader.util.Android;
import se.alexanderblom.imageloader.util.PriorityThreadFactory;
import android.net.Uri;
import android.os.Process;
import android.util.Log;

public class NetworkLoader implements Loader {
    private static final String TAG = "NetworkLoader";

    private Map<String, URLStreamHandler> streamHandlers;

    private ExecutorHelper executorHelper;

    public NetworkLoader(Map<String, URLStreamHandler> streamHandlers) {
        this.streamHandlers = Collections.unmodifiableMap(streamHandlers);

        ExecutorService executor = Executors.newFixedThreadPool(2, new PriorityThreadFactory(Process.THREAD_PRIORITY_BACKGROUND));
        executorHelper = new ExecutorHelper(executor);
    }

    @Override
    public void load(Request request, Iterator<Loader> chain, Listener listener) {
        ListenerFuture.Task task = new DownloadTask(request);
        executorHelper.run(request, listener, task);
    }

    @Override
    public void cancel(Request request) {
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

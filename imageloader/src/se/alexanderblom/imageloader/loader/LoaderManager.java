package se.alexanderblom.imageloader.loader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import se.alexanderblom.imageloader.Request;
import se.alexanderblom.imageloader.transformation.Transformation;
import android.graphics.Bitmap;
import android.util.Log;

public class LoaderManager {
    private DiskLoader diskLoader;
    private NetworkLoader networkLoader;
    private TransformingLoader transformingLoader;

    private List<Loader> standardChain;

    private PendingRequests pendingRequests;

    public interface Listener {
        void onLoaded(Bitmap b);
        void onError(Throwable t);
    }

    public LoaderManager(DiskLoader diskLoader, NetworkLoader networkLoader) {
        this.diskLoader = diskLoader;
        this.networkLoader = networkLoader;
        transformingLoader = new TransformingLoader();

        standardChain = new ArrayList<Loader>();
        if (diskLoader != null) {
            standardChain.add(diskLoader);
        }
        standardChain.add(networkLoader);

        // Ensure the standard chain is not modified and is safe to iterate
        // over in multiple threads
        standardChain = Collections.unmodifiableList(standardChain);

        pendingRequests = new PendingRequests();
    }

    public void load(Object tag, Request request, final Listener listener) {
        List<Loader> chain = standardChain;

        Transformation transformation = request.getTransformation();
        if (transformation != null) {
            // Use special chain with transformation
            ArrayList<Loader> loaderChain = new ArrayList<Loader>();
            loaderChain.add(diskLoader);
            loaderChain.add(transformingLoader);
            loaderChain.add(diskLoader);
            loaderChain.add(networkLoader);

            chain = loaderChain;
        }

        Loader.Listener l = pendingRequests.addRequest(tag, request, listener);

        // Only load if neccesary
        if (l != null) {
            Iterator<Loader> it = chain.iterator();
            it.next().load(request, it, l);
        }
    }

    public void close() {
        if (diskLoader != null) {
            diskLoader.close();
        }
    }

    private static class TransformingLoader implements Loader {
        private static final String TAG = "TransformingLoader";

        @Override
        public void load(Request request, Iterator<Loader> chain, final Listener listener) {
            Log.d(TAG, "Transforming " + request);

            final Transformation transformation = request.getTransformation();

            // Modify request
            Request modified = request.withoutTransformation();
            chain.next().load(modified, chain, new Listener() {
                @Override
                public void onStreamLoaded(InputStream is) {
                    Bitmap b = transformation.transform(is);
                    deliverResult(b);
                }

                @Override
                public void onBitmapLoaded(Bitmap b) {
                    b = transformation.transform(b);
                    deliverResult(b);
                }

                private void deliverResult(Bitmap b) {
                    if (b == null) {
                        onError(new NullPointerException("Transformer returned null"));
                    } else {
                        listener.onBitmapLoaded(b);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    listener.onError(t);
                }
            });
        }
    }
}

package com.webimageloader.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import android.graphics.Bitmap;
import android.util.Log;

import com.webimageloader.transformation.Transformation;

public class TransformingLoader implements Loader {
    private static final String TAG = "TransformingLoader";

    @Override
    public void load(LoaderRequest request, Iterator<Loader> chain, final Listener listener) {
        Log.d(TAG, "Transforming " + request);

        final Transformation transformation = request.getTransformation();

        // Modify request
        LoaderRequest modified = request.withoutTransformation();
        chain.next().load(modified, chain, new Listener() {
            @Override
            public void onStreamLoaded(InputStream is, Metadata metadata) {
                try {
                    Bitmap b = transformation.transform(is);
                    deliverResult(b, metadata);
                } catch (IOException e) {
                    listener.onError(e);
                }
            }

            @Override
            public void onBitmapLoaded(Bitmap b, Metadata metadata) {
                b = transformation.transform(b);
                deliverResult(b, metadata);
            }

            private void deliverResult(Bitmap b, Metadata metadata) {
                if (b == null) {
                    onError(new NullPointerException("Transformer returned null"));
                } else {
                    listener.onBitmapLoaded(b, metadata);
                }
            }

            @Override
            public void onNotModified(Metadata metadata) {
                listener.onNotModified(metadata);
            }

            @Override
            public void onError(Throwable t) {
                listener.onError(t);
            }
        });
    }

    @Override
    public void cancel(LoaderRequest request) {
        // We can't cancel anything as we don't run the
        // transformation on our own thread
    }
}
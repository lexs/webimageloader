package com.webimageloader.loader;

import java.util.Iterator;

import com.webimageloader.util.InputSupplier;

import android.graphics.Bitmap;

public interface Loader {
    interface Listener {
        void onStreamLoaded(InputSupplier input, Metadata metadata);
        void onBitmapLoaded(Bitmap b, Metadata metadata);
        void onNotModified(Metadata metadata);
        void onError(Throwable t);
    }

    void load(LoaderRequest request, Iterator<Loader> chain, Listener listener);
    void cancel(LoaderRequest request);
}

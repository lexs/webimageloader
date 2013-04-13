package com.webimageloader.loader;

import com.webimageloader.util.InputSupplier;

import android.graphics.Bitmap;

public interface Loader {
    interface Listener {
    	void onProgress(int progress);    	
        void onStreamLoaded(InputSupplier input, Metadata metadata);
        void onBitmapLoaded(Bitmap b, Metadata metadata);
        void onNotModified(Metadata metadata);
        void onError(Throwable t);
    }

    void load(LoaderWork.Manager manager, LoaderRequest request);
}

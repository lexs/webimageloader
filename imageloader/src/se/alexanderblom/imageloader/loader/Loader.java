package se.alexanderblom.imageloader.loader;

import java.io.InputStream;
import java.util.Iterator;

import se.alexanderblom.imageloader.Request;

import android.graphics.Bitmap;

public interface Loader {
    interface Listener {
        void onStreamLoaded(InputStream is);
        void onBitmapLoaded(Bitmap b);
        void onError(Throwable t);
    }

    void load(Request request, Iterator<Loader> chain, Listener listener);
}

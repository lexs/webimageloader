package com.webimageloader.util;

import android.graphics.Bitmap;
import com.webimageloader.ImageLoader;
import com.webimageloader.Request;
import com.webimageloader.transformation.Transformation;

import java.io.IOException;

public abstract class AbstractImageLoader implements ImageLoader {
    @Override
    public Bitmap loadBlocking(String url) throws IOException {
        return loadBlocking(new Request(url));
    }

    @Override
    public Bitmap loadBlocking(String url, Transformation transformation) throws IOException {
        return loadBlocking(new Request(url, transformation));
    }

    @Override
    public void preload(String url) {
        preload(new Request(url));
    }

    @Override
    public void preload(String url, Transformation transformation) {
        preload(new Request(url, transformation));
    }

    @Override
    public <T> Bitmap load(T tag, String url, Listener<T> listener) {
        return load(tag, new Request(url), listener);
    }

    @Override
    public <T> Bitmap load(T tag, String url, Transformation transformation, Listener<T> listener) {
        return load(tag, new Request(url, transformation), listener);
    }
}

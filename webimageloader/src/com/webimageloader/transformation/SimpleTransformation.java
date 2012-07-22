package com.webimageloader.transformation;

import java.io.InputStream;

import com.webimageloader.util.BitmapUtils;

import android.graphics.Bitmap;

public abstract class SimpleTransformation implements Transformation {
    @Override
    public Bitmap transform(InputStream is) {
        Bitmap b = BitmapUtils.decodeStream(is);
        return transform(b);
    }
}

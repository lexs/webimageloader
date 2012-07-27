package com.webimageloader.transformation;

import java.io.IOException;
import java.io.InputStream;

import com.webimageloader.util.BitmapUtils;

import android.graphics.Bitmap;

public abstract class SimpleTransformation implements Transformation {
    @Override
    public Bitmap transform(InputStream is) throws IOException {
        Bitmap b = BitmapUtils.decodeStream(is);
        if (b == null) {
            throw new IOException("Failed to decode stream");
        }
        return transform(b);
    }
}

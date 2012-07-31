package com.webimageloader.transformation;

import java.io.IOException;
import java.io.InputStream;

import com.webimageloader.util.BitmapUtils;

import android.graphics.Bitmap;

/**
 * Adapter class to use if you don't need the {@link InputStream} provided by
 * the base {@link Transformation} class.
 *
 * @author Alexander Blom <alexanderblom.se>
 */
public abstract class SimpleTransformation implements Transformation {
    /**
     * {@inheritDoc}
     */
    @Override
    public Bitmap transform(InputStream is) throws IOException {
        Bitmap b = BitmapUtils.decodeStream(is);
        if (b == null) {
            throw new IOException("Failed to decode stream");
        }
        return transform(b);
    }
}

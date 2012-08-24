package com.webimageloader.transformation;

import java.io.IOException;
import java.io.InputStream;

import com.webimageloader.util.BitmapUtils;
import com.webimageloader.util.InputSupplier;

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
    public Bitmap transform(InputSupplier input) throws IOException {
        InputStream is = input.getInput();

        try {
            Bitmap b = BitmapUtils.decodeStream(is);

            return transform(b);
        } finally {
            is.close();
        }
    }
}

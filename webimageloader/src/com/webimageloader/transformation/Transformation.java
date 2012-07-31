package com.webimageloader.transformation;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;

/**
 * Transformation to apply to an image while loading it
 * @author Alexander Blom <alexanderblom.se>
 */
public interface Transformation {
    /**
     * Get the identified for this transformation. It should be unique and include any
     * Parameters passed to this transformation.
     *
     * @return the identifier
     */
    String getIdentifier();

    /**
     * Transform this {@link InputStream} to a {@link Bitmap}.
     *
     * @param is original {@link InputStream}
     * @return transformed {@link Bitmap}
     * @throws IOException if the conversion failed
     */
    Bitmap transform(InputStream is) throws IOException;

    /**
     * Transform this {@link Bitmap} to a new {@link Bitmap}.
     *
     * @param b original {@link Bitmap}
     * @return transformed {@link Bitmap}
     */
    Bitmap transform(Bitmap b);
}

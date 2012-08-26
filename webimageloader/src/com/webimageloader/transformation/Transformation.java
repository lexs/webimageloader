package com.webimageloader.transformation;

import java.io.IOException;

import com.webimageloader.util.InputSupplier;

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
     * Get the format used when saving the result of this transformation. This
     * can be useful for example when relying on alpha.
     *
     * @return the bitmap compress format, null for default
     */
    Bitmap.CompressFormat getCompressFormat();

    /**
     * Transform this {@link InputSupplier} to a {@link Bitmap}.
     *
     * @param input original {@link InputSupplier}
     * @return transformed {@link Bitmap}
     * @throws IOException if the conversion failed
     */
    Bitmap transform(InputSupplier input) throws IOException;

    /**
     * Transform this {@link Bitmap} to a new {@link Bitmap}.
     *
     * @param b original {@link Bitmap}
     * @return transformed {@link Bitmap}
     */
    Bitmap transform(Bitmap b);
}

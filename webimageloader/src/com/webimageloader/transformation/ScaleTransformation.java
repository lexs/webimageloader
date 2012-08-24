package com.webimageloader.transformation;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.webimageloader.util.InputSupplier;

/**
 * Scale an image to the specified size, it is safe to use
 * this transformation on very large images as it loads the
 * images using a sample size, as descriped in Android Training
 * 
 * @author Alexander Blom <alexanderblom.se>
 */
public class ScaleTransformation implements Transformation {
    private int reqWidth;
    private int reqHeight;

    /**
     * Create a new scale transformation which will scale the image to
     * the specified required size. The image can however be slightly larger
     * than the size specified.
     * 
     * @param reqWidth required width, the image will not be smaller than this
     * @param reqHeight required height, the image will not be smaller than this
     */
    public ScaleTransformation(int reqWidth, int reqHeight) {
        this.reqWidth = reqWidth;
        this.reqHeight = reqHeight;
    }

    @Override
    public String getIdentifier() {
        return "webimageloader_scale-" + reqWidth + "x" + reqHeight;
    }

    @Override
    public Bitmap transform(InputSupplier input) throws IOException {
        return decodeSampledBitmap(input);
    }

    @Override
    public Bitmap transform(Bitmap b) {
        return Bitmap.createScaledBitmap(b, reqWidth, reqHeight, true);
    }

    private Bitmap decodeSampledBitmap(InputSupplier input) throws IOException {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        
        InputStream is = input.getInput();
        try {
            BitmapFactory.decodeStream(is, null, options);
        } finally {
            is.close();
        }

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        
        is = input.getInput();
        try {
            return BitmapFactory.decodeStream(is, null, options);
        } finally {
            is.close();
        }
    }
    
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }
}

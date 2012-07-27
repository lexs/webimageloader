package com.webimageloader.transformation;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;


public interface Transformation {
    String getIdentifier();

    Bitmap transform(InputStream is) throws IOException;
    Bitmap transform(Bitmap b);
}

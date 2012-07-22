package com.webimageloader.transformation;

import java.io.InputStream;

import android.graphics.Bitmap;


public interface Transformation {
    String getIdentifier();

    Bitmap transform(InputStream is);
    Bitmap transform(Bitmap b);
}

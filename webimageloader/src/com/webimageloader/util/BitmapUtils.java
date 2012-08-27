package com.webimageloader.util;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.webimageloader.Constants;

public class BitmapUtils {
    public static Bitmap.CompressFormat getCompressFormat(String contentType) {
        if ("image/png".equals(contentType)) {
            return Bitmap.CompressFormat.PNG;
        } else if ("image/jpeg".equals(contentType)) {
            return Bitmap.CompressFormat.JPEG;
        } else {
            // Unknown format, use default
            return Constants.DEFAULT_COMPRESS_FORMAT;
        }
    }

    @TargetApi(14)
    public static String getContentType(Bitmap.CompressFormat format) {
        switch (format) {
            case PNG:
                return "image/png";
            case JPEG:
                return "image/jpeg";
            case WEBP:
                return "image/webp";
            default:
                // Unknown format, use default
                return getContentType(Constants.DEFAULT_COMPRESS_FORMAT);
        }
    }

    public static Bitmap decodeStream(InputStream is) throws IOException {
        Bitmap b = BitmapFactory.decodeStream(is);
        if (b == null) {
            throw new IOException("Failed to create bitmap, decodeStream() returned null");
        }

        return b;
    }

    private BitmapUtils() {}
}

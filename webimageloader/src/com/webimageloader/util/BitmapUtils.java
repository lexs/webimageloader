package com.webimageloader.util;

import java.io.FilterInputStream;
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
        // Handle a bug in older versions of Android, see
        // http://android-developers.blogspot.se/2010/07/multithreading-for-performance.html
        if (!Android.isAPI(9)) {
            is = new FlushedInputStream(is);
        }

        Bitmap b = BitmapFactory.decodeStream(is);
        if (b == null) {
            throw new IOException("Failed to create bitmap, decodeStream() returned null");
        }

        return b;
    }

    private BitmapUtils() {}

    /**
     * A {@link FilterInputStream} that blocks until the requested number of bytes
     * have been read/skipped, or the end of the stream is reached.
     * <p>
     * This filter can be used as a work-around for <a
     * href="http://code.google.com/p/android/issues/detail?id=6066">Issue
     * #6066</a>.
     */
    private static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                      int b = read();
                      if (b < 0) {
                          break;  // we reached EOF
                      } else {
                          bytesSkipped = 1; // we read one byte
                      }
               }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }
}

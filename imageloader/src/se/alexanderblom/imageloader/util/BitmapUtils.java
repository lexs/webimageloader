package se.alexanderblom.imageloader.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtils {
    public static Bitmap decodeStream(InputStream is) {
        // Handle a bug in older versions of Android, see
        // http://android-developers.blogspot.se/2010/07/multithreading-for-performance.html
        if (!Android.isAPI(9)) {
            return BitmapFactory.decodeStream(new FlushedInputStream(is));
        } else {
            return BitmapFactory.decodeStream(is);
        }
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
        /*
        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            // Since we don't override "write(int oneByte)", we can write directly to "out"
            // and avoid the inefficient implementation from the FilterOutputStream.
            in.write(buffer, offset, length);
        }*/
    }
}

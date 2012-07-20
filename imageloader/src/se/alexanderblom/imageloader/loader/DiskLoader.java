package se.alexanderblom.imageloader.loader;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import se.alexanderblom.imageloader.Request;
import se.alexanderblom.imageloader.concurrent.ListenerFuture;
import se.alexanderblom.imageloader.util.DiskLruCache;
import se.alexanderblom.imageloader.util.DiskLruCache.Editor;
import se.alexanderblom.imageloader.util.DiskLruCache.Snapshot;
import se.alexanderblom.imageloader.util.IOUtil;
import android.graphics.Bitmap;
import android.util.Log;

public class DiskLoader implements Loader, Closeable {
    private static final String TAG = "DiskLoader";

    private static final int APP_VERSION = 1;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    private static final int COMPRESS_QUALITY = 75;

    private static final int INPUT_IMAGE = 0;
    private static final int INPUT_METADATA = 1;
    private static final int VALUE_COUNT = 1;

    private ExecutorService executor;

    private DiskLruCache cache;

    public static DiskLoader open(File directory, long maxSize) throws IOException {
        return new DiskLoader(DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize));
    }

    private DiskLoader(DiskLruCache cache) {
        this.cache = cache;

        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void close() {
        executor.shutdownNow();

        IOUtil.closeQuietly(cache);
    }

    @Override
    public void load(final Request request, final Iterator<Loader> chain, final Listener listener) {
        run(listener, new ListenerFuture.Task() {
            @Override
            public void run(Listener listener) throws Exception {
                String key = hashKeyForDisk(request);
                Snapshot snapshot = cache.get(key);
                if (snapshot != null) {
                    try {
                        InputStream is = snapshot.getInputStream(INPUT_IMAGE);
                        listener.onStreamLoaded(is);
                        is.close();

                        Log.v(TAG, "Loaded " + request + " from disk");
                    } finally {
                        snapshot.close();
                    }
                } else {
                    // We need to get the next loader
                    Loader next = chain.next();
                    next.load(request, chain, new NextListener(request, listener));
                }
            }
        });
    }

    private void run(Listener listener, ListenerFuture.Task task) {
        executor.submit(new ListenerFuture(task, listener));
    }

    private class NextListener implements Listener {
        private Request request;
        private Listener listener;

        public NextListener(Request request, Listener listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void onStreamLoaded(InputStream is) {
            try {
                final String key = hashKeyForDisk(request);
                Editor editor = cache.edit(key);
                if (editor == null) {
                    throw new IOException("File is already being edited");
                }

                try {
                    // TODO: Buffer
                    OutputStream os = editor.newOutputStream(INPUT_IMAGE);
                    copy(is, os);
                    os.close();
                    editor.commit();

                    // Read back the file we just saved
                    run(listener, new ReadTask(request));
                } catch (IOException e) {
                    editor.abort();
                    throw e;
                }
            } catch (IOException e) {
                listener.onError(e);
            }
        }

        @Override
        public void onBitmapLoaded(Bitmap b) {
            try {
                final String key = hashKeyForDisk(request);
                Editor editor = cache.edit(key);
                if (editor == null) {
                    throw new IOException("File is already being edited");
                }

                try {
                    // TODO: Buffer
                    OutputStream os = editor.newOutputStream(INPUT_IMAGE);
                    b.compress(COMPRESS_FORMAT, COMPRESS_QUALITY, os);
                    os.close();
                    editor.commit();

                    // Read back the file we just saved
                    run(listener, new ReadTask(request));
                } catch (IOException e) {
                    editor.abort();
                    throw e;
                }
            } catch (IOException e) {
                listener.onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            listener.onError(t);
        }
    }

    private class ReadTask implements ListenerFuture.Task {
        private Request request;

        public ReadTask(Request request) {
            this.request = request;
        }

        @Override
        public void run(Listener listener) throws Exception {
            String key = hashKeyForDisk(request);

            Snapshot snapshot = cache.get(key);
            if (snapshot == null) {
                throw new IllegalStateException("File not available");
            }

            try {
                InputStream is = snapshot.getInputStream(INPUT_IMAGE);
                listener.onStreamLoaded(is);
                is.close();
            } finally {
                snapshot.close();
            }
        }
    }

    private static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable
     * for using as a disk filename.
     */
    private static String hashKeyForDisk(Request request) {
        String key = request.getCacheKey();

        try {
            byte[] bytes = MessageDigest.getInstance("SHA-1").digest(key.getBytes());

            return bytesToHexString(bytes);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(key.hashCode());
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}

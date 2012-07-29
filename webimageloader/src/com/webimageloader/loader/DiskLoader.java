package com.webimageloader.loader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.webimageloader.concurrent.ListenerFuture;
import com.webimageloader.util.DiskLruCache;
import com.webimageloader.util.Hasher;
import com.webimageloader.util.IOUtil;
import com.webimageloader.util.PriorityThreadFactory;
import com.webimageloader.util.DiskLruCache.Editor;
import com.webimageloader.util.DiskLruCache.Snapshot;

import android.graphics.Bitmap;
import android.os.Process;
import android.util.Log;

public class DiskLoader extends BackgroundLoader implements Closeable {
    private static final String TAG = "DiskLoader";

    private static final int APP_VERSION = 2;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    private static final int COMPRESS_QUALITY = 75;

    private static final int INPUT_IMAGE = 0;
    private static final int INPUT_METADATA = 1;
    private static final int VALUE_COUNT = 2;

    private DiskLruCache cache;
    private Hasher hasher;

    public static DiskLoader open(File directory, long maxSize) throws IOException {
        return new DiskLoader(DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize));
    }

    @Override
    protected ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(new PriorityThreadFactory("Disk", Process.THREAD_PRIORITY_BACKGROUND));
    }

    private DiskLoader(DiskLruCache cache) {
        this.cache = cache;

        hasher = new Hasher();
    }

    @Override
    public void close() {
        super.close();

        IOUtil.closeQuietly(cache);
    }

    @Override
    protected void loadInBackground(LoaderRequest request, Iterator<Loader> chain, Listener listener) throws IOException {
        String key = hashKeyForDisk(request);
        Snapshot snapshot = cache.get(key);
        if (snapshot != null) {
            try {
                Log.v(TAG, "Loaded " + request + " from disk");

                InputStream is = snapshot.getInputStream(INPUT_IMAGE);

                Metadata metadata = readMetadata(snapshot);
                if (System.currentTimeMillis() > metadata.getExpires()) {
                    // Cache has expired
                    Log.v(TAG, request + " has expired, updating");
                    chain.next().load(request.withMetadata(metadata), chain, new NextListener(request, listener));
                }

                listener.onStreamLoaded(is, metadata);
                is.close();

            } finally {
                snapshot.close();
            }
        } else {
            // We need to get the next loader
            Loader next = chain.next();
            next.load(request, chain, new NextListener(request, listener));
        }
    }

    private Metadata readMetadata(Snapshot snapshot) throws IOException {
        // Use a small buffer as the metadata itself is small
        InputStream is = new BufferedInputStream(snapshot.getInputStream(INPUT_METADATA), 1024);
        try {
            return Metadata.from(is);
        } finally {
            is.close();
        }
    }

    private class NextListener implements Listener {
        private LoaderRequest request;
        private Listener listener;

        public NextListener(LoaderRequest request, Listener listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void onStreamLoaded(InputStream is, Metadata metadata) {
            try {
                String key = hashKeyForDisk(request);
                Editor editor = cache.edit(key);
                if (editor == null) {
                    throw new IOException("File is already being edited");
                }

                OutputStream os = new BufferedOutputStream(editor.newOutputStream(INPUT_IMAGE));
                try {
                    copy(new BufferedInputStream(is), os);
                    os.close();
                    writeMetadata(editor, metadata);

                    editor.commit();

                    // Read back the file we just saved
                    run(request, listener, new ReadTask(request, metadata));
                } catch (IOException e) {
                    // We failed writing to the cache, we can't really do
                    // anything to clean this up
                    editor.abort();
                    listener.onError(e);
                }
            } catch (IOException e) {
                // We failed opening the cache, this
                // means that the InputStream is still untouched.
                // Pass it trough to the listener without caching.
                Log.e(TAG, "Failed opening cache", e);
                listener.onStreamLoaded(is, metadata);
            }
        }

        @Override
        public void onBitmapLoaded(Bitmap b, Metadata metadata) {
            try {
                String key = hashKeyForDisk(request);
                Editor editor = cache.edit(key);
                if (editor == null) {
                    throw new IOException("File is already being edited");
                }

                try {
                    Bitmap.CompressFormat format = getCompressFormat(metadata.getContentType());
                    writeBitmap(editor, b, format);
                    writeMetadata(editor, metadata);

                    editor.commit();
                } catch (IOException e) {
                    // We failed writing to the cache
                    editor.abort();

                    // Let the outer catch handle this
                    throw e;
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed saving bitmap to cache", e);
            }

            // We can always pass on the bitmap we got, even if
            // we didn't manage to write it to cache
            listener.onBitmapLoaded(b, metadata);
        }

        @Override
        public void onNotModified(Metadata metadata) {
            try {
                String key = hashKeyForDisk(request);
                Editor editor = cache.edit(key);
                if (editor == null) {
                    throw new IOException("File is already being edited");
                }

                try {
                    writeMetadata(editor, metadata);

                    editor.commit();
                } catch (IOException e) {
                    // We failed writing to the cache
                    editor.abort();

                    // Let the outer catch handle this
                    throw e;
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to update metadata", e);
            }

            listener.onNotModified(metadata);
        }

        @Override
        public void onError(Throwable t) {
            listener.onError(t);
        }

        private void writeMetadata(Editor editor, Metadata metadata) throws IOException {
            OutputStream os = new BufferedOutputStream(editor.newOutputStream(INPUT_METADATA));
            try {
                metadata.writeTo(os);
            } finally {
                IOUtil.closeQuietly(os);
            }
        }

        private void writeBitmap(Editor editor, Bitmap b, Bitmap.CompressFormat format) throws IOException {
            OutputStream os = new BufferedOutputStream(editor.newOutputStream(INPUT_IMAGE));
            try {
                b.compress(format, COMPRESS_QUALITY, os);
            } finally {
                IOUtil.closeQuietly(os);
            }
        }

        private Bitmap.CompressFormat getCompressFormat(String contentType) {
            if ("image/png".equals(contentType)) {
                return Bitmap.CompressFormat.PNG;
            } else if ("image/jpeg".equals(contentType)) {
                return Bitmap.CompressFormat.JPEG;
            } else {
                // Unknown format, use default
                return COMPRESS_FORMAT;
            }
        }
    }

    private class ReadTask implements ListenerFuture.Task {
        private LoaderRequest request;
        private Metadata metadata;

        public ReadTask(LoaderRequest request, Metadata metadata) {
            this.request = request;
            this.metadata = metadata;
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
                listener.onStreamLoaded(is, metadata);
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
    private String hashKeyForDisk(LoaderRequest request) {
        String key = request.getCacheKey();

        // We don't except to have a lot of threads
        // so it's okay to synchronize access

        synchronized (hasher) {
            return hasher.hash(key);
        }
    }
}

package com.webimageloader.loader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.os.Process;
import android.util.Log;

import com.webimageloader.Constants;
import com.webimageloader.ImageLoader.Logger;
import com.webimageloader.concurrent.ListenerFuture;
import com.webimageloader.util.BitmapUtils;
import com.webimageloader.util.DiskLruCache;
import com.webimageloader.util.DiskLruCache.Editor;
import com.webimageloader.util.DiskLruCache.Snapshot;
import com.webimageloader.util.Hasher;
import com.webimageloader.util.IOUtil;
import com.webimageloader.util.InputSupplier;
import com.webimageloader.util.PriorityThreadFactory;

public class DiskLoader extends BackgroundLoader implements Closeable {
    private static final String TAG = "DiskLoader";

    private static final int APP_VERSION = 2;

    private static final int BUFFER_SIZE = 8192;

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
        Snapshot snapshot = getSnapshot(request);
        if (snapshot != null) {
            try {
                if (Logger.VERBOSE) Log.v(TAG, "Loaded " + request + " from disk");

                Metadata metadata = readMetadata(snapshot);
                DiskInputSupplier input = new DiskInputSupplier(request, snapshot);

                listener.onStreamLoaded(input, metadata);

                long expires = metadata.getExpires();
                if (expires != Metadata.NEVER_EXPIRES && System.currentTimeMillis() > expires) {
                    // Cache has expired
                    if (Logger.VERBOSE) Log.v(TAG, request + " has expired, updating");
                    chain.next().load(request.withMetadata(metadata), chain, new NextListener(request, listener));
                }
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

    private Snapshot getSnapshot(LoaderRequest request) throws IOException {
        String key = hashKeyForDisk(request);
        return cache.get(key);
    }

    private Editor getEditor(LoaderRequest request) throws IOException {
        String key = hashKeyForDisk(request);

        Editor editor = cache.edit(key);
        if (editor == null) {
            throw new IOException("File is already being edited");
        }

        return editor;
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

    private class NextListener implements Listener {
        private LoaderRequest request;
        private Listener listener;

        public NextListener(LoaderRequest request, Listener listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void onStreamLoaded(InputSupplier input, final Metadata metadata) {
            try {
                Editor editor = getEditor(request);

                OutputStream os = new BufferedOutputStream(editor.newOutputStream(INPUT_IMAGE), BUFFER_SIZE);
                try {
                    IOUtil.copy(input, os);
                    os.close();
                    writeMetadata(editor, metadata);

                    editor.commit();

                    // Read back the file we just saved
                    run(request, listener, new ListenerFuture.Task() {
                        @Override
                        public void run(Listener listener) throws Exception {
                            DiskInputSupplier input = new DiskInputSupplier(request);
                            listener.onStreamLoaded(input, metadata);
                        }
                    });
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
                listener.onStreamLoaded(input, metadata);
            }
        }

        @Override
        public void onBitmapLoaded(Bitmap b, Metadata metadata) {
            try {
                Editor editor = getEditor(request);

                try {
                    Bitmap.CompressFormat format = BitmapUtils.getCompressFormat(metadata.getContentType());
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
                Editor editor = getEditor(request);

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
            OutputStream os = new BufferedOutputStream(editor.newOutputStream(INPUT_METADATA), BUFFER_SIZE);
            try {
                metadata.writeTo(os);
            } finally {
                IOUtil.closeQuietly(os);
            }
        }

        private void writeBitmap(Editor editor, Bitmap b, Bitmap.CompressFormat format) throws IOException {
            OutputStream os = new BufferedOutputStream(editor.newOutputStream(INPUT_IMAGE), BUFFER_SIZE);
            try {
                b.compress(format, Constants.DEFAULT_COMPRESS_QUALITY, os);
            } finally {
                IOUtil.closeQuietly(os);
            }
        }
    }

    private class DiskInputSupplier implements InputSupplier {
        private String key;
        private Snapshot snapshot;

        public DiskInputSupplier(LoaderRequest request) {
            this(request, null);
        }

        public DiskInputSupplier(LoaderRequest request, Snapshot snapshot) {
            this.key = hashKeyForDisk(request);
            this.snapshot = snapshot;
        }

        @Override
        public InputStream getInput() throws IOException {
            if (snapshot == null) {
                snapshot = cache.get(key);

                if (snapshot == null) {
                    throw new IOException("Snapshot not available");
                }
            }

            // Wrap input stream so we can close the snapshot
            return new FilterInputStream(snapshot.getInputStream(INPUT_IMAGE)) {
                @Override
                public void close() throws IOException {
                    super.close();

                    snapshot.close();
                    snapshot = null;
                }
            };
        }
    }
}

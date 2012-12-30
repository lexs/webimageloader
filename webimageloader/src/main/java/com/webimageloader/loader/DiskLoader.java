package com.webimageloader.loader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.os.Process;
import android.util.Log;

import com.jakewharton.DiskLruCache;
import com.jakewharton.DiskLruCache.Editor;
import com.jakewharton.DiskLruCache.Snapshot;
import com.webimageloader.Constants;
import com.webimageloader.ImageLoader.Logger;
import com.webimageloader.util.ListenerFuture;
import com.webimageloader.util.BitmapUtils;
import com.webimageloader.util.Hasher;
import com.webimageloader.util.IOUtil;
import com.webimageloader.util.InputSupplier;

public class DiskLoader extends SimpleBackgroundLoader implements Closeable {
    private static final String TAG = "DiskLoader";

    private static final int APP_VERSION = 2;

    private static final int BUFFER_SIZE = 8192;

    private static final int INPUT_IMAGE = 0;
    private static final int INPUT_METADATA = 1;
    private static final int VALUE_COUNT = 2;

    private DiskLruCache cache;
    private final Hasher hasher;

    public static DiskLoader open(File directory, long maxSize, int threadCount) throws IOException {
        return new DiskLoader(DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize), threadCount);
    }

    private DiskLoader(DiskLruCache cache, int threadCount) {
        super("Disk", Process.THREAD_PRIORITY_BACKGROUND, threadCount);

        this.cache = cache;
        hasher = new Hasher();
    }

    @Override
    public void close() {
        super.close();

        IOUtil.closeQuietly(cache);
    }

    @Override
    protected void loadInBackground(LoaderWork.Manager manager, LoaderRequest request) throws IOException {
        Snapshot snapshot = getSnapshot(request);
        if (snapshot != null) {
            try {
                if (Logger.VERBOSE) Log.v(TAG, "Loaded " + request + " from disk");

                Metadata metadata = readMetadata(snapshot);
                DiskInputSupplier input = new DiskInputSupplier(request, snapshot);

                manager.deliverStream(input, metadata);

                long expires = metadata.getExpires();
                if (expires != Metadata.NEVER_EXPIRES && System.currentTimeMillis() > expires) {
                    // Cache has expired
                    if (Logger.VERBOSE) Log.v(TAG, request + " has expired, updating");
                    manager.next(request.withMetadata(metadata), new NextListener(request, manager));
                }
            } finally {
                snapshot.close();
            }
        } else {
            // We need to add the next loader
            manager.next(request, new NextListener(request, manager));
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
        private LoaderWork.Manager manager;

        public NextListener(LoaderRequest request, LoaderWork.Manager manager) {
            this.request = request;
            this.manager = manager;
        }

        @Override
        public void onStreamLoaded(InputSupplier input, final Metadata metadata) {
            try {
                Editor editor = getEditor(request);

                OutputStream os = new BufferedOutputStream(editor.newOutputStream(INPUT_IMAGE), BUFFER_SIZE);
                try {
                    try {
                        IOUtil.copy(input, os);
                    } finally {
                        os.close();
                    }

                    writeMetadata(editor, metadata);

                    editor.commit();

                    // Read back the file we just saved
                    run(manager, new ListenerFuture.Task() {
                        @Override
                        public void run() throws Exception {
                            DiskInputSupplier input = new DiskInputSupplier(request);
                            manager.deliverStream(input, metadata);
                        }
                    });
                } catch (IOException e) {
                    // We failed writing to the cache, we can't really do
                    // anything to clean this up
                    editor.abort();
                    manager.deliverError(e);
                }
            } catch (IOException e) {
                // We failed opening the cache, this
                // means that the InputStream is still untouched.
                // Pass it trough to the listener without caching.
                Log.e(TAG, "Failed opening cache", e);
                manager.deliverStream(input, metadata);
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
            manager.deliverBitmap(b, metadata);
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

            manager.deliverNotMotified(metadata);
        }

        @Override
        public void onError(Throwable t) {
            manager.deliverError(t);
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

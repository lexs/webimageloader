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

import com.webimageloader.Request;
import com.webimageloader.concurrent.ExecutorHelper;
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

public class DiskLoader implements Loader, Closeable {
    private static final String TAG = "DiskLoader";

    private static final int APP_VERSION = 1;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    private static final int COMPRESS_QUALITY = 75;

    private static final int INPUT_IMAGE = 0;
    private static final int INPUT_METADATA = 1;
    private static final int VALUE_COUNT = 1;

    private ExecutorHelper executorHelper;

    private DiskLruCache cache;
    private Hasher hasher;

    public static DiskLoader open(File directory, long maxSize) throws IOException {
        return new DiskLoader(DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize));
    }

    private DiskLoader(DiskLruCache cache) {
        this.cache = cache;

        ExecutorService executor = Executors.newSingleThreadExecutor(new PriorityThreadFactory("Disk", Process.THREAD_PRIORITY_BACKGROUND));
        executorHelper = new ExecutorHelper(executor);

        hasher = new Hasher();
    }

    @Override
    public void close() {
        executorHelper.shutdown();

        IOUtil.closeQuietly(cache);
    }

    @Override
    public void load(final Request request, final Iterator<Loader> chain, final Listener listener) {
        executorHelper.run(request, listener, new ListenerFuture.Task() {
            @Override
            public void run(Listener listener) throws Exception {
                String key = hashKeyForDisk(request);
                Snapshot snapshot = cache.get(key);
                if (snapshot != null) {
                    try {
                        Log.v(TAG, "Loaded " + request + " from disk");

                        InputStream is = snapshot.getInputStream(INPUT_IMAGE);
                        listener.onStreamLoaded(is);
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
        });
    }

    @Override
    public void cancel(Request request) {
        executorHelper.cancel(request);
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
                    OutputStream os = new BufferedOutputStream(editor.newOutputStream(INPUT_IMAGE));
                    copy(new BufferedInputStream(is), os);
                    os.close();
                    editor.commit();

                    // Read back the file we just saved
                    executorHelper.run(request, listener, new ReadTask(request));
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
                    OutputStream os = new BufferedOutputStream(editor.newOutputStream(INPUT_IMAGE));
                    b.compress(COMPRESS_FORMAT, COMPRESS_QUALITY, os);
                    os.close();
                    editor.commit();

                    // Read back the file we just saved
                    executorHelper.run(request, listener, new ReadTask(request));
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
    private String hashKeyForDisk(Request request) {
        String key = request.getCacheKey();

        // We don't except to have a lot of threads
        // so it's okay to synchronize access

        synchronized (hasher) {
            return hasher.hash(key);
        }
    }
}

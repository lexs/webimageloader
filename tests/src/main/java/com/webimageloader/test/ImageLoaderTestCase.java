package com.webimageloader.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.AndroidTestCase;

import com.webimageloader.ImageLoader;
import com.webimageloader.ImageLoader.Listener;

@TargetApi(16)
public class ImageLoaderTestCase extends AndroidTestCase {
    private static final int TIMEOUT = 1;

    private static final int TEN_MEGABYTES = 10 * 1024 * 1024;

    private static final String MOCK_SCHEME = "mock://";
    private static final String CORRECT_FILE_PATH = "test.png";
    private static final String CORRECT_MOCK_FILE_PATH = MOCK_SCHEME + CORRECT_FILE_PATH;
    private static final String WRONG_FILE_PATH = MOCK_SCHEME + "error.jpeg";

    private static final Listener<Object> EMPTY_LISTENER = new Listener<Object>() {
        @Override
        public void onSuccess(Object tag, Bitmap b) {}

        @Override
        public void onError(Object tag, Throwable t) {}
    };

    private ImageLoader loader;
    private Bitmap correctFile;

    @Override
    protected void setUp() throws Exception {
        int random = Math.abs(new Random().nextInt());
        File cacheDir = new File(getContext().getCacheDir(), String.valueOf(random));
        loader = new ImageLoader.Builder(getContext())
                .enableDiskCache(cacheDir, TEN_MEGABYTES)
                .enableMemoryCache(TEN_MEGABYTES)
                .addURLSchemeHandler("mock", new MockURLStreamHandler(getContext().getAssets()))
                .build();

        correctFile = BitmapFactory.decodeStream(getContext().getAssets().open(CORRECT_FILE_PATH));
    }

    @Override
    protected void tearDown() throws Exception {
        loader.destroy();
    }

    public void testSameThread() throws IOException {
        Bitmap b = loader.loadBlocking(CORRECT_MOCK_FILE_PATH);

        assertTrue(correctFile.sameAs(b));
    }

    public void testNoDiskCacheFallback() throws IOException {
        File invalidCacheDir = new File("../");

        ImageLoader loader = new ImageLoader.Builder(getContext())
        .enableDiskCache(invalidCacheDir, TEN_MEGABYTES)
        .enableMemoryCache(TEN_MEGABYTES)
        .addURLSchemeHandler("mock", new MockURLStreamHandler(getContext().getAssets()))
        .build();

        Bitmap b = loader.loadBlocking(CORRECT_MOCK_FILE_PATH);

        assertTrue(correctFile.sameAs(b));
    }

    public void testTag() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final Object t = new Object();
        final Holder<Object> h = new Holder<Object>();

        loader.load(t, CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                h.value = tag;

                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                h.value = tag;

                latch.countDown();
            }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));
        assertSame(t, h.value);
    }

    public void testMissingImageAsync() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final Holder<Throwable> h = new Holder<Throwable>();

        loader.load(null, WRONG_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                h.value = t;

                latch.countDown();
            }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));
        assertTrue(h.value instanceof FileNotFoundException);
    }

    public void testMissingImage() throws IOException {
        try {
            loader.loadBlocking(WRONG_FILE_PATH);
            fail("Should have thrown an exception");
        } catch (FileNotFoundException e) {
            // Expected
        }
    }

    public void testMemory() throws IOException {
        loader.loadBlocking(CORRECT_MOCK_FILE_PATH);
        Bitmap b = loader.load(null, CORRECT_MOCK_FILE_PATH, EMPTY_LISTENER);
        assertNotNull(b);
    }

    public void testAsyncSuccess() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final Holder<Bitmap> h = new Holder<Bitmap>();

        loader.load(null, CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                h.value = b;

                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));

        assertNotNull(h.value);
        assertTrue(h.value.sameAs(correctFile));
    }

    public void testAsyncError() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final Holder<Throwable> h = new Holder<Throwable>();

        loader.load(null, WRONG_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                h.value = t;

                latch.countDown();
            }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));
        assertNotNull(h.value);
    }

    public void testMultipleRequests() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            loader.load(null, CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
                @Override
                public void onSuccess(Object tag, Bitmap b) {
                    latch.countDown();
                }

                @Override
                public void onError(Object tag, Throwable t) {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));
    }

    public void testRequestReuse() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);

        final Holder<Bitmap> h1 = new Holder<Bitmap>();
        final Holder<Bitmap> h2 = new Holder<Bitmap>();

        loader.load(null, CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                h1.value = b;

                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                latch.countDown();
            }
        });

        loader.load(null, CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                h2.value = b;

                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));

        assertNotNull(h1.value);
        assertNotNull(h2.value);

        assertSame(h1.value, h2.value);
    }

    public void testRequestCancellation() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Object tag = new Object();

        final Holder<Boolean> failed = new Holder<Boolean>();

        loader.load(tag, WRONG_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                failed.value = true;

                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                failed.value = true;

                latch.countDown();
            }
        });

        loader.load(tag, CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));

        if (failed.value == Boolean.TRUE) {
            fail("First request should have been cancelled");
        }
    }

    public void testTagCancel() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Object tag = new Object();

        // Use wrong path so it will fail faster
        loader.load(tag, WRONG_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                latch.countDown();
            }
        });

        loader.cancel(tag);

        // We should not get any callbacks
        assertFalse(latch.await(TIMEOUT, TimeUnit.SECONDS));
    }

    private static class MockURLStreamHandler extends URLStreamHandler {
        private AssetManager assets;

        public MockURLStreamHandler(AssetManager assets) {
            this.assets = assets;
        }

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return new MockURLConnection(assets, url);
        }
    }

    private static class MockURLConnection extends URLConnection {
        private AssetManager assets;
        private String filename;

        protected MockURLConnection(AssetManager assets, URL url) {
            super(url);

            this.assets = assets;
            filename = url.getAuthority();
        }

        @Override
        public void connect() throws IOException {}

        @Override
        public InputStream getInputStream() throws IOException {
            return assets.open(filename);
        }
    }

    private static class Holder<T> {
        public T value;
    }
}

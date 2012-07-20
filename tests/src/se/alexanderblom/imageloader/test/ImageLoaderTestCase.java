package se.alexanderblom.imageloader.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import se.alexanderblom.imageloader.ImageLoader;
import se.alexanderblom.imageloader.ImageLoader.Listener;
import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;

public class ImageLoaderTestCase extends AndroidTestCase {
    private static final int TEN_MEGABYTES = 10 * 1024 * 1024;

    private static final String CORRECT_FILE_PATH = "test.png";
    private static final String CORRECT_MOCK_FILE_PATH = "content://mock/" + CORRECT_FILE_PATH;
    private static final String WRONG_FILE_PATH = "content://mock/error.jpeg";

    private static final Listener<Object> EMPTY_LISTENER = new Listener<Object>() {
        @Override
        public void onSuccess(Object tag, Bitmap b) {}

        @Override
        public void onError(Object tag, Throwable t) {}
    };

    private ImageLoader loader;
    private Bitmap correctFile;

    private ContentResolver createMockResolver() {
        MockContentResolver resolver = new MockContentResolver();
        resolver.addProvider("mock", new MockProvider(getContext().getAssets()));

        return resolver;
    }

    @Override
    protected void setUp() throws Exception {
        int random = Math.abs(new Random().nextInt());
        File cacheDir = new File(getContext().getCacheDir(), String.valueOf(random));
        loader = new ImageLoader.Builder()
                .enableDiskCache(cacheDir, TEN_MEGABYTES)
                .enableMemoryCache(TEN_MEGABYTES)
                .supportResources(createMockResolver())
                .build();

        correctFile = BitmapFactory.decodeStream(getContext().getAssets().open(CORRECT_FILE_PATH));
    }

    @Override
    protected void tearDown() throws Exception {
        loader.destroy();
    }

    public void testSameThread() throws IOException {
        Bitmap b = loader.loadSynchronously(CORRECT_MOCK_FILE_PATH);

        assertTrue(correctFile.sameAs(b));
    }

    public void testNoDiskCacheFallback() throws IOException {
        File invalidCacheDir = new File("../");

        ImageLoader loader = new ImageLoader.Builder()
        .enableDiskCache(invalidCacheDir, TEN_MEGABYTES)
        .enableMemoryCache(TEN_MEGABYTES)
        .supportResources(createMockResolver())
        .build();

        Bitmap b = loader.loadSynchronously(CORRECT_MOCK_FILE_PATH);

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

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertSame(t, h.value);
    }

    public void testWrongPath() {
        try {
            loader.loadSynchronously(WRONG_FILE_PATH);
            fail("Should have thrown an exception");
        } catch (IOException e) {
        }
    }

    public void testMemory() throws IOException {
        loader.loadSynchronously(CORRECT_MOCK_FILE_PATH);
        Bitmap b = loader.load(new Object(), CORRECT_MOCK_FILE_PATH, EMPTY_LISTENER);
        assertNotNull(b);
    }

    public void testAsyncSuccess() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final Holder<Bitmap> h = new Holder<Bitmap>();

        loader.load(new Object(), CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
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

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        assertNotNull(h.value);
        assertTrue(h.value.sameAs(correctFile));
    }

    public void testAsyncError() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final Holder<Throwable> h = new Holder<Throwable>();

        loader.load(new Object(), WRONG_FILE_PATH, new Listener<Object>() {
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

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNotNull(h.value);
    }

    public void testMultipleRequests() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            loader.load(new Object(), CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
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

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    public void testRequestReuse() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);

        final Holder<Bitmap> h1 = new Holder<Bitmap>();
        final Holder<Bitmap> h2 = new Holder<Bitmap>();

        loader.load(new Object(), CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
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

        loader.load(new Object(), CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
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

        assertTrue(latch.await(10, TimeUnit.SECONDS));

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

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        if (failed.value == Boolean.TRUE) {
            fail("First request should have been cancelled");
        }
    }

    private static class MockProvider extends MockContentProvider {
        private AssetManager assets;

        public MockProvider(AssetManager assets) {
            this.assets = assets;
        }

        @Override
        public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
            try {
                return assets.openFd(uri.getLastPathSegment());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public AssetFileDescriptor openTypedAssetFile(Uri url, String mimeType, Bundle opts) {
            try {
                return openAssetFile(url, null);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class Holder<T> {
        public T value;
    }
}

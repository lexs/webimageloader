package se.alexanderblom.imageloader.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import se.alexanderblom.imageloader.ImageLoader;
import se.alexanderblom.imageloader.ImageLoader.Listener;
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

    @Override
    protected void setUp() throws Exception {
        int TEN_MEGABYTES = 10 * 1024 * 1024;

        MockContentResolver resolver = new MockContentResolver();
        resolver.addProvider("mock", new MockProvider(getContext().getAssets()));

        int random = Math.abs(new Random().nextInt());
        File cacheDir = new File(getContext().getCacheDir(), String.valueOf(random));
        loader = new ImageLoader.Builder()
                .enableDiskCache(cacheDir, TEN_MEGABYTES)
                .enableMemoryCache(TEN_MEGABYTES)
                .supportResources(resolver)
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

    public void testTag() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final Object t = new Object();

        loader.load(t, CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                assertSame(t, tag);

                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                assertSame(t, tag);

                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
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

        loader.load(new Object(), CORRECT_MOCK_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                assertTrue(b.sameAs(correctFile));

                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                fail("Should not error");

                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    public void testAsyncError() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        loader.load(new Object(), WRONG_FILE_PATH, new Listener<Object>() {
            @Override
            public void onSuccess(Object tag, Bitmap b) {
                fail("Should have errored");

                latch.countDown();
            }

            @Override
            public void onError(Object tag, Throwable t) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
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
                    t.printStackTrace();

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

        loader.load(new Object(), WRONG_FILE_PATH, new Listener<Object>() {
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

        assertNotNull(h1.value);
        assertNotNull(h2.value);

        assertSame(h1.value, h2.value);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
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

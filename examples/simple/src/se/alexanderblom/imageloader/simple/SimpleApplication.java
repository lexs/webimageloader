package se.alexanderblom.imageloader.simple;

import java.io.File;

import se.alexanderblom.imageloader.ImageLoader;
import android.app.Application;

public class SimpleApplication extends Application {
    public static final String IMAGE_LOADER_SERVICE = "image_loader";

    private ImageLoader imageLoader;

    @Override
    public void onCreate() {
        super.onCreate();

        int TEN_MEGABYTES = 10 * 1024 * 1024;

        File cacheDir = new File(getExternalCacheDir(), "images");
        imageLoader = new ImageLoader.Builder()
                .enableDiskCache(cacheDir, TEN_MEGABYTES)
                .enableMemoryCache(TEN_MEGABYTES)
                .supportResources(getContentResolver())
                .build();
    }

    @Override
    public Object getSystemService(String name) {
        if (IMAGE_LOADER_SERVICE.equals(name)) {
            return imageLoader;
        } else {
            return super.getSystemService(name);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        imageLoader.destroy();
    }
}

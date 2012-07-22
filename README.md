WebImageLoader
==============

WebImageLoader is a library designed to take to hassle out of handling images on the web.

Usage
=====

WebImageLoader makes no assumptions about how you want to use your loader so you'll have to
create it yourself.

```java
// Get memory class of this device, exceeding this amount will throw an
// OutOfMemory exception.
ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
int memClass = am.getMemoryClass();

// Use part of the available memory for memory cache.
final int memoryCacheSize = 1024 * 1024 * memClass / 8;

File cacheDir = new File(getExternalCacheDir(), "images");
ImageLoader imageLoader = new ImageLoader.Builder()
        .enableDiskCache(cacheDir, 10 * 1024 * 1024)
        .enableMemoryCache(memoryCacheSize);
```

You can also use the provided Application class if you want.

```xml
<application android:name="com.webimageloader.ext.ImageLoaderApplication">
    <!-- Your app -->
</application>
```

This will create a ImageLoader with reasonable defaults.

```java
ImageLoader imageLoader = ImageLoaderApplication.getLoader(context);
```

Loading images
--------------

Loading images is simple if you want it to be.

```java
// This will show a nice fadein when the image has loaded
new ImageHelper(context, imageLoader)
        .load(imageView, "http://example.com/image.png");
```

You can also use specify a loading and failure image

```java
new ImageHelper(context, imageLoader)
        .setLoadingResource(R.drawable.loading)
        .setErrorResource(R.drawable.error)
        .load(imageView, "http://example.com/image.png");
```

Loading images can also be done more explicit if needed.

```java
Bitmap b = loader.load(imageView, "http://example.com/image.png", new Listener<ImageView>() {
    @Override
    public void onSuccess(ImageView v, Bitmap b) {
        // Everything went well
        v.setImageBitmap(b);
    }

    @Override
    public void onError(ImageView v, Throwable t) {
        // Something went wrong
        Log.d("MyApp", "Failed to load image", t);
    }
});

// Did we get an image immediately?
if (b != null) {
    imageView.setImageBitmap(b);
}
```

Developed By
============

* Alexander Blom - <me@alexanderblom.se>
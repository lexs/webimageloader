WebImageLoader
==============

WebImageLoader is a library designed to take to hassle out of handling images on the web. It has the following features:

* Images are downloaded on a background thread pool and saved to disk and memory.
* Disk and memory cache size is configurable and can even be reconfigured on the fly.
* Separate thread do load images back from disk after being cached, reducing I/O bottlenecks on most phones.
* Reusing requests when the same image is requested multiple times.
* Respects cache-control and expires headers and will refetch images when they expire (using conditional get).
* Support image transformations which are also cached to disk and memory.
* Support to do synchronous fetches while still taking advantage of the cache.
* Support for download progress callbacks.
* Easy setup without singletons.
* Compatible with API level 7 and up.
* Only depends on [DiskLruCache][DiskLruCache].

Usage
=====

Use the builder to build an ImageLoader that suits your needs.

```java
// Get memory class of this device, exceeding this amount will throw an
// OutOfMemory exception.
ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
int memClass = am.getMemoryClass();

// Use part of the available memory for memory cache.
final int memoryCacheSize = 1024 * 1024 * memClass / 8;

File cacheDir = new File(getExternalCacheDir(), "images");
ImageLoader imageLoader = new ImageLoader.Builder(context)
        .enableDiskCache(cacheDir, 10 * 1024 * 1024)
        .enableMemoryCache(memoryCacheSize).build();
```

Or use the provided Applications class for convenience and reasonable defaults (which you can override!)

```xml
<application android:name="com.webimageloader.ext.ImageLoaderApplication">
    <!-- Your app -->
</application>
```

Retrive your loader like this.

```java
ImageLoader imageLoader = ImageLoaderApplication.getLoader(context);
```

Loading images
--------------

Loading images is simple if you want it to be.

```java
// This will show a nice fade in when the image has loaded
new ImageHelper(context, imageLoader)
        .setFadeIn(true)
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

Transformations
---------------

You can transform (and cache!) the images you get.

```java
final int width = 100;
final int height = 100;
        
Transformation t = new SimpleTransformation() {
    @Override
    public String getIdentifier() {
        // Pass a unique identifier for caching
        return "scale-" + width + "x" + height;
    }
    
    @Override
    public Bitmap transform(Bitmap b) {
        return Bitmap.createScaledBitmap(b, width, height, true);
    }
};

new ImageHelper(this, imageLoader)
        .load(imageView, "http://example.com/image.png", t);
```

Progress
========

Progress is easy if you have a [ProgressBar][ProgressBar], it will only be shown when when needed.

```java
// Automatically update the progress bar
new ImageHelper(context, imageLoader)
        .load(imageView, progressBar, "http://example.com/image.png");
```

Or handle progress yourself.

```java
Bitmap b = loader.load(imageView, "http://example.com/image.png", new Listener<ImageView>() {
    @Override
    public void onSuccess(ImageView v, Bitmap b) {
        // Same as above
    }

    @Override
    public void onError(ImageView v, Throwable t) {
        // Same as above
    }
}, new ProgressListener() {
    @Override
    public void onProgress(float value) {
        // value is in the range 0f-1f
    }
});
```

Obtaining
=========

You can include the library by [downloading the .jar][jar] and also adding [DiskLruCache][DiskLruCache] to your project.

If you are a Maven user, simply add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>com.webimageloader</groupId>
    <artifactId>webimageloader</artifactId>
    <version>1.2.0</version>
</dependency>
``` 

Developed By
============

* Alexander Blom - [alexanderblom.se](http://alexanderblom.se)

License
=======
WebImageLoader is licensed under Apache 2.0. If you find this library useful feel free to send me a tweet (or not).

    Copyright 2012 Alexander Blom

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[jar]: http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22webimageloader%22
[DiskLruCache]: https://github.com/JakeWharton/DiskLruCache
[ProgressBar]: https://developer.android.com/reference/android/widget/ProgressBar.html

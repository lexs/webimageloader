package com.webimageloader.example.simple;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.webimageloader.ImageLoader;
import com.webimageloader.ImageLoader.Listener;
import com.webimageloader.URLUtil;
import com.webimageloader.example.simple.R;
import com.webimageloader.ext.ImageLoaderApplication;
import com.webimageloader.transformation.SimpleTransformation;
import com.webimageloader.transformation.Transformation;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private static final String IMAGE1 = "http://images5.fanpop.com/image/photos/31100000/Picturess-Lol-random-31165113-600-600.png";
    private static final String IMAGE3 = "http://www.adiumxtras.com/images/pictures/chuck_norris_random_fact_generator_6_3957_2224_image_2578.jpg";
    private static final String IMAGE4 = "http://luxrerum.icmm.csic.es/files/images/random04.jpg";

    private ImageLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        loader = (ImageLoader) getApplicationContext().getSystemService(ImageLoaderApplication.IMAGE_LOADER_SERVICE);

        View reloadButton = findViewById(R.id.button_reload);
        reloadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loadImage();
            }
        });

        loadImage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        loader.destroy();
    }

    private void loadImage() {
        loadImage(R.id.image1, IMAGE1);
        loadImage(R.id.image2, URLUtil.resourceUrl(this, android.R.drawable.ic_media_next));
        loadImage(R.id.image3, IMAGE3);
        loadImage(R.id.image4, IMAGE4);
    }

    private void loadImage(int id, String url) {
        ImageView imageView = (ImageView) findViewById(id);

        Transformation t = new ShrinkTransformation();
        Bitmap b = loader.load(imageView, url, t, new Listener<ImageView>() {
            @Override
            public void onSuccess(ImageView v, Bitmap b) {
                TransitionDrawable d = new TransitionDrawable(new Drawable[] {
                        new ColorDrawable(android.R.color.transparent),
                        new BitmapDrawable(getResources(), b) });

                v.setImageDrawable(d);
                d.startTransition(300);
            }

            @Override
            public void onError(ImageView v, Throwable t) {
                Log.d(TAG, "Error loading bitmap", t);
            }
        });

        imageView.setImageBitmap(b);
    }

    private static class ShrinkTransformation extends SimpleTransformation {
        @Override
        public String getIdentifier() {
            return "shrink";
        }

        @Override
        public Bitmap transform(Bitmap b) {
            return Bitmap.createScaledBitmap(b, 100, 100, true);
        }

    }

    private static class ResizeTransformation implements Transformation {
        private int width;
        private int height;

        public ResizeTransformation(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public String getIdentifier() {
            return "resize-" + width + "x" + height;
        }

        @Override
        public Bitmap transform(InputStream is) {
            is = new BufferedInputStream(is);

            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, width, height);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            try {
                is.reset();
                return BitmapFactory.decodeStream(is, null, options);
            } catch (IOException e) {
                Log.e(TAG, "Error", e);

                return null;
            }

        }

        @Override
        public Bitmap transform(Bitmap b) {
            return Bitmap.createScaledBitmap(b, width, height, true);
        }

        private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                if (width > height) {
                    inSampleSize = Math.round((float) height / (float) reqHeight);
                } else {
                    inSampleSize = Math.round((float) width / (float) reqWidth);
                }
            }
            return inSampleSize;
        }

        private static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(res, resId, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeResource(res, resId, options);
        }
    }
}

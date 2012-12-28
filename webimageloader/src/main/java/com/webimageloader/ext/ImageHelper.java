package com.webimageloader.ext;

import com.webimageloader.ImageLoader;
import com.webimageloader.ImageLoader.Listener;
import com.webimageloader.transformation.Transformation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.Log;
import android.widget.ImageView;

/**
 * Helper class for loading images into a {@link ImageView}
 *
 * @author Alexander Blom <alexanderblom.se>
 */
public class ImageHelper {
    private static final String TAG = "ImageHelper";

    private static final int DEFAULT_FADE_DURATION = 300;

    private Context context;
    private ImageLoader loader;

    private int loadingResource;
    private int errorResource;
    private boolean fadeIn = false;
    private int fadeDuration = DEFAULT_FADE_DURATION;

    private LoadingListener listener;


    /**
     * Create a new {@link ImageHelper} using the specified loader
     *
     * @param context the context when getting resources
     * @param loader the load to use
     */
    public ImageHelper(Context context, ImageLoader loader) {
        this.context = context;
        this.loader = loader;

        listener = new LoadingListener();
    }

    /**
     * Set an image to be displayed when an image is loading
     *
     * @param loadingResource the resource to use
     *
     * @return this helper
     */
    public ImageHelper setLoadingResource(int loadingResource) {
        this.loadingResource = loadingResource;

        return this;
    }

    /**
     * Set an image to be displayed if an error occurred
     *
     * @param errorResource the resource to use
     * @return this helper
     */
    public ImageHelper setErrorResource(int errorResource) {
        this.errorResource = errorResource;

        return this;
    }

    /**
     * Set whether to fade in after loading
     *
     * @param fadeIn true or false
     * @return this helper
     */
    public ImageHelper setFadeIn(boolean fadeIn) {
        this.fadeIn = fadeIn;

        return this;
    }

    /**
     * Enable and set the fade in duration.
     *
     * @param duration transition duration in milliseconds.
     * @return this helper
     */
    public ImageHelper setFadeIn(int duration) {
        this.fadeIn = true;
        this.fadeDuration = duration;

        return this;
    }

    /**
     * Load the specified url into this {@link ImageView}.
     *
     * @param v the target view
     * @param url the url to load
     * @return this helper
     *
     * @see #load(ImageView, String, Transformation)
     */
    public ImageHelper load(ImageView v, String url) {
        return load(v, url, null);
    }

    /**
     * Load the specified url into this {@link ImageView}.
     *
     * @param v the target view
     * @param url the url to load
     * @param transformation transformation to apply, can be null
     * @return this helper
     */
    public ImageHelper load(ImageView v, String url, Transformation transformation) {
        Bitmap b = loader.load(v, url, transformation, listener);

        if (b != null) {
            v.setImageBitmap(b);
        } else if (loadingResource != 0) {
            v.setImageResource(loadingResource);
        } else {
            v.setImageBitmap(null);
        }

        return this;
    }

    private class LoadingListener implements Listener<ImageView> {
        @Override
        public void onSuccess(ImageView v, Bitmap b) {
            if (!fadeIn) {
                v.setImageBitmap(b);
            } else {
            	v.setImageBitmap(null);
            	
                Drawable old = v.getDrawable();
                if (old == null) {
                    old = new ColorDrawable(android.R.color.transparent);
                }

                TransitionDrawable d = new TransitionDrawable(new Drawable[] {
                        old,
                        new BitmapDrawable(context.getResources(), b)
                });

                v.setImageDrawable(d);
                d.startTransition(fadeDuration);
            }
        }

        @Override
        public void onError(ImageView v, Throwable t) {
            Log.d(TAG, "Error loading bitmap", t);
            if (errorResource > 0) {
                v.setImageResource(errorResource);
            }
        }
    }
}

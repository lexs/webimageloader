package com.webimageloader.sample.patterns;

import android.graphics.Bitmap;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import com.webimageloader.ImageLoader;
import com.webimageloader.ext.ImageLoaderApplication;

public class DetailsActivity extends FragmentActivity {
    public static final String EXTRA_URL = "extra_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View backgroundView = findViewById(android.R.id.content);

        ImageLoader imageLoader = ImageLoaderApplication.getLoader(this);
        Bitmap b = imageLoader.load(backgroundView, getIntent().getStringExtra(EXTRA_URL), new ImageLoader.Listener<View>() {
            @Override
            public void onSuccess(View v, Bitmap b) {
                setImage(v, b);
            }

            @Override
            public void onError(View v, Throwable t) {
            }
        });

        if (b != null) {
            setImage(backgroundView, b);
        }
    }

    @SuppressWarnings("deprecation")
    private void setImage(View v, Bitmap b) {
        BitmapDrawable d = new BitmapDrawable(getResources(), b);
        d.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        v.setBackgroundDrawable(d);
    }
}

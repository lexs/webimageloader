package com.webimageloader.example.simple;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.webimageloader.ImageLoader;
import com.webimageloader.ext.ImageHelper;
import com.webimageloader.ext.ImageLoaderApplication;
import com.webimageloader.transformation.SimpleTransformation;
import com.webimageloader.transformation.Transformation;

public class MainActivity extends Activity {
    private static final String IMAGE1 = "https://raw.github.com/lexs/webimageloader/develop/extras/numbers/1.png";
    private static final String IMAGE2 = "https://raw.github.com/lexs/webimageloader/develop/extras/numbers/2.png";
    private static final String IMAGE3 = "https://raw.github.com/lexs/webimageloader/develop/extras/numbers/3.png";
    private static final String IMAGE4 = "https://raw.github.com/lexs/webimageloader/develop/extras/numbers/4.png";

    private ImageLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        loader = ImageLoaderApplication.getLoader(this);

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
        loadImage(R.id.image2, IMAGE2);
        loadImage(R.id.image3, IMAGE3);
        loadImage(R.id.image4, IMAGE4);
    }

    private void loadImage(int id, String url) {
        ImageView imageView = (ImageView) findViewById(id);

        Transformation t = new FlipTransformation();
        new ImageHelper(this, loader)
                .setFadeIn(true)
                .load(imageView, url, t);
    }

    private static class FlipTransformation extends SimpleTransformation {
        @Override
        public String getIdentifier() {
            return "flip";
        }

        @Override
        public Bitmap transform(Bitmap b) {
            Matrix m = new Matrix();
            m.preScale(-1, 1);

            return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
        }
    }
}

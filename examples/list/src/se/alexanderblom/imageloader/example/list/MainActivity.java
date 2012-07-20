package se.alexanderblom.imageloader.example.list;

import java.io.File;
import java.util.Random;

import se.alexanderblom.imageloader.ImageLoader;
import se.alexanderblom.imageloader.ImageLoader.Listener;
import android.app.ListActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends ListActivity {
    private static final String TAG = "MainActivity";

    private static final int NUM_IMAGES = 100;
    private static final int CACHE_SIZE = 10 * 1024 * 1024;
    private ImageLoader imageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int random = Math.abs(new Random().nextInt());
        File cacheDir = new File(getCacheDir(), String.valueOf(random));
        imageLoader = new ImageLoader.Builder()
                .enableDiskCache(cacheDir, CACHE_SIZE)
                .enableMemoryCache(CACHE_SIZE)
                .build();

        setListAdapter(new Adapter(this));
    }

    private class Adapter extends BaseAdapter {
        private LayoutInflater inflater;

        public Adapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return NUM_IMAGES;
        }

        @Override
        public String getItem(int position) {
            return getImageUrl(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            if (v == null) {
                v = inflater.inflate(R.layout.list_item, parent, false);
            }

            ImageView imageView = (ImageView) v.findViewById(R.id.image);
            TextView textView = (TextView) v.findViewById(R.id.text);

            setImage(imageView, getImageUrl(position));
            textView.setText("Image #" + position);

            return v;
        }

        private String getImageUrl(int position) {
            return "http://static.nikreiman.com/numbers/" + position + ".png";
        }

        private void setImage(ImageView v, String url) {
            Bitmap b = imageLoader.load(v, url, new Listener<ImageView>() {
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

            v.setImageBitmap(b);
        }
    }
}

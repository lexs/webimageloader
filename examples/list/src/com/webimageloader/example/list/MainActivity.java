package com.webimageloader.example.list;

import java.io.File;
import java.util.Random;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.webimageloader.ImageLoader;
import com.webimageloader.example.list.R;
import com.webimageloader.ext.ImageHelper;
import com.webimageloader.loader.MemoryCache;

public class MainActivity extends ListActivity {
    private static final int NUM_IMAGES = 100;
    private static final int CACHE_SIZE = 10 * 1024 * 1024;

    private ImageLoader imageLoader;
    private ImageHelper imageHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        int random = Math.abs(new Random().nextInt());
        File cacheDir = new File(getCacheDir(), String.valueOf(random));
        imageLoader = new ImageLoader.Builder()
                .enableDiskCache(cacheDir, CACHE_SIZE)
                .enableMemoryCache(CACHE_SIZE)
                .build();

        imageHelper = new ImageHelper(this, imageLoader);
        imageHelper.setLoadingResource(android.R.drawable.sym_def_app_icon);

        StatsView statsView = (StatsView) findViewById(R.id.stats);
        statsView.setMemoryCache(imageLoader.getMemoryCache());

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
            return "https://raw.github.com/lexs/webimageloader/develop/extras/numbers/" + position + ".png";
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

            imageHelper.load(imageView, getItem(position));
            textView.setText("Image #" + position);

            return v;
        }
    }

    private static class StatsView extends TextView {
        private MemoryCache memoryCache;

        public StatsView(Context context) {
            super(context);
        }

        public StatsView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public StatsView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public void setMemoryCache(MemoryCache memoryCache) {
            this.memoryCache = memoryCache;

            scheduleUpdates();
        }

        private void scheduleUpdates() {
            post(new Runnable() {
                @Override
                public void run() {
                    MemoryCache.DebugInfo info = memoryCache.getDebugInfo();

                    String text = "Memory cache stats\n"
                            + "Hit count: " + info.hitCount + "\n"
                            + "Miss count: " + info.missCount + "\n"
                            + "Put count: " + info.putCount + "\n"
                            + "Eviction count: " + info.evictionCount;


                    setText(text);

                    postDelayed(this, 1000);
                }
            });
        }
    }
}

package com.webimageloader.sample.simple;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.webimageloader.ImageLoader;
import com.webimageloader.ext.ImageHelper;
import com.webimageloader.ext.ImageLoaderApplication;

public class MainActivity extends ListActivity {
    private static final int NUM_IMAGES = 100;

    private ImageLoader imageLoader;
    private ImageHelper imageHelper;

    private Adapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageLoader.Logger.logAll();

        imageLoader = ImageLoaderApplication.getLoader(this);

        imageHelper = new ImageHelper(this, imageLoader);
        imageHelper.setFadeIn(true);
        imageHelper.setLoadingResource(android.R.drawable.sym_def_app_icon);

        adapter = new Adapter(this);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String url = adapter.getItem(position);
        Intent intent = new Intent(this, DetailsActivity.class)
                .putExtra(DetailsActivity.ARG_URL, url);

        startActivity(intent);
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
}

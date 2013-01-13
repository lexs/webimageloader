package com.webimageloader.sample.patterns;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.webimageloader.ImageLoader;
import com.webimageloader.ext.ImageHelper;
import com.webimageloader.ext.ImageLoaderApplication;
import com.webimageloader.ext.ListImageLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbsListLoader implements LoaderManager.LoaderCallbacks<List<String>> {
    private static final String TAG = "AbsListLoader";

    private Context context;
    private AbsListView listView;

    private String url;

    public AbsListLoader(Fragment fragment, AbsListView listView, String url) {
        this.listView = listView;
        this.context = fragment.getActivity();
        this.url = url;

        listView.setDrawSelectorOnTop(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                String url = (String) parent.getItemAtPosition(position);
                Intent intent = new Intent(context, DetailsActivity.class)
                        .putExtra(DetailsActivity.EXTRA_URL, url);

                if (Build.VERSION.SDK_INT >= 16) {
                    // Use cool animation
                    ActivityOptions animation = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
                    context.startActivity(intent, animation.toBundle());
                } else {
                    context.startActivity(intent);
                }
            }
        });

        fragment.getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<List<String>> onCreateLoader(int i, Bundle bundle) {
        return new PatternsLoader(context, url);
    }

    @Override
    public void onLoadFinished(Loader<List<String>> loader, List<String> images) {
        ImageLoader imageLoader = ImageLoaderApplication.getLoader(context);
        listView.setAdapter(new Adapter(context, images, new ListImageLoader(imageLoader, listView)));
    }

    @Override
    public void onLoaderReset(Loader<List<String>> loader) {
    }

    private class Adapter extends ArrayAdapter<String> {
        private LayoutInflater inflater;
        private ImageHelper imageHelper;

        public Adapter(Context context, List<String> objects, ListImageLoader imageLoader) {
            super(context, 0, objects);

            inflater = LayoutInflater.from(context);
            imageHelper = new ImageHelper(context, imageLoader)
                    .setFadeIn(true)
                    .setDrawableCreator(new ImageHelper.DrawableCreator() {
                        @Override
                        public Drawable createDrawable(Context context, Bitmap b) {
                            BitmapDrawable d = new BitmapDrawable(context.getResources(), b);
                            d.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

                            return d;
                        }
                    });

            // This will use getView() for preloading
            imageLoader.setPreloadAdapter(this);
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            if (v == null) {
                v = inflater.inflate(R.layout.list_item, parent, false);
            }

            String url = getItem(position);

            ImageView patternView = (ImageView) v.findViewById(R.id.image);
            imageHelper.load(patternView, url);

            return v;
        }
    }

    private static class PatternsLoader extends AsyncLoader<List<String>> {
        private String url;

        public PatternsLoader(Context context, String url) {
            super(context);

            this.url = url;
        }

        @Override
        public List<String> loadInBackground() {
            try {
                InputStream is = new java.net.URL(url).openStream();
                String content = toString(new BufferedInputStream(is));
                return parseResponse(content);
            } catch (IOException e) {
                Log.e(TAG, "Failed to fetch images", e);

                return Collections.emptyList();
            }
        }

        private List<String> parseResponse(String content) throws IOException {
            try {
                ArrayList<String> images = new ArrayList<String>();

                JSONArray jsonImages = new JSONArray(content);
                for (int i = 0; i < jsonImages.length(); i++) {
                    JSONObject jsonImage = jsonImages.getJSONObject(i);

                    String url = jsonImage.getString("imageUrl");
                    images.add(url);
                }

                return images;
            } catch (JSONException e) {
                throw new IOException("Failed to parse json", e);
            }
        }

        private static String toString(InputStream is) throws IOException {
            final char[] buffer = new char[1024];
            final StringBuilder out = new StringBuilder();
            try {
                final Reader in = new InputStreamReader(is, "UTF-8");
                try {
                    while (true) {
                        int rsz = in.read(buffer, 0, buffer.length);
                        if (rsz < 0)
                            break;
                        out.append(buffer, 0, rsz);
                    }
                } finally {
                    in.close();
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            return out.toString();
        }
    }
}

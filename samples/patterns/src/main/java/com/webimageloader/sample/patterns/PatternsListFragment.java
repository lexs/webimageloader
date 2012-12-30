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
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import com.webimageloader.ImageLoader;
import com.webimageloader.ext.ImageHelper;
import com.webimageloader.ext.ImageLoaderApplication;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatternsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<String>> {
    private static final String TAG = "PatternsListFragment";

    private static final String URL = "http://www.colourlovers.com/api/patterns/top?numResults=100&format=json";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setDrawSelectorOnTop(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String url = (String) l.getItemAtPosition(position);
        Intent intent = new Intent(getActivity(), DetailsActivity.class)
                .putExtra(DetailsActivity.EXTRA_URL, url);

        if (Build.VERSION.SDK_INT >= 16) {
            // Use cool animation
            ActivityOptions animation = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
            getActivity().startActivity(intent, animation.toBundle());
        } else {
            startActivity(intent);
        }
    }

    @Override
    public Loader<List<String>> onCreateLoader(int i, Bundle bundle) {
        return new PatternsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<String>> loader, List<String> images) {
        setListAdapter(new Adapter(getActivity(), images));
    }

    @Override
    public void onLoaderReset(Loader<List<String>> loader) {
    }

    private class Adapter extends ArrayAdapter<String> {
        private LayoutInflater inflater;
        private ImageLoader imageLoader;
        private ImageHelper imageHelper;

        public Adapter(Context context, List<String> objects) {
            super(context, 0, objects);

            inflater = LayoutInflater.from(context);
            imageLoader = ImageLoaderApplication.getLoader(getContext());
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
        public PatternsLoader(Context context) {
            super(context);
        }

        @Override
        public List<String> loadInBackground() {
            try {
                InputStream is = new URL(URL).openStream();
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

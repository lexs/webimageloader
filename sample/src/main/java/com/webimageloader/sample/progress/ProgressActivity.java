package com.webimageloader.sample.progress;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.webimageloader.ImageLoader;
import com.webimageloader.Request;
import com.webimageloader.ext.ImageHelper;
import com.webimageloader.ext.ImageLoaderApplication;
import com.webimageloader.sample.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;

public class ProgressActivity extends Activity {
    private static final String TAG = "ProgressActivity";

    private static final String INSTAGRAM_URL = "https://api.instagram.com/v1/media/popular?client_id=f005c552182d434e8532e11c8af82f6e";

    private ImageLoader imageLoader;
    private ImageHelper imageHelper;

    private ImageView imageView;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_progress);

        ImageLoader.Logger.logAll();

        imageLoader = ImageLoaderApplication.getLoader(this);

        imageHelper = new ImageHelper(this, imageLoader);
        imageHelper.setFadeIn(true);

        imageView = (ImageView) findViewById(R.id.image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImage();
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setMax(100);

        showImage();
    }

    private void showImage() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                imageView.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
            }

            @Override
            protected String doInBackground(Void... params) {
                try {
                    InputStream is = new URL(INSTAGRAM_URL).openStream();
                    String content = toString(new BufferedInputStream(is));
                    return parseResponse(content);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to fetch images", e);

                    return null;
                }
            }

            @Override
            protected void onPostExecute(String url) {
                if (url == null) {
                    Toast.makeText(ProgressActivity.this, "Failed to fetch image url", Toast.LENGTH_SHORT).show();
                } else {
                    progressBar.setIndeterminate(false);
                    imageHelper.load(imageView, progressBar, new Request(url));
                }
            }

            private String parseResponse(String content) throws IOException {
                try {
                    JSONObject json = new JSONObject(content);

                    return json.getJSONArray("data").getJSONObject(0).getJSONObject("images")
                            .getJSONObject("standard_resolution").getString("url");
                } catch (JSONException e) {
                    throw new IOException("Failed to parse json", e);
                }
            }

            private String toString(InputStream is) throws IOException {
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
        }.execute();
    }
}

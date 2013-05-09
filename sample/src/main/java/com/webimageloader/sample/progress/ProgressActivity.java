package com.webimageloader.sample.progress;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.google.gson.JsonParser;
import com.webimageloader.ImageLoader;
import com.webimageloader.Request;
import com.webimageloader.ext.ImageHelper;
import com.webimageloader.ext.ImageLoaderApplication;
import com.webimageloader.sample.R;

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
                    Reader reader = new InputStreamReader(new URL(INSTAGRAM_URL).openStream(), "utf-8");
                    return new JsonParser().parse(reader).getAsJsonObject()
                            .getAsJsonArray("data")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("images")
                            .getAsJsonObject("standard_resolution")
                            .get("url").getAsString();
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
        }.execute();
    }
}

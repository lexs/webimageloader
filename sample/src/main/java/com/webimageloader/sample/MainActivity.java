package com.webimageloader.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import com.webimageloader.ext.ImageLoaderApplication;
import com.webimageloader.sample.numbers.NumbersActivity;
import com.webimageloader.sample.patterns.PatternsActivity;
import com.webimageloader.sample.progress.ProgressActivity;
import com.webimageloader.util.IOUtil;

import java.io.File;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        findViewById(R.id.numbers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NumbersActivity.class));
            }
        });

        findViewById(R.id.patterns).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PatternsActivity.class));
            }
        });

        findViewById(R.id.progress).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProgressActivity.class));
            }
        });

        findViewById(R.id.clear_cache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emptyCache();
            }
        });
    }

    private void emptyCache() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                findViewById(R.id.clear_cache).setEnabled(false);
            }

            @Override
            protected Void doInBackground(Void... params) {
                // XXX: This method is not part of the public API
                File directory = IOUtil.getDiskCacheDir(MainActivity.this, "images");
                clearAllFiles(directory);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                ImageLoaderApplication.getLoader(MainActivity.this)
                        .getMemoryCache().evictAll();

                findViewById(R.id.clear_cache).setEnabled(true);
            }

            private void clearAllFiles(File folder) {
                for (File child : folder.listFiles()) {
                    if (child.isDirectory()) {
                        clearAllFiles(child);
                    } else {
                        child.delete();
                    }
                }
            }
        }.execute();
    }
}

package com.webimageloader.sample.patterns;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.webimageloader.ImageLoader;

public class MainActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageLoader.Logger.logAll();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, new PatternsListFragment())
                    .commit();
        }
    }
}

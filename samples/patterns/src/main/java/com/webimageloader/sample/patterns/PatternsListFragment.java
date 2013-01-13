package com.webimageloader.sample.patterns;

import android.os.Bundle;
import android.support.v4.app.ListFragment;

public class PatternsListFragment extends ListFragment {
    private static final String URL = "http://www.colourlovers.com/api/patterns/top?numResults=100&format=json";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListShown(true);
        new AbsListLoader(this, getListView(), URL);
    }
}

package com.webimageloader.sample.patterns;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

public class PatternsGridFragment extends Fragment {
    private static final String URL = "http://www.colourlovers.com/api/patterns/new?numResults=100&format=json";

    private GridView gridView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        gridView = new GridView(getActivity());
        gridView.setNumColumns(2);

        return gridView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        new AbsListLoader(this, gridView, URL);
    }
}

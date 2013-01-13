package com.webimageloader.sample.patterns;

import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
import com.webimageloader.ImageLoader;

public class MainActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageLoader.Logger.logAll();

        ViewPager viewPager = new ViewPager(this);
        viewPager.setId(R.id.view_pager);
        setContentView(viewPager);

        viewPager.setAdapter(new Adapter(getSupportFragmentManager()));
    }

    private class Adapter extends FragmentStatePagerAdapter {
        public Adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new PatternsGridFragment();
                case 1:
                    return new PatternsListFragment();
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}

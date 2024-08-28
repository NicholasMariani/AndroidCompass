package com.admin.androidcompass.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.admin.androidcompass.DetailsActivity;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {


    private int numOfTabs;
    private DetailsActivity context;

    public SectionsPagerAdapter(@NonNull FragmentManager fm, int numOfTabs, DetailsActivity context) {
        super(fm);
        this.numOfTabs = numOfTabs;
        this.context = context;

    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                return new LocationFragment(context);
            case 1:
                return new SensorsFragment(context);
            case 2:
                return new CalibrationFragment(context);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return numOfTabs;
    }
}
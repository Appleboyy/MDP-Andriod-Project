package com.example.MDP_Android.ui.main;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.MDP_Android.R;

// Adapter to control CommFragment, MapTabFragment & ControlFragment
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    //    Initialise variables
    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.tab_text_2, R.string.tab_text_3, R.string.tab_text_4};
    private final Context mContext;

    //    Setup page variables
    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    //    Get the fragment to display in positioning
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return CommFragment.newInstance(position + 1);
            case 1:
                return MapTabFragment.newInstance(position + 1);
            case 2:
                return ControlFragment.newInstance(position + 1);
            default:
                return PlaceholderFragment.newInstance(position + 1);
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        return 3;
    }
}
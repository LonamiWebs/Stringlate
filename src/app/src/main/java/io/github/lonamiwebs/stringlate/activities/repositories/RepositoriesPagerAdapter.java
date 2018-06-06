package io.github.lonamiwebs.stringlate.activities.repositories;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.info.MoreInfoFragment;

class RepositoriesPagerAdapter extends FragmentPagerAdapter {

    //region Members

    private final Context mContext;

    //endregion

    //region Initialization

    public RepositoriesPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }

    //endregion

    //region Overrides

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new HistoryFragment();
            case 1:
                return new AddNewRepositoryFragment();
            case 2:
                return new MoreInfoFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.getString(R.string.history);
            case 1:
                return mContext.getString(R.string.add_project);
            case 2:
                return mContext.getString(R.string.more);
        }
        return null;
    }

    //endregion
}

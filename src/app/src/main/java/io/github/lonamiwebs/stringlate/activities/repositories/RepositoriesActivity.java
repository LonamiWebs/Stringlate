package io.github.lonamiwebs.stringlate.activities.repositories;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import io.github.lonamiwebs.stringlate.R;

import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_REPO_DISCOVERED;

public class RepositoriesActivity extends AppCompatActivity {

    //region Members

    private RepositoriesPagerAdapter mRepositoriesPagerAdapter;
    private ViewPager mViewPager;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repositories);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRepositoriesPagerAdapter = new RepositoriesPagerAdapter(getSupportFragmentManager(), this);
        mViewPager = (ViewPager)findViewById(R.id.container);
        mViewPager.setAdapter(mRepositoriesPagerAdapter);

        TabLayout tabLayout = (TabLayout)findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    //endregion

    //region Activity results

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_REPO_DISCOVERED:
                    // Position #1 is the "Add new repository" fragment
                    mViewPager.setCurrentItem(1, true);
                    // Let the child fragment now this activity result occurred
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        }
    }

    //endregion
}

package io.github.lonamiwebs.stringlate.activities.repositories;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import net.gsantner.opoc.util.ContextUtils;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.BrowserActivity;
import io.github.lonamiwebs.stringlate.activities.SettingsActivity;
import io.github.lonamiwebs.stringlate.activities.translate.TranslateActivity;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.RepoHandlerHelper;
import io.github.lonamiwebs.stringlate.utilities.StringlateApi;

import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_REPO_DISCOVERED;

public class RepositoriesActivity extends AppCompatActivity {

    //region Members

    private RepositoriesPagerAdapter mRepositoriesPagerAdapter;
    private BottomNavigationView mBottomNavigationView;
    private ViewPager mViewPager;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new ContextUtils(this).setAppLanguage(new AppSettings(this).getLanguage());
        setContentView(R.layout.activity_repositories);

        mRepositoriesPagerAdapter = new RepositoriesPagerAdapter(getSupportFragmentManager(), this);
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mRepositoriesPagerAdapter);
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);

        mBottomNavigationView = findViewById(R.id.navigation);
        mBottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Add fab action
        FloatingActionButton fab = findViewById(R.id.fab_add_project);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(1, true);
            }
        });

        // Check if we opened the application because a GitHub link was clicked
        // If this is the case then we should show the "Add repository" fragment
        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_VIEW)) {
            // Opened via a GitHub.com url
            mViewPager.setCurrentItem(1, false);
        } else if (action.equals(StringlateApi.ACTION_TRANSLATE)) {

            // Opened via our custom StringlateApi, ensure we have the required extras
            if (intent.hasExtra(StringlateApi.EXTRA_GIT_URL)) {
                final String gitUrl = intent.getStringExtra(StringlateApi.EXTRA_GIT_URL);
                RepoHandler repo = RepoHandlerHelper.fromContext(this, gitUrl);
                if (repo.isEmpty()) {
                    // This repository is empty, clean any created
                    // garbage and show the "Add repository" fragment
                    repo.delete();
                    mViewPager.setCurrentItem(1, false);
                } else {
                    // We already had this repository so directly
                    // show the "Translate" activity and finish this
                    TranslateActivity.launch(this, repo);
                    finish();
                }
            } else {
                // No extra was given, finish taking no further action
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Always notify data set changed to refresh the repository list
        mRepositoriesPagerAdapter.notifyDataSetChanged();

        if (SettingsActivity.activityRetVal == SettingsActivity.RESULT.CHANGE_RESTART) {
            SettingsActivity.activityRetVal = SettingsActivity.RESULT.NOCHANGE;
            recreate();
        }
    }

    //endregion

    //region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_repositories, menu);
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Online help
            case R.id.action_help:
                // Avoid the "Remove unused resources" from removing these files…
                if (R.raw.en != 0 && R.raw.es != 0) {
                    Intent intent = new Intent(this, BrowserActivity.class);
                    intent.putExtra(BrowserActivity.EXTRA_DO_SHOW_STRINGLATE_HELP, true);
                    startActivity(intent);
                }
                return true;
            // Login to GitHub
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //endregion

    //region Navigation

    public void goToHistory() {
        mViewPager.setCurrentItem(0, true);
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_history:
                    mViewPager.setCurrentItem(0, true);
                    return true;
                case R.id.navigation_add_repository:
                    mViewPager.setCurrentItem(1, true);
                    return true;
            }
            return false;
        }

    };

    private final ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mBottomNavigationView.getMenu().getItem(position).setChecked(true);

            // Animate add project fab
            FloatingActionButton fab = findViewById(R.id.fab_add_project);
            fab.animate().scaleX(position == 0 ? 1.0f : 0.0f).scaleY((position == 0 ? 1.0f : 0.0f));
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    //endregion

    //region Activity results

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_REPO_DISCOVERED:
                    // Position #1 is the "Add new repository" fragment
                    mViewPager.setCurrentItem(1, true);
                    // Let the child fragment know this activity result occurred
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        }
    }

    //endregion
}

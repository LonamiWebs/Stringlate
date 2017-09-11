package io.github.lonamiwebs.stringlate.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.Messenger;
import io.github.lonamiwebs.stringlate.adapters.ApplicationAdapter;
import io.github.lonamiwebs.stringlate.classes.applications.ApplicationsSyncTask;
import io.github.lonamiwebs.stringlate.classes.lazyloader.FileCache;
import io.github.lonamiwebs.stringlate.classes.lazyloader.ImageLoader;
import io.github.lonamiwebs.stringlate.settings.AppSettings;

public class DiscoverActivity extends AppCompatActivity {

    //region Members

    private AppSettings mSettings;

    private LinearLayout mSyncingLayout;
    private ProgressBar mSyncingProgressBar;
    private TextView mNoRepositoryTextView;
    private RecyclerView mApplicationListView;
    private ApplicationAdapter mApplicationAdapter;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        mSettings = new AppSettings(this);

        mSyncingLayout = findViewById(R.id.syncingLayout);
        mSyncingProgressBar = findViewById(R.id.syncingProgressBar);
        mNoRepositoryTextView = findViewById(R.id.noRepositoryTextView);
        mApplicationListView = findViewById(R.id.applicationListView);

        mApplicationAdapter = new ApplicationAdapter(this, mSettings.isDownloadIconsAllowed());
        mApplicationListView.setAdapter(mApplicationAdapter);
        mApplicationListView.setLayoutManager(new LinearLayoutManager(this));

        mApplicationAdapter.onItemClick = new ApplicationAdapter.OnItemClick() {
            @Override
            public void onClick(final Intent data) {
                setResult(RESULT_OK, data);
                finish();
            }
        };

        mApplicationListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy <= 0) {
                    final LinearLayoutManager manager = (LinearLayoutManager)
                            mApplicationListView.getLayoutManager();

                    if (manager.findFirstVisibleItemPosition() + manager.getChildCount() ==
                            manager.getItemCount()) {
                        mApplicationAdapter.loadMore();
                    }
                }
            }
        });

        mNoRepositoryTextView.setText(getString(
                R.string.apps_repo_not_downloaded, getString(R.string.update_applications)));
        checkViewsVisibility(mApplicationAdapter.getItemCount() != 0);

        Messenger.onApplicationsSync.add(applicationsSync);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Messenger.onApplicationsSync.remove(applicationsSync);
    }

    //endregion

    //region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_discover, menu);

        menu.findItem(R.id.allowDownloadIcons).setChecked(mSettings.isDownloadIconsAllowed());

        // Associate the searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String query) {
                refreshListView(query);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Synchronizing repository
            case R.id.updateApplications:
                mApplicationAdapter.beginSyncApplications();
                return true;
            // Toggle downloading icons ability
            case R.id.allowDownloadIcons:
                boolean allow = !item.isChecked();
                item.setChecked(allow);
                mSettings.setDownloadIconsAllowed(allow);
                mApplicationAdapter.setAllowInternetDownload(allow);
                return true;
            // Clearing the icons cache
            case R.id.clearIconsCache:
                String cleared = FileCache.getHumanReadableSize(
                        this, new ImageLoader(this, false).clearCache());

                Toast.makeText(this,
                        getString(R.string.icon_cache_cleared, cleared), Toast.LENGTH_SHORT).show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //endregion

    //region ApplicationDetails list view

    //region Update index

    private final Messenger.OnApplicationsSync applicationsSync = new Messenger.OnApplicationsSync() {
        @Override
        public void onUpdate(float progress) {
            checkViewsVisibility(false);
            mSyncingProgressBar.setProgress((int) (progress * 100f));
        }

        @Override
        public void onFinish(boolean okay) {
            checkViewsVisibility(okay);
            if (okay)
                refreshListView("");
        }
    };

    // Updates the visibility of the views depending on the current state
    void checkViewsVisibility(boolean appsLoaded) {
        final boolean syncing = ApplicationsSyncTask.isSyncing();
        if (syncing) {
            mSyncingLayout.setVisibility(View.VISIBLE);
            mSyncingProgressBar.setProgress((int) (100 * ApplicationsSyncTask.progress));
        } else {
            mSyncingLayout.setVisibility(View.GONE);
        }

        mNoRepositoryTextView.setVisibility(syncing || appsLoaded ? View.GONE : View.VISIBLE);
    }

    //endregion

    //region Update list view

    private void refreshListView(@NonNull String filter) {
        mApplicationAdapter.setNewFilter(filter);
        checkViewsVisibility(mApplicationAdapter.getItemCount() != 0);
    }

    //endregion

    //endregion
}

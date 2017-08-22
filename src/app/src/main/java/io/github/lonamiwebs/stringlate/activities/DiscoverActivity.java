package io.github.lonamiwebs.stringlate.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.applications.ApplicationAdapter;
import io.github.lonamiwebs.stringlate.classes.applications.ApplicationDetails;
import io.github.lonamiwebs.stringlate.classes.applications.ApplicationList;
import io.github.lonamiwebs.stringlate.classes.lazyloader.FileCache;
import io.github.lonamiwebs.stringlate.classes.lazyloader.ImageLoader;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.settings.AppSettings;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.DEFAULT_APPS_LIMIT;

public class DiscoverActivity extends AppCompatActivity {

    //region Members

    private AppSettings mSettings;

    private TextView mNoRepositoryTextView;
    private ListView mApplicationListView;

    private ApplicationList mApplicationList;

    // We don't want to infinitely call "load more" if there are no more applications loaded
    private boolean anyAppLeft;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        mSettings = new AppSettings(this);

        mNoRepositoryTextView = findViewById(R.id.noRepositoryTextView);
        mApplicationListView = findViewById(R.id.applicationListView);

        mApplicationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ApplicationDetails app = (ApplicationDetails) mApplicationListView.getItemAtPosition(i);
                Intent data = new Intent();
                data.putExtra("url", app.getSourceCodeUrl());
                data.putExtra("web", app.getWebUrl());
                data.putExtra("name", app.getName());
                setResult(RESULT_OK, data);
                finish();
            }
        });

        mApplicationListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (totalItemCount > 0 && firstVisibleItem + visibleItemCount == totalItemCount)
                    loadMore();
            }
        });

        mApplicationList = new ApplicationList(this);
        if (mApplicationList.loadIndexXml()) {
            refreshListView("");
        } else {
            mNoRepositoryTextView.setText(getString(
                    R.string.apps_repo_not_downloaded, getString(R.string.update_applications)));
            mNoRepositoryTextView.setVisibility(VISIBLE);
        }
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
                updateApplicationsIndex();
                return true;
            // Toggle downloading icons ability
            case R.id.allowDownloadIcons:
                boolean allow = !item.isChecked();
                item.setChecked(allow);
                mSettings.setDownloadIconsAllowed(allow);
                refreshListView("");
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

    private void updateApplicationsIndex() {
        // TODO We need to avoid calling this twice.
        // If the async task already exists, don't create another.
        // For some reason rotating the screen will *not* dismiss the async task, but attempting
        // to report any progress to the main UI activity will fail to do so.
        new AsyncTask<Void, String, Boolean>() {
            @Override
            protected void onPreExecute() {
                mNoRepositoryTextView.setText("");
                mNoRepositoryTextView.setVisibility(VISIBLE);
                mApplicationListView.setVisibility(GONE);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                return mApplicationList.syncRepo(new ProgressUpdateCallback() {
                    @Override
                    public void onProgressUpdate(String title, String description) {
                        publishProgress(title, description);
                    }

                    @Override
                    public void onProgressFinished(String description, boolean status) {
                        // TODO I probably could rework everything to get rid of this method,
                        // since now I create the AsyncTask OUTSIDE the syncing methods (as it
                        // should have always been done).
                    }
                });
            }

            @Override
            protected void onProgressUpdate(String... values) {
                // Description\n\nText
                mNoRepositoryTextView.setText(values[0] + "\n\n" + values[1]);
            }

            @Override
            protected void onPostExecute(Boolean okay) {
                if (!okay) {
                    // Set back the "repo not synced" text
                    mNoRepositoryTextView.setText(getString(
                            R.string.apps_repo_not_downloaded, getString(R.string.update_applications)));
                }

                refreshListView("");
            }
        }.execute();
    }

    //endregion

    //region Update list view

    private void refreshListView(@NonNull String filter) {
        // Keep the reference of the new internal slice array list
        ArrayList<ApplicationDetails> appsSlice = mApplicationList.newSlice(filter);

        // Initial bulk load, this will determine whether there are more apps left or not
        anyAppLeft = mApplicationList.increaseSlice(DEFAULT_APPS_LIMIT);

        if (appsSlice.isEmpty()) {
            mNoRepositoryTextView.setVisibility(VISIBLE);
            mApplicationListView.setVisibility(GONE);
            mApplicationListView.setAdapter(null);
        } else {
            mNoRepositoryTextView.setVisibility(GONE);
            mApplicationListView.setVisibility(VISIBLE);
            mApplicationListView.setAdapter(new ApplicationAdapter(
                    this, appsSlice, mSettings.isDownloadIconsAllowed()));
        }
    }

    private void loadMore() {
        if (!anyAppLeft)
            return;

        // Increase the slice size and notify the changes
        anyAppLeft &= mApplicationList.increaseSlice(DEFAULT_APPS_LIMIT);
        ((ApplicationAdapter) mApplicationListView.getAdapter()).notifyDataSetChanged();
    }

    //endregion

    //endregion
}

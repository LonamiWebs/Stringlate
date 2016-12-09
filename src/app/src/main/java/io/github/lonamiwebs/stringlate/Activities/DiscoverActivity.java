package io.github.lonamiwebs.stringlate.Activities;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import io.github.lonamiwebs.stringlate.Applications.Application;
import io.github.lonamiwebs.stringlate.Applications.ApplicationAdapter;
import io.github.lonamiwebs.stringlate.Applications.ApplicationList;
import io.github.lonamiwebs.stringlate.Interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.LazyImageLoader.ImageLoader;
import io.github.lonamiwebs.stringlate.R;

public class DiscoverActivity extends AppCompatActivity {

    //region Members

    private ListView mApplicationListView;

    private ApplicationList mApplicationList;

    private boolean mApplyAppsLimit; // Limit applications to show on the ListView?

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        mApplicationListView = (ListView)findViewById(R.id.applicationListView);
        mApplicationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Application app = (Application)mApplicationListView.getItemAtPosition(i);
                Intent data = new Intent();
                data.putExtra("url", app.getSourceCodeUrl());
                setResult(RESULT_OK, data);
                finish();
            }
        });

        mApplicationList = new ApplicationList(this);
        if (!mApplicationList.loadIndexXml()) {
            Toast.makeText(this, R.string.load_apps_repo_failed, Toast.LENGTH_LONG).show();
        }
    }

    //endregion

    //region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.discover_menu, menu);

        MenuItem applyLimitItem = menu.findItem(R.id.applyListLimit);
        mApplyAppsLimit = applyLimitItem.isChecked();
        applyLimitItem.setTitle(getString(
                R.string.show_only_x, ApplicationList.DEFAULT_APPS_LIMIT));

        // Associate the searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView)menu.findItem(R.id.search).getActionView();
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

        // We need to let the menu initialize before we can refresh the ListView
        refreshListView();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Synchronizing repository
            case R.id.updateApplications:
                updateApplicationsIndex();
                return true;
            // Limiting the count of applications shown
            case R.id.applyListLimit:
                toggleAppListLimit(item);
                return true;
            // Clearing the icons cache
            case R.id.clearIconsCache:
                new ImageLoader(this).clearCache();
                Toast.makeText(this, R.string.icon_cache_cleared, Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void toggleAppListLimit(MenuItem item) {
        mApplyAppsLimit = !mApplyAppsLimit;
        item.setChecked(mApplyAppsLimit);
        refreshListView();
    }

    //endregion

    void updateApplicationsIndex() {
        // There must be a non-empty title if we want it to be set later
        final ProgressDialog progress = ProgressDialog.show(this, "…", "", true);

        mApplicationList.syncRepo(new ProgressUpdateCallback() {
            @Override
            public void onProgressUpdate(String title, String description) {
                progress.setTitle(title);
                progress.setMessage(description);
            }

            @Override
            public void onProgressFinished(String description, boolean status) {
                progress.dismiss();
                if (status)
                    refreshListView();
                else
                    Toast.makeText(getApplicationContext(),
                            R.string.sync_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    void refreshListView() {
        refreshListView(null);
    }

    void refreshListView(String filter) {
        mApplicationListView.setAdapter(new ApplicationAdapter(
                this, R.layout.item_application_list,
                mApplicationList.getApplications(mApplyAppsLimit, filter)));
    }
}

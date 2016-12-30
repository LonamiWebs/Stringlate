package io.github.lonamiwebs.stringlate.activities;

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
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.applications.Application;
import io.github.lonamiwebs.stringlate.classes.applications.ApplicationAdapter;
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
    private Button mShowMoreButton;

    private ApplicationList mApplicationList;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        mSettings = new AppSettings(this);

        mNoRepositoryTextView = (TextView)findViewById(R.id.noRepositoryTextView);
        mApplicationListView = (ListView)findViewById(R.id.applicationListView);

        mShowMoreButton = new Button(this);
        mShowMoreButton.setText(getString(R.string.show_more));
        mShowMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadMore();
            }
        });
        mApplicationListView.addFooterView(mShowMoreButton);

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
        if (mApplicationList.loadIndexXml()) {
            refreshListView(null);
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
                refreshListView(null);
                return true;
            // Clearing the icons cache
            case R.id.clearIconsCache:
                String cleared = FileCache.getHumanReadableSize(
                        new ImageLoader(this, false).clearCache());

                Toast.makeText(this,
                        getString(R.string.icon_cache_cleared, cleared), Toast.LENGTH_SHORT).show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //endregion

    //region Application list view

    //region Update index

    void updateApplicationsIndex() {
        // There must be a non-empty title if we want it to be set later
        final ProgressDialog progress = ProgressDialog.show(this, "…", "…", true);

        mApplicationList.syncRepo(new ProgressUpdateCallback() {
            @Override
            public void onProgressUpdate(String title, String description) {
                progress.setTitle(title);
                progress.setMessage(description);
            }

            @Override
            public void onProgressFinished(String description, boolean status) {
                progress.dismiss();
                if (status) {
                    refreshListView(null);
                }
                else
                    Toast.makeText(getApplicationContext(),
                            R.string.sync_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //endregion

    //region Update list view

    void refreshListView(String filter) {
        // Keep the reference of the new internal slice array list
        ArrayList<Application> appsSlice = mApplicationList.newSlice(filter);

        // Initial bulk load (the whole list might also have
        // been consumed, so also update the "Show more" visibility)
        updateShowMoreVisibility(mApplicationList.increaseSlice(DEFAULT_APPS_LIMIT));

        if (appsSlice.isEmpty()) {
            mNoRepositoryTextView.setVisibility(VISIBLE);
            mApplicationListView.setVisibility(GONE);
            mApplicationListView.setAdapter(null);
        } else {
            mNoRepositoryTextView.setVisibility(GONE);
            mApplicationListView.setVisibility(VISIBLE);
            mApplicationListView.setAdapter(new ApplicationAdapter(
                    this, R.layout.item_application_list, appsSlice,
                    mSettings.isDownloadIconsAllowed()));
        }
    }

    void loadMore() {
        // We first need to cast to HeaderViewListAdapter since we're using a footer view
        ApplicationAdapter adapter = (ApplicationAdapter)
                ((HeaderViewListAdapter)mApplicationListView.getAdapter()).getWrappedAdapter();

        // Increase the slice size and notify the changes
        updateShowMoreVisibility(mApplicationList.increaseSlice(DEFAULT_APPS_LIMIT));
        adapter.notifyDataSetChanged();
    }

    void updateShowMoreVisibility(boolean anyAppLeft) {
        mShowMoreButton.setVisibility(anyAppLeft ? VISIBLE : GONE);
    }

    //endregion

    //endregion
}

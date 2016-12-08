package io.github.lonamiwebs.stringlate.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import io.github.lonamiwebs.stringlate.R;

public class DiscoverActivity extends AppCompatActivity {

    //region Members

    private ListView mApplicationListView;

    private ApplicationList mApplicationList;

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
        if (mApplicationList.loadIndexXml()) {
            refreshListView();
        } else {
            Toast.makeText(this, "Could not load the repository, please sync and try again.", Toast.LENGTH_LONG).show();
        }
    }

    //endregion

    //region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.discover_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Synchronizing repository
            case R.id.updateApplications:
                updateApplicationsIndex();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //endregion

    void updateApplicationsIndex() {
        final ProgressDialog progress = ProgressDialog.show(this,
                "doin' stuff",
                "realstuff :d", true);

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
                    Toast.makeText(getApplicationContext(), "Failed to synchronize.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    void refreshListView() {
        mApplicationListView.setAdapter(new ApplicationAdapter(
                this, R.layout.item_application_list, mApplicationList.getApplications()));
    }
}

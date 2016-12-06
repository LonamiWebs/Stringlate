package io.github.lonamiwebs.stringlate.Activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import io.github.lonamiwebs.stringlate.R;

public class DiscoverActivity extends AppCompatActivity {

    private ListView mPackageListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        mPackageListView = (ListView)findViewById(R.id.packageListView);
    }
}

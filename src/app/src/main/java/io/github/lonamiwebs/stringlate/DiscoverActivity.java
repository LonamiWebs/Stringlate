package io.github.lonamiwebs.stringlate;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class DiscoverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);
    }

    public void onExitClick(View v) {
        finish();
    }
}

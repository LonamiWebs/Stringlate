package io.github.lonamiwebs.stringlate.Activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.lonamiwebs.stringlate.R;

public class DiscoverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        SimpleAdapter adapter = new SimpleAdapter(this,
                data,
                R.layout.item_application_list,
                new String[] { "Icon", "Name", "Description" },
                new int[] { R.id.appIcon, R.id.appName, R.id.appDescription });

        ((ListView)findViewById(R.id.packageListView)).setAdapter(adapter);
    }

    final static ArrayList<HashMap<String, ?>> data = new ArrayList<HashMap<String, ?>>();

    static {
        HashMap<String, Object> row  = new HashMap<String, Object>();
        row.put("Icon", R.drawable.app_not_found);
        row.put("Name", "Stringlate");
        row.put("Description", "Official version");
        data.add(row);
        row  = new HashMap<String, Object>();
        row.put("Icon", R.drawable.app_not_found);
        row.put("Name", "Stringlate");
        row.put("Description", "Absolutely not hacked version");
        data.add(row);
        row  = new HashMap<String, Object>();
        row.put("Icon", R.drawable.app_not_found);
        row.put("Name", "Cadenaductor");
        row.put("Description", "Stringlate versión española");
        data.add(row);
    }
}

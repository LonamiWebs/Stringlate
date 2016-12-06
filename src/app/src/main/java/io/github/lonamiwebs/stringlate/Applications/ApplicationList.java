package io.github.lonamiwebs.stringlate.Applications;

import android.content.Context;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.lonamiwebs.stringlate.R;

public class ApplicationList {

    //region Members

    private final ArrayList<Application> mApplications;

    //endregion

    //region Initialization

    public ApplicationList() {
        mApplications = new ArrayList<>();
    }

    //endregion

    //region Getters

    SimpleAdapter getListAdapter(Context context) {
        ArrayList<HashMap<String, ?>> data = new ArrayList<>();
        for (Application app : mApplications)
            data.add(app.toHashMap());

        return new SimpleAdapter(context,
                data,
                R.layout.item_application_list,
                new String[] { Application.ICON, Application.NAME, Application.DESCRIPTION },
                new int[] { R.id.appIcon, R.id.appName, R.id.appDescription });
    }

    //endregion
}

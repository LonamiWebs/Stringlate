package io.github.lonamiwebs.stringlate.Applications;

import android.content.Context;
import android.widget.SimpleAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.Utilities.FileDownloader;

public class ApplicationList {

    //region Members

    private final static String FDROID_REPO_URL = "https://f-droid.org/repo/index.jar";

    private File mRoot;
    private Context mContext;

    private static final String BASE_DIR = "index";

    private final ArrayList<Application> mApplications;

    //endregion

    //region Initialization

    public ApplicationList(Context context) {
        mApplications = new ArrayList<>();
        mContext = context;

        mRoot = new File(mContext.getFilesDir(), BASE_DIR);
    }

    //endregion

    //region Getters

    SimpleAdapter getListAdapter() {
        ArrayList<HashMap<String, ?>> data = new ArrayList<>();
        for (Application app : mApplications)
            data.add(app.toHashMap());

        return new SimpleAdapter(mContext,
                data,
                R.layout.item_application_list,
                new String[] { Application.ICON, Application.NAME, Application.DESCRIPTION },
                new int[] { R.id.appIcon, R.id.appName, R.id.appDescription });
    }

    //endregion

    public void syncRepo() {
        downloadIndex();
    }

    void downloadIndex() {
        FileDownloader.downloadFile(FDROID_REPO_URL, getIndexFile());
    }

    private File getIndexFile() {
        return new File(mRoot, "index.jar");
    }
}

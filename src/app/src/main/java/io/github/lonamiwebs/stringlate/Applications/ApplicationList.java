package io.github.lonamiwebs.stringlate.Applications;

import android.content.Context;
import android.os.AsyncTask;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import io.github.lonamiwebs.stringlate.Interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.Utilities.FileDownloader;
import io.github.lonamiwebs.stringlate.Utilities.FileExtractor;

public class ApplicationList implements Iterable<Application> {

    //region Members

    private final static String FDROID_REPO_URL = "https://f-droid.org/repo";
    private final static String FDROID_INDEX_URL = FDROID_REPO_URL+"/index.jar";

    private File mRoot;
    private Context mContext;

    private String mIconBaseUrl;

    private static final String BASE_DIR = "index";

    private ArrayList<Application> mApplications;

    //endregion

    //region Initialization

    public ApplicationList(Context context) {
        mApplications = new ArrayList<>();
        mContext = context;

        mRoot = new File(mContext.getFilesDir(), BASE_DIR);
        mIconBaseUrl = FDROID_REPO_URL+getIconDirectory();
    }

    //endregion

    //region Getters

    public ArrayList<Application> getApplications() {
        return mApplications;
    }

    //endregion

    public void syncRepo(final ProgressUpdateCallback callback) {
        final AsyncTask<Void, Void, Void> step3 = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                callback.onProgressUpdate("Loading and minimizing strings.xml file...", "Wait plz.");
            }

            @Override
            protected Void doInBackground(Void... voids) {
                loadIndexXml(); // Loses not-required information
                saveIndexXml(); // Thus minimizes the file
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                callback.onProgressFinished("Done", true);
            }
        };
        final AsyncTask<Void, Void, Void> step2 = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                callback.onProgressUpdate("Extracting index.xml from index.jar...", "Wait plz.");
            }

            @Override
            protected Void doInBackground(Void... voids) {
                extractIndexXml();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                step3.execute();
            }
        };
        final AsyncTask<Void, Void, Void> step1 = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                callback.onProgressUpdate("Downloading index.jar...", "Wait plz.");
            }

            @Override
            protected Void doInBackground(Void... voids) {
                downloadIndexJar();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                step2.execute();
            }
        };

        step1.execute();
    }

    // Step 1: Download the index.jar
    private void downloadIndexJar() {
        FileDownloader.downloadFile(FDROID_INDEX_URL, getIndexFile("jar"));
    }

    // Step 2: Extract the index.jar
    private void extractIndexXml() {
        FileExtractor.unpackZip(getIndexFile("jar"), mRoot, false);
    }

    // Step 3a: Load the ApplicationList from the index.xml
    public boolean loadIndexXml() {
        try {
            File file = getIndexFile("xml");
            if (file.isFile()) {
                mApplications = ApplicationListParser
                        .parseFromXml(new FileInputStream(getIndexFile("xml")));
                return true;
            } else {
                mApplications.clear();
            }
        } catch (IOException | XmlPullParserException e) {
            // Won't happen
            e.printStackTrace();
        }
        return false;
    }

    // Step 3b: Save a (minimized) version of the index.xml
    private void saveIndexXml() {
        try {
            ApplicationListParser.parseToXml(this,
                    new FileOutputStream(getIndexFile("xml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getIndexFile(String extension) {
        return new File(mRoot, "index."+extension);
    }

    public static final String FALLBACK_ICONS_DIR = "/icons/";

    private String getIconDirectory() {
        final double dpi = mContext.getResources().getDisplayMetrics().densityDpi;
        if (dpi >= 640)
            return "/icons-640/";

        if (dpi >= 480)
            return "/icons-480/";

        if (dpi >= 320)
            return "/icons-320/";

        if (dpi >= 240)
            return "/icons-240/";

        if (dpi >= 160)
            return "/icons-160/";

        if (dpi >= 120)
            return "/icons-120/";

        return FALLBACK_ICONS_DIR;
    }

    @Override
    public Iterator<Application> iterator() {
        return mApplications.iterator();
    }
}

package io.github.lonamiwebs.stringlate.classes.applications;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.utilities.FileDownloader;
import io.github.lonamiwebs.stringlate.utilities.FileExtractor;

public class ApplicationList implements Iterable<Application> {

    //region Members

    public final static String FDROID_REPO_URL = "https://f-droid.org/repo";
    private final static String FDROID_INDEX_URL = FDROID_REPO_URL+"/index.jar";

    private File mRoot;
    private Context mContext;

    private static final String BASE_DIR = "index";

    private ArrayList<Application> mApplications;
    private HashSet<String> mInstalledPackages;

    // Keep track of a filtered slice, so ListViews can have a "Show more"
    private ArrayList<Application> mApplicationsSlice;
    private String mSliceFilter;
    private int mLastSliceIndex;

    //endregion

    //region Initialization

    public ApplicationList(Context context) {
        mApplications = new ArrayList<>();
        mContext = context;

        mRoot = new File(mContext.getCacheDir(), BASE_DIR);

        mInstalledPackages = new HashSet<>();
        PackageManager pm = mContext.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            mInstalledPackages.add(packageInfo.packageName);
        }
    }

    //endregion

    //region Getters

    // Gets a new slice with the given filter for the application name
    public ArrayList<Application> newSlice(String filter) {
        mApplicationsSlice = new ArrayList<>();
        mSliceFilter = filter == null ? null : filter.trim().toLowerCase();
        mLastSliceIndex = 0;

        return mApplicationsSlice;
    }

    // Increases the previously retrieved slice by count,
    // returning true if this method can be called again
    public boolean increaseSlice(int count) {
        if (mLastSliceIndex >= mApplications.size())
            return false;

        int end;
        if (mSliceFilter == null) {
            end = mLastSliceIndex + count;
            if (end >= mApplications.size())
                end = mApplications.size();

            for (; mLastSliceIndex < end; mLastSliceIndex++) {
                mApplicationsSlice.add(mApplications.get(mLastSliceIndex));
            }
        } else {
            end = mApplications.size();

            for (; mLastSliceIndex < end && count > 0; mLastSliceIndex++) {
                Application app = mApplications.get(mLastSliceIndex);
                if (app.getName().toLowerCase().contains(mSliceFilter)) {
                    mApplicationsSlice.add(app);
                    count--;
                }
            }
        }

        // Return true if this method can be called again
        return mLastSliceIndex < mApplications.size();
    }

    //endregion

    public void syncRepo(final ProgressUpdateCallback callback) {
        final AsyncTask<Void, Void, Void> step3 = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                callback.onProgressUpdate(mContext.getString(R.string.loading_index_xml),
                        mContext.getString(R.string.loading_index_xml_long));
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
                callback.onProgressFinished(mContext.getString(R.string.done), true);
            }
        };
        final AsyncTask<Void, Void, Void> step2 = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                callback.onProgressUpdate(mContext.getString(R.string.extracting_index_xml),
                        mContext.getString(R.string.extracting_index_xml_long));
            }

            @Override
            protected Void doInBackground(Void... voids) {
                extractIndexXml();
                deleteIndexJar();
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
                callback.onProgressUpdate(mContext.getString(R.string.downloading_index_jar),
                        mContext.getString(R.string.downloading_index_jar_long));
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

    // Step 2a: Extract the index.xml from the index.jar
    private void extractIndexXml() {
        FileExtractor.unpackZip(getIndexFile("jar"), mRoot, false);
    }

    // Step 2b: Delete index.jar
    private boolean deleteIndexJar() {
        return getIndexFile("jar").delete();
    }

    // Step 3a: Load the ApplicationList from the index.xml
    public boolean loadIndexXml() {
        try {
            File file = getIndexFile("xml");
            if (file.isFile()) {
                mApplications = ApplicationListParser.parseFromXml(
                        new FileInputStream(getIndexFile("xml")),
                        mInstalledPackages);

                // Also sort the applications alphabetically, installed first
                Collections.sort(mApplications, new Comparator<Application>() {
                    @Override
                    public int compare(Application t1, Application t2) {
                        if (t1.isInstalled() == t2.isInstalled()) {
                            return t1.getName().compareTo(t2.getName());
                        } else {
                            return t1.isInstalled() ? -1 : 1;
                        }
                    }
                });
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

    @Override
    public Iterator<Application> iterator() {
        return mApplications.iterator();
    }
}

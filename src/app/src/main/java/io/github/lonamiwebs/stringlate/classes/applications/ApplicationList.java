package io.github.lonamiwebs.stringlate.classes.applications;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

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
import io.github.lonamiwebs.stringlate.utilities.Constants;
import io.github.lonamiwebs.stringlate.utilities.FileDownloader;
import io.github.lonamiwebs.stringlate.utilities.FileExtractor;

public class ApplicationList implements Iterable<ApplicationDetails> {

    //region Members
    private final File mRoot;
    private final Context mContext;

    private static final String BASE_DIR = "index";

    private ArrayList<ApplicationDetails> mApplications;
    private final HashSet<String> mInstalledPackages;

    // Keep track of a filtered slice, so ListViews can have a "Show more"
    private ArrayList<ApplicationDetails> mApplicationsSlice;
    @NonNull private String mSliceFilter;
    private int mLastSliceIndex;

    //endregion

    //region Initialization

    public ApplicationList(Context context) {
        mApplications = new ArrayList<>();
        mContext = context;
        mSliceFilter = "";

        mRoot = new File(mContext.getCacheDir(), BASE_DIR);

        mInstalledPackages = new HashSet<>();
        PackageManager pm = mContext.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            mInstalledPackages.add(packageInfo.packageName);
        }

        ApplicationListParser.loadFDroidIconPath(mContext);
    }

    //endregion

    //region Getters

    // Gets a new slice with the given filter for the application name
    public ArrayList<ApplicationDetails> newSlice(@NonNull String filter) {
        mApplicationsSlice = new ArrayList<>();
        mSliceFilter = filter.trim().toLowerCase();
        mLastSliceIndex = 0;

        return mApplicationsSlice;
    }

    // Increases the previously retrieved slice by count,
    // returning true if this method can be called again
    public boolean increaseSlice(int count) {
        if (mLastSliceIndex >= mApplications.size())
            return false;

        int end;
        if (mSliceFilter.isEmpty()) {
            end = mLastSliceIndex + count;
            if (end >= mApplications.size())
                end = mApplications.size();

            for (; mLastSliceIndex < end; mLastSliceIndex++) {
                mApplicationsSlice.add(mApplications.get(mLastSliceIndex));
            }
        } else {
            end = mApplications.size();

            for (; mLastSliceIndex < end && count > 0; mLastSliceIndex++) {
                ApplicationDetails app = mApplications.get(mLastSliceIndex);
                if (app.getName().toLowerCase().contains(mSliceFilter) ||
                        app.getDescription().toLowerCase().contains(mSliceFilter)) {
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
        FileDownloader.downloadFile(Constants.FDROID_INDEX_URL, getIndexFile("jar"));
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
                Collections.sort(mApplications, new Comparator<ApplicationDetails>() {
                    @Override
                    public int compare(ApplicationDetails t1, ApplicationDetails t2) {
                        if (t1.isInstalled() == t2.isInstalled()) {
                            return t1.getName().compareToIgnoreCase(t2.getName());
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
    public Iterator<ApplicationDetails> iterator() {
        return mApplications.iterator();
    }
}

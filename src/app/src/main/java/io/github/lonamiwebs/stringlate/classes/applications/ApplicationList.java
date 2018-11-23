package io.github.lonamiwebs.stringlate.classes.applications;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import net.gsantner.opoc.util.NetworkUtils;
import net.gsantner.opoc.util.ZipUtils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import io.github.lonamiwebs.stringlate.classes.Messenger;

public class ApplicationList implements Iterable<ApplicationDetails> {
    public final static String FDROID_REPO_URL = "https://f-droid.org/repo";
    public final static String FDROID_INDEX_URL = FDROID_REPO_URL + "/index.jar";
    public final static String FDROID_ICONS_DIR_FALLBACK = "/icons/";

    //region Members
    private final File mRoot;

    private static final String BASE_DIR = "index";

    private ArrayList<ApplicationDetails> mApplications;
    private final HashSet<String> mInstalledPackages;

    // Keep track of a filtered slice, so ListViews can have a "Show more"
    private ArrayList<ApplicationDetails> mApplicationsSlice;
    @NonNull
    private String mSliceFilter;
    private int mLastSliceIndex;

    //endregion

    //region Initialization

    public ApplicationList(Context context) {
        mApplications = new ArrayList<>();
        mSliceFilter = "";

        mRoot = new File(context.getCacheDir(), BASE_DIR);

        mInstalledPackages = new HashSet<>();
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            mInstalledPackages.add(packageInfo.packageName);
        }

        ApplicationListParser.loadFDroidIconPath(context);
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
                if (app.getProjectName().toLowerCase().contains(mSliceFilter) ||
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

    public boolean syncRepo(final Messenger.OnSyncProgress callback) {
        // Step 1: Download the index.jar
        callback.onUpdate(1, 0f);
        NetworkUtils.downloadFile(FDROID_INDEX_URL, getIndexFile("jar"), progress ->
                callback.onUpdate(1, progress));

        // Step 2: Extract the index.xml from the index.jar, then delete the index.jar
        callback.onUpdate(2, 0f);
        ZipUtils.unzip(getIndexFile("jar"), mRoot, true, progress ->
                callback.onUpdate(2, progress));
        if (!getIndexFile("jar").delete())
            return false;

        // Step 3, load the xml and lose non-required information, then save it minimized
        callback.onUpdate(3, 0f);
        if (!loadIndexXml())
            return false;

        try {
            // Save index.xml
            callback.onUpdate(4, 0f);
            ApplicationListParser.parseToXml(
                    this, new FileOutputStream(getIndexFile("xml"))
            );
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Done
        return true;
    }

    public boolean loadIndexXml() {
        try {
            File file = getIndexFile("xml");
            if (file.isFile()) {
                mApplications = ApplicationListParser.parseFromXml(
                        new FileInputStream(getIndexFile("xml")),
                        mInstalledPackages);

                // Also sort the applications alphabetically, installed first
                Collections.sort(mApplications, (t1, t2) -> {
                    if (t1.isInstalled() == t2.isInstalled()) {
                        return t1.getProjectName().compareToIgnoreCase(t2.getProjectName());
                    } else {
                        return t1.isInstalled() ? -1 : 1;
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

    private File getIndexFile(String extension) {
        return new File(mRoot, "index." + extension);
    }

    @Override
    public Iterator<ApplicationDetails> iterator() {
        return mApplications.iterator();
    }
}

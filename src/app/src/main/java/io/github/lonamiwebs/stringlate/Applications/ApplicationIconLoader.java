package io.github.lonamiwebs.stringlate.Applications;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.github.lonamiwebs.stringlate.Interfaces.Callback;
import io.github.lonamiwebs.stringlate.Utilities.FileDownloader;

import static io.github.lonamiwebs.stringlate.Applications.ApplicationList.FDROID_REPO_URL;

// TODO I don't really like this way. There must be a better way to download icons
public class ApplicationIconLoader {
    Lock mLock;
    Queue<Application> mApplications;
    Context mContext;

    Callback<Application> mCallback; // Icon downloaded callback

    private String mIconsBaseUrl;
    private File mIconsRoot;

    private AsyncTask<Void, Application, Void> mDownloadIconsTask;

    public ApplicationIconLoader(Context context, File root) {
        mApplications = new LinkedList<Application>();
        mLock = new ReentrantLock();
        mContext = context;

        mIconsRoot = new File(root, "icons");
        mIconsBaseUrl = FDROID_REPO_URL+getIconDirectory(context);
    }

    public void setOnIconDownloadedCallback(Callback<Application> callback) {
        mCallback = callback;
    }

    // Enqueues the given application to retrieve its icon
    public void enqueueDownloadIcon(Application app) {
        mLock.lock();
        if (!getIconFile(app).isFile() && !mApplications.contains(app)) {
            Log.i("LONAMIWEBS", "ADDED APP "+app.getPackageName());
            mApplications.add(app);
            if (mDownloadIconsTask == null) {
                mDownloadIconsTask = getDownloadIconsAsyncTask();
                mDownloadIconsTask.execute();
            }
        }
        mLock.unlock();
    }

    private AsyncTask<Void, Application, Void> getDownloadIconsAsyncTask() {
        return new AsyncTask<Void, Application, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Application app;
                boolean appsLeft = true;
                while (appsLeft) {
                    mLock.lock();
                    if (mApplications.isEmpty()) {
                        break;
                    } else {
                        app = mApplications.remove();
                        appsLeft = !mApplications.isEmpty();
                    }
                    mLock.unlock();
                    if (app.getIconName().isEmpty())
                        continue;

                    File iconFile = getIconFile(app);
                    String url = mIconsBaseUrl+app.getIconName();
                    if (FileDownloader.downloadFile(url, iconFile) && iconFile.isFile())
                        publishProgress(app);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Application... apps) {
                super.onProgressUpdate(apps);
                if (mCallback != null)
                    mCallback.onCallback(apps[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mDownloadIconsTask = null;
            }
        };
    }

    public File getIconFile(Application app) {
        return new File(mIconsRoot, app.getIconName());
    }

    private static final String FALLBACK_ICONS_DIR = "/icons/";
    private static String getIconDirectory(Context context) {
        final double dpi = context.getResources().getDisplayMetrics().densityDpi;
        if (dpi >= 640) return "/icons-640/";
        if (dpi >= 480) return "/icons-480/";
        if (dpi >= 320) return "/icons-320/";
        if (dpi >= 240) return "/icons-240/";
        if (dpi >= 160) return "/icons-160/";
        if (dpi >= 120) return "/icons-120/";

        return FALLBACK_ICONS_DIR;
    }
}

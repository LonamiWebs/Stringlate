package io.github.lonamiwebs.stringlate.classes.applications;

import android.os.Handler;

import java.util.concurrent.locks.ReentrantLock;

import io.github.lonamiwebs.stringlate.classes.Messenger;

// Similar to RepoSyncTask, but for applications
public class ApplicationsSyncTask extends Thread {

    private final ApplicationList mApplicationList;
    private final Handler mHandler;

    private final static ReentrantLock syncingLock = new ReentrantLock();

    // Starts the synchronization of the applications index, or
    // does nothing if another thread is already doing this work.
    public static void startSync(ApplicationList appList) {
        if (!syncingLock.tryLock())
            return;

        new ApplicationsSyncTask(appList).start();
    }

    public static boolean isSyncing() {
        return syncingLock.isLocked();
    }

    public static float progress;

    private ApplicationsSyncTask(final ApplicationList appList) {
        mApplicationList = appList;
        mHandler = new Handler();
    }

    @Override
    public void run() {
        final boolean okay = mApplicationList.syncRepo((stage, progress) ->
                mHandler.post(() ->
                        onProgressUpdate(stage, progress)));

        mHandler.post(() -> {
            syncingLock.unlock();
            Messenger.notifyApplicationSyncFinished(okay);
        });
    }

    private void onProgressUpdate(final int stage, float progress) {
        // Four stages, simply to a linear interpolation
        ApplicationsSyncTask.progress = (stage - 1f) / 3f + progress * 0.25f;
        Messenger.notifyApplicationSync(ApplicationsSyncTask.progress);
    }
}

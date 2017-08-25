package io.github.lonamiwebs.stringlate.git;

import android.content.Context;

import org.eclipse.jgit.lib.ProgressMonitor;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.Messenger;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;

public class GitCloneProgressCallback implements ProgressMonitor {

    private final Context mContext;
    private final Messenger.OnRepoSyncProgress mCallback;
    private int mDone, mWork;
    private boolean mStarted;

    private long mLastMs;

    private final static long DELAY_PER_UPDATE = 30; // 30ms

    private final static String RECEIVING_TITLE = "Receiving objects";
    private final static String RESOLVING_TITLE = "Resolving deltas";

    public GitCloneProgressCallback(final Context context, final Messenger.OnRepoSyncProgress callback) {
        mContext = context;
        mCallback = callback;
    }

    @Override
    final public void beginTask(String title, int totalWork) {
        if (title.equals(RECEIVING_TITLE) || title.equals(RESOLVING_TITLE)) {
            mDone = 0;
            mWork = totalWork;
            mStarted = true;
        } else {
            mStarted = false;
        }
    }

    @Override
    final public void update(int completed) {
        if (!mStarted)
            return;

        mDone += completed;

        // This method is called way so often, slow it down
        long time = System.currentTimeMillis();
        if (time - mLastMs >= DELAY_PER_UPDATE) {
            mLastMs = time;
            mCallback.onUpdate(1, (float)mDone / (float)mWork);
        }
    }

    @Override
    final public void start(int totalTasks) {
    }

    @Override
    final public void endTask() {
    }

    @Override
    final public boolean isCancelled() {
        return false;
    }
}

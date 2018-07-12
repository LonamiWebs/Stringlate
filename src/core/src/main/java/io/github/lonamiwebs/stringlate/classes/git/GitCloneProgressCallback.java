package io.github.lonamiwebs.stringlate.classes.git;


import org.eclipse.jgit.lib.ProgressMonitor;

import io.github.lonamiwebs.stringlate.classes.Messenger;

public class GitCloneProgressCallback implements ProgressMonitor {
    private final Messenger.OnSyncProgress mCallback;
    private int mDone, mWork;
    private boolean mStarted;
    private boolean mCancelled;

    private long mLastMs;

    private final static long DELAY_PER_UPDATE = 75;

    private final static String RECEIVING_TITLE = "Receiving objects";
    private final static String RESOLVING_TITLE = "Resolving deltas";

    public GitCloneProgressCallback(final Messenger.OnSyncProgress callback) {
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
            mCallback.onUpdate(1, (float) mDone / (float) mWork);
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
        return mCancelled;
    }

    public void cancel() {
        mCancelled = true;
    }
}

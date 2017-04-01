package io.github.lonamiwebs.stringlate.git;

import android.app.Activity;

import org.eclipse.jgit.lib.ProgressMonitor;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;

public abstract class GitCloneProgressCallback
        implements ProgressUpdateCallback, ProgressMonitor {

    private Activity mActivity;
    private int mDone, mWork;
    private boolean mStarted;

    private long mLastMs;

    private final static long DELAY_PER_UPDATE = 60; // 60ms

    private final static String RECEIVING_TITLE = "Receiving objects";
    private final static String RESOLVING_TITLE = "Resolving deltas";

    protected GitCloneProgressCallback(Activity activity) {
        mActivity = activity;
    }

    @Override
    final public void beginTask(String title, int totalWork) {
        if (title.equals(RECEIVING_TITLE) || title.equals(RESOLVING_TITLE)) {
            mDone = 0;
            mWork = totalWork;
            mStarted = true;
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
            updateProgress();
        }
    }

    @Override
    final public void start(int totalTasks) { }

    @Override
    final public void endTask() { }

    private void updateProgress() {
        final String title = mActivity.getString(R.string.cloning_repo);
        final String content = mActivity.getString(
                R.string.cloning_repo_progress, 100f * mDone / mWork);

        // TODO This probably can be improved. Some handler to post the result?
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onProgressUpdate(title, content);
            }
        });
    }

    @Override
    final public boolean isCancelled() {
        return false;
    }
}

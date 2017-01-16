package io.github.lonamiwebs.stringlate.git;

import android.app.Activity;

import org.eclipse.jgit.lib.ProgressMonitor;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;

public abstract class GitCloneProgressCallback
        implements ProgressUpdateCallback, ProgressMonitor {

    private Activity mActivity;
    private int mCurrentTask, mDone, mWork;

    private long mLastMs;

    private final static long DELAY_PER_UPDATE = 60; // 60ms

    protected GitCloneProgressCallback(Activity activity) {
        mActivity = activity;
    }

    @Override
    final public void beginTask(String title, int totalWork) {
        mCurrentTask++;
        mLastMs = 0;
        mDone = 0;
        mWork = totalWork;
    }

    @Override
    final public void update(int completed) {
        mDone += completed;

        // This method is called way so often, slow it down
        long time = System.currentTimeMillis();
        if (time - mLastMs >= DELAY_PER_UPDATE) {
            mLastMs = time;
            updateProgress();
        }
    }

    @Override
    final public void start(int totalTasks) { /* Total tasks is a liar */ }

    @Override
    final public void endTask() { }

    private void updateProgress() {
        final String title = mActivity.getString(R.string.cloning_repo);
        final String content = mActivity.getString(
                R.string.cloning_repo_progress, getProgress(mCurrentTask, mDone, mWork));

        // TODO This probably can be improved. Some handler to post the result?
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onProgressUpdate(title, content);
            }
        });
    }

    private static float getProgress(int task, float done, float work) {
        // After timing cloning a large repository (F-Droid client) twice and averaging:
        // Task 1 → 2: 2ms     (0.003%)
        // Task 2 → 3: 125ms   (0.204%)
        // Task 3 → 4: 28010ms (45.74%)
        // Task 4 → 5: 33100ms (54.05%)
        if (work > 0 && task < 5) {
            return getTotalTime(task) + (done / work) * getTime(task);
        } else {
            return getTotalTime(task);
        }
    }

    private static float getTime(int task) {
        switch (task) {
            case 1: return 0.003f;
            case 2: return 0.204f;
            case 3: return 45.74f;
            case 4: return 54.52f;
            default: return 0.00f;
        }
    }

    private static float getTotalTime(int untilTask) {
        switch (untilTask) {
            case 1: return 0.000f;
            case 2: return 0.003f;
            case 3: return 0.207f;
            case 4: return 45.95f;
            default: return 1.00f;
        }
    }

    @Override
    final public boolean isCancelled() {
        return false;
    }
}

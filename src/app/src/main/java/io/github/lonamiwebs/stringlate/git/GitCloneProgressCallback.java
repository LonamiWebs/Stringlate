package io.github.lonamiwebs.stringlate.git;

import android.app.Activity;

import org.eclipse.jgit.lib.ProgressMonitor;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;

public abstract class GitCloneProgressCallback
        implements ProgressUpdateCallback, ProgressMonitor {

    private Activity mActivity;
    private int mCurrentTask, mDone;
    private float mWork;

    protected GitCloneProgressCallback(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void start(int totalTasks) {
        mCurrentTask = 0;
    }

    @Override
    public void beginTask(String title, int totalWork) {
        mCurrentTask++;
        mDone = 0;
        mWork = totalWork;
    }

    @Override
    public void update(int completed) {
        mDone += completed;
        updateProgress();
    }

    @Override
    public void endTask() {
        mDone = (int)mWork;
        updateProgress();
    }

    private void updateProgress() {
        if (mWork > 0) {
            // We cannot know the task count before hand, so we only have
            // mCurrentTask and we assume that these are the total tasks.
            // The current task averages the most-1 or it would be 100%.
            //
            // Then, we simply multiply the current work done by how much
            // we would fill if that task were done. Say we have 3/4 tasks
            // done, then the work should fill that 25%.
            float perTask = 1.0f / mCurrentTask;
            float progress = 100.0f *
                    (((mCurrentTask - 1.0f) * perTask) +
                            ((mDone / mWork) * perTask));

            final String title = mActivity.getString(R.string.cloning_repo);
            final String content = mActivity.getString(
                    R.string.cloning_repo_progress, progress);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onProgressUpdate(title, content);
                }
            });
        }
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}

package io.github.lonamiwebs.stringlate.classes;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.interfaces.StringsSource;
import io.github.lonamiwebs.stringlate.utilities.RepoHandlerHelper;

public class RepoSyncTask extends Thread {

    private final Context mContext;
    private final RepoHandler mRepo;
    private final StringsSource mSource;
    private final Handler mHandler;
    private final boolean newRepo;

    // If addingNew is true, and the repository fails to sync, a notice will be shown,
    // and the repository settings will be deleted so that empty repositories don't show.
    public RepoSyncTask(final Context context, final RepoHandler repo,
                        final StringsSource source, final boolean addingNew) {
        mContext = context;
        mRepo = repo;
        mSource = source;
        newRepo = addingNew;
        mHandler = new Handler();
    }

    @Override
    public void run() {
        final boolean okay =
                RepoHandlerHelper.syncResources(mContext, mRepo, mSource, (stage, progress) ->
                        mHandler.post(() ->
                                onProgressUpdate(stage, progress)));

        mHandler.post(() -> {
            Messenger.notifyRepoSyncFinished(mRepo, okay);
            if (okay) {
                Messenger.notifyRepoAdded(mRepo);
            } else {
                if (!mRepo.wasCancelled()) {
                    Toast.makeText(
                            mContext,
                            mContext.getString(R.string.sync_failed, mRepo.getProjectName()),
                            Toast.LENGTH_SHORT
                    ).show();
                }

                // New repository, so it cannot have old resources- delete it since it failed
                if (newRepo)
                    mRepo.delete();
            }
        });
    }

    private void onProgressUpdate(final int stage, float progress) {
        progress = clamp(progress, 0f, 1f);

        // Give stage one 75% of the weight
        if (stage == 1)
            progress *= 0.75f;
        else
            progress = 0.75f + progress * 0.25f;

        Messenger.notifyRepoSync(mRepo, clamp(progress, 0f, 1f));
    }

    private static float clamp(float x, float min, float max) {
        return x < min ? min : (x > max ? max : x);
    }
}

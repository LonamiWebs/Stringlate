package io.github.lonamiwebs.stringlate.classes.repos;

import android.content.Context;
import android.os.Handler;

import io.github.lonamiwebs.stringlate.classes.Messenger;
import io.github.lonamiwebs.stringlate.interfaces.StringsSource;

public class RepoSyncTask extends Thread {

    private final RepoHandler mRepo;
    private final StringsSource mSource;
    private final Context mContext;
    private final Handler mHandler;

    public RepoSyncTask(final RepoHandler repo, final StringsSource source, final Context context) {
        mRepo = repo;
        mSource = source;
        mContext = context;
        mHandler = new Handler();
    }

    @Override
    public void run() {
        final boolean okay = mRepo.syncResources(mSource, new Messenger.OnRepoSyncProgress() {
            @Override
            public void onUpdate(final int stage, final float progress) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onProgressUpdate(stage, progress);
                    }
                });
            }
        });

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (okay)
                    Messenger.notifyRepoAdded(mRepo);
                else
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

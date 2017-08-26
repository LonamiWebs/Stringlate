package io.github.lonamiwebs.stringlate.classes.repos;

import android.content.Context;
import android.os.AsyncTask;

import io.github.lonamiwebs.stringlate.classes.Messenger;
import io.github.lonamiwebs.stringlate.interfaces.StringsSource;

public class RepoSyncTask extends AsyncTask<Void, Object, Boolean> {

    private final RepoHandler mRepo;
    private final StringsSource mSource;
    private final Context mContext;

    public RepoSyncTask(final RepoHandler repo, final StringsSource source, final Context context) {
        mRepo = repo;
        mSource = source;
        mContext = context;
    }

    @Override
    protected Boolean doInBackground(Void[] voids) {
        return mRepo.syncResources(mSource, new Messenger.OnRepoSyncProgress() {
            @Override
            public void onUpdate(int stage, float progress) {
                publishProgress(stage, progress);
            }
        });
    }

    @Override
    protected void onProgressUpdate(Object... values) {
        final int stage = (int)values[0];
        float progress = clamp((float)values[1], 0f, 1f);

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

    @Override
    protected void onPostExecute(Boolean okay) {
        if (okay)
            Messenger.notifyRepoAdded(mRepo);
        else
            mRepo.delete();
    }

    public void execute() {
        super.execute();
    }
}

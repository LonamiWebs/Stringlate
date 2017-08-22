package io.github.lonamiwebs.stringlate.classes.repos;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.interfaces.StringsSource;

public class RepoSyncTask extends AsyncTask<Void, RepoSyncTask.UpdateProgress, Boolean> {

    private final RepoHandler mRepo;
    private final StringsSource mSource;
    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final NotificationCompat.Builder mNotifyBuilder;

    private final String NOTIFICATION_CHANNEL_ID = "repo.sync";
    private final int NOTIFICATION_ID = 402;

    private boolean mFinishStatus;

    static class UpdateProgress {
        public final String title, description;

        public UpdateProgress(final String title, final String description) {
            this.title = title;
            this.description = description;
        }
    }

    public RepoSyncTask(final RepoHandler repo, final StringsSource source, final Context context) {
        mRepo = repo;
        mSource = source;
        mContext = context;
        mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotifyBuilder =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setOngoing(true);
    }

    @Override
    protected Boolean doInBackground(Void[] voids) {
        return mRepo.syncResources(mSource, new ProgressUpdateCallback() {
            @Override
            public void onProgressUpdate(String title, String description) {
                publishProgress(new UpdateProgress(title, description));
            }

            @Override
            public void showMessage(String message) {
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onProgressUpdate(UpdateProgress... values) {
        UpdateProgress progress = values[0];
        mNotifyBuilder.setContentTitle(progress.title);
        mNotifyBuilder.setContentText(progress.description);
        mNotificationManager.notify(NOTIFICATION_ID, mNotifyBuilder.build());
    }

    @Override
    protected void onPostExecute(Boolean okay) {
        mNotificationManager.cancel(NOTIFICATION_ID);

        if (okay) {
            // TODO Should this be put somewhere else?
            RepoHandler.notifyRepositoryCountChanged();
        }
    }

    public void execute() {
        super.execute();
    }
}

package io.github.lonamiwebs.stringlate.utilities;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.Random;

import io.github.lonamiwebs.stringlate.R;

// Helper class to run some code in the background and while it's running,
// show a notification. Once the code has been ran and if it succeeds,
// another clickable notification will spawn. Tapping it will take the
// user to the specified activity. If it didn't succeed, a toast will appear.
public abstract class NotificationRunner extends AsyncTask<Void, Void, Boolean> {

    protected final Context mContext;
    private final NotificationManager mNotificationManager;
    private final NotificationCompat.Builder mNotifyBuilder;

    @NonNull
    private String mRunningTitle, mRunningText, mSuccessTitle, mSuccessText, mFailText;
    private Intent mLaunchOnSuccess;

    private final int mNotificationId;

    protected NotificationRunner(final Context context) {
        mContext = context;

        mRunningTitle = mRunningText = mSuccessTitle = mSuccessText = mFailText = "";
        mNotificationId = 200 + new Random().nextInt(100);
        mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotifyBuilder =
                new NotificationCompat.Builder(context, "async.task")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setOngoing(true);
    }

    public NotificationRunner setRunning(final String title, final String text) {
        mRunningTitle = title;
        mRunningText = text;
        return this;
    }

    public NotificationRunner setSuccess(final String title, final String text, final Intent intent) {
        mSuccessTitle = title;
        mSuccessText = text;
        mLaunchOnSuccess = intent;
        return this;
    }

    public NotificationRunner setFailure(final String text) {
        mFailText = text;
        return this;
    }

    @Override
    protected void onPreExecute() {
        mNotifyBuilder
                .setContentTitle(mRunningTitle)
                .setContentText(mRunningText);

        if (!mRunningTitle.isEmpty() || !mRunningText.isEmpty())
            mNotificationManager.notify(mNotificationId, mNotifyBuilder.build());
    }

    protected void updateProgress(final String title, final String text) {
        mNotifyBuilder
                .setContentTitle(title)
                .setContentText(text);
        publishProgress();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        mNotificationManager.notify(mNotificationId, mNotifyBuilder.build());
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            mNotifyBuilder
                    .setContentTitle(mSuccessTitle)
                    .setContentText(mSuccessText)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(
                            mContext, 0, mLaunchOnSuccess, PendingIntent.FLAG_CANCEL_CURRENT
                    ));
            mNotificationManager.notify(mNotificationId, mNotifyBuilder.build());
        } else if (!mFailText.isEmpty()) {
            Toast.makeText(mContext, mFailText, Toast.LENGTH_SHORT).show();
        }
    }
}

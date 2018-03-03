package io.github.lonamiwebs.stringlate.activities.export;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.AbstractMap;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.git.GitHub;
import io.github.lonamiwebs.stringlate.classes.locales.LocaleString;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.RepoHandlerHelper;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;

public class CreatePullRequestActivity extends AppCompatActivity {

    //region Members

    private AppSettings mSettings;

    private TextView mInfoTextView;
    private EditText mCommitMessageEditText;

    private RepoHandler mRepo;
    private String mLocale;
    private String mUsername;

    private Boolean mNeedFork = null;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pull_request);

        mInfoTextView = findViewById(R.id.infoTextView);
        mCommitMessageEditText = findViewById(R.id.commitMessageEditText);

        mSettings = new AppSettings(this);

        Intent intent = getIntent();
        mRepo = RepoHandlerHelper.fromBundle(intent.getBundleExtra(EXTRA_REPO));
        mLocale = intent.getStringExtra(EXTRA_LOCALE);

        mCommitMessageEditText.setText(getString(
                R.string.updated_x_translation_spam, mLocale,
                LocaleString.getEnglishDisplay(mLocale)));

        checkPermissions();
    }

    //endregion

    //region First time setup

    private void checkPermissions() {
        new AsyncTask<Void, Void, AbstractMap.SimpleImmutableEntry<String, Boolean>>() {
            @Override
            protected AbstractMap.SimpleImmutableEntry<String, Boolean> doInBackground(Void... params) {
                return GitHub.canPush(mSettings.getGitHubToken(), mRepo);
            }

            @Override
            protected void onPostExecute(AbstractMap.SimpleImmutableEntry<String, Boolean> canPush) {
                if (canPush.getValue()) {
                    mInfoTextView.setText(R.string.can_push_no_pr);
                } else {
                    mInfoTextView.setText(R.string.cannot_push_will_pr);
                }
                mUsername = canPush.getKey();
                mNeedFork = !canPush.getValue();
            }
        }.execute();
    }

    //endregion

    //region Button events

    public void commitChanges(View view) {
        if (mNeedFork == null) {
            // Not ready yet
            Toast.makeText(this, R.string.loading_ellipsis, Toast.LENGTH_SHORT).show();
            return;
        }

        final String commitMessage = mCommitMessageEditText.getText().toString().trim();
        if (commitMessage.isEmpty()) {
            Toast.makeText(this, R.string.commit_msg_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        CreateUrlActivity.launchIntent(this, Exporter.createPullRequestExporter(
                mRepo, mNeedFork, mLocale, commitMessage,
                mUsername, mSettings.getGitHubToken()
        ));

        finish();
    }

    //endregion
}

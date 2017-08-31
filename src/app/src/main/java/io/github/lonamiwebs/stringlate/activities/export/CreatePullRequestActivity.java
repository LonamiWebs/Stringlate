package io.github.lonamiwebs.stringlate.activities.export;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.AbstractMap;
import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.LocaleString;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.git.GitHub;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.NotificationRunner;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;

public class CreatePullRequestActivity extends AppCompatActivity {

    //region Members

    private AppSettings mSettings;

    private TextView mInfoTextView;
    private Spinner mBranchesSpinner;
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
        mBranchesSpinner = findViewById(R.id.branchesSpinner);
        mCommitMessageEditText = findViewById(R.id.commitMessageEditText);

        mSettings = new AppSettings(this);

        Intent intent = getIntent();
        mRepo = RepoHandler.fromBundle(this, intent.getBundleExtra(EXTRA_REPO));
        mLocale = intent.getStringExtra(EXTRA_LOCALE);

        mCommitMessageEditText.setText(getString(
                R.string.added_x_translation_spam, mLocale,
                LocaleString.getEnglishDisplay(mLocale)));

        checkPermissions();
        checkBranches();
    }

    //endregion

    //region First time setup

    private void checkPermissions() {
        new AsyncTask<Void, Void, AbstractMap.SimpleImmutableEntry<String, Boolean>>() {
            @Override
            protected AbstractMap.SimpleImmutableEntry<String, Boolean> doInBackground(Void... params) {
                return GitHub.gCanPush(mSettings.getGitHubToken(), mRepo);
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

    private void checkBranches() {
        new AsyncTask<Void, Void, ArrayList<String>>() {
            @Override
            protected ArrayList<String> doInBackground(Void... params) {
                ArrayList<String> result = new ArrayList<>();

                JSONArray branches = GitHub.gGetBranches(mRepo);
                if (branches != null) {
                    try {
                        for (int i = 0; i < branches.length(); i++) {
                            result.add(branches.getJSONObject(i).getString("name"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                return result;
            }

            @Override
            protected void onPostExecute(ArrayList<String> branches) {
                loadBranchesSpinner(branches);
            }
        }.execute();
    }

    //endregion

    //region Spinner loading

    private void loadBranchesSpinner(ArrayList<String> branches) {
        ArrayAdapter<String> idAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, branches);

        idAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBranchesSpinner.setAdapter(idAdapter);
    }

    //endregion

    //region Button events

    public void commitChanges(View view) {
        final String branch = (String) mBranchesSpinner.getSelectedItem();
        if (mNeedFork == null || branch == null) {
            Toast.makeText(this, R.string.loading_ellipsis, Toast.LENGTH_SHORT).show();
            return;
        }

        final String commitMessage = mCommitMessageEditText.getText().toString().trim();
        if (commitMessage.isEmpty()) {
            Toast.makeText(this, R.string.commit_msg_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        new NotificationRunner(this) {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    final String postedUrl = Exporter.getPullRequestExporter(
                            mRepo, mContext, mNeedFork, mLocale, branch, commitMessage,
                            mUsername, mSettings.getGitHubToken()
                    ).call();
                    setSuccess(
                            getString(R.string.done),
                            postedUrl,
                            CreateUrlSuccessActivity.createIntent(
                                    mContext, getString(R.string.done), postedUrl)
                    );
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            }
        }.setFailure(getString(R.string.something_went_wrong))
                .execute();

        finish();
    }

    //endregion
}

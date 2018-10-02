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
        mRepo = RepoHandlerHelper.fromBundle(intent.getBundleExtra(EXTRA_REPO));
        mLocale = intent.getStringExtra(EXTRA_LOCALE);

        mCommitMessageEditText.setText(getString(
                R.string.updated_x_translation_spam, mLocale,
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

    private void checkBranches() {
        new AsyncTask<Void, Void, ArrayList<String>>() {
            @Override
            protected ArrayList<String> doInBackground(Void... params) {
                ArrayList<String> result = new ArrayList<>();

                JSONArray branches = GitHub.getBranches(mRepo);
                if (branches != null) {
                    try {
                        for (int i = 0; i < branches.length(); i++) {
                            result.add(branches.getJSONObject(i).getString("name"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                if (result.isEmpty()) {
                    result.add("master");
                }
                if (result.size() > 1) {
                    String defaultBranch = GitHub.getDefaultBranch(mRepo);
                    if (defaultBranch != null && result.contains(defaultBranch)) {
                        result.remove(defaultBranch);
                        result.add(0, defaultBranch);
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
        if (branches.size() == 1) {
            // No need to let the user select a BASE branch if there's only one
            findViewById(R.id.branchesLayout).setVisibility(View.GONE);
        }
    }

    //endregion

    //region Button events

    public void commitChanges(View view) {
        final String baseBranch = (String) mBranchesSpinner.getSelectedItem();
        if (mNeedFork == null || baseBranch == null) {
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
                mRepo, mNeedFork, mLocale, baseBranch, commitMessage,
                mUsername, mSettings.getGitHubToken()
        ));

        finish();
    }

    //endregion
}

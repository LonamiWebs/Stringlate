package io.github.lonamiwebs.stringlate.activities.export;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InvalidObjectException;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.LocaleString;
import io.github.lonamiwebs.stringlate.git.GitHub;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.utilities.Utils;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;

public class CreateIssueActivity extends AppCompatActivity {

    //region Members

    private AppSettings mSettings;

    private RepoHandler mRepo;
    private String mLocale;

    private EditText mIssueTitleEditText;
    private EditText mIssueDescriptionEditText;

    private int mExistingIssueNumber;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_issue);

        mSettings = new AppSettings(this);

        mIssueTitleEditText = (EditText)findViewById(R.id.issueTitleEditText);
        mIssueDescriptionEditText = (EditText)findViewById(R.id.issueDescriptionEditText);

        // Retrieve the strings.xml content to be exported
        Intent intent = getIntent();
        mRepo = RepoHandler.fromBundle(this, intent.getBundleExtra(EXTRA_REPO));
        mLocale = intent.getStringExtra(EXTRA_LOCALE);
        mExistingIssueNumber = mRepo.getCreatedIssue(mLocale);

        String display = LocaleString.getEnglishDisplay(mLocale);
        if (mExistingIssueNumber == -1) {
            mIssueTitleEditText.setText(getString(R.string.added_x_translation, mLocale, display));
            mIssueDescriptionEditText.setText(getString(R.string.new_issue_template, mLocale, display));
        } else {
            findViewById(R.id.issueTitleLayout).setVisibility(View.GONE);
            mIssueDescriptionEditText.setText(getString(R.string.comment_issue_template, mLocale, display));
        }
    }

    //endregion

    //region Button events

    public void onCreateIssue(final View v) {
        String title = null;
        if (mExistingIssueNumber == -1) {
            title = mIssueTitleEditText.getText().toString().trim();
            if (title.isEmpty()) {
                mIssueTitleEditText.setError(getString(R.string.title_empty));
                return;
            }
        }
        String description = mIssueDescriptionEditText.getText().toString().trim();
        if (!description.contains("%x")) {
            mIssueDescriptionEditText.setError(getString(R.string.issue_desc_x));
            return;
        } else {
            String xml = mRepo.mergeDefaultTemplate(mLocale);
            description = description.replace("%x", String.format("```xml\n%s\n```", xml));
        }
        if (Utils.isNotConnected(this, true))
            return;

        final ProgressDialog progress = ProgressDialog.show(this,
                getString(R.string.creating_issue_ellipsis),
                getString(R.string.creating_issue_long_ellipsis), true);

        new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... desc) {
                try {
                    if (mExistingIssueNumber == -1)
                        return GitHub.gCreateIssue(mRepo, desc[0],
                                desc[1], mSettings.getGitHubToken());
                    else
                        return GitHub.gCommentIssue(mRepo, mExistingIssueNumber,
                                desc[1], mSettings.getGitHubToken());
                } catch (InvalidObjectException ignored) {
                    // This wasn't a GitHub repository. How did we get here?
                    return null;
                }
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                progress.dismiss();
                onIssueCreated(result);
            }
        }.execute(title, description);
    }

    //endregion

    //region Check posted issue

    private void onIssueCreated(JSONObject jsonObject) {
        try {
            if (jsonObject == null) throw new JSONException("Invalid GitHub repository.");

            String postedUrl = jsonObject.getString("html_url");
            if (mExistingIssueNumber == -1) {
                mExistingIssueNumber = jsonObject.getInt("number");
                mRepo.addCreatedIssue(mLocale, mExistingIssueNumber);
            }

            finish();
            CreateUrlSuccessActivity.launchIntent(
                    this, getString(R.string.create_issue_success), postedUrl);
        }
        catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.create_issue_error, Toast.LENGTH_SHORT).show();
        }
    }

    //endregion
}

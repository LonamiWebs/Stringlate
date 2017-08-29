package io.github.lonamiwebs.stringlate.activities.export;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import org.json.JSONObject;

import java.io.InvalidObjectException;

import io.github.lonamiwebs.stringlate.utilities.NotificationRunner;
import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.LocaleString;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.git.GitHub;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.Helpers;

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

        mIssueTitleEditText = findViewById(R.id.issueTitleEditText);
        mIssueDescriptionEditText = findViewById(R.id.issueDescriptionEditText);

        // Retrieve the strings.xml content to be exported
        Intent intent = getIntent();
        mRepo = RepoHandler.fromBundle(this, intent.getBundleExtra(EXTRA_REPO));
        mLocale = intent.getStringExtra(EXTRA_LOCALE);

        Integer issue = mRepo.settings.getCreatedIssues().get(mLocale);
        mExistingIssueNumber = issue == null ? -1 : issue;

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
        if (new Helpers(this).isDisconnectedFromInternet(R.string.no_internet_connection))
            return;

        final String issueTitle = title;
        final String issueDesc = description;
        new NotificationRunner(this) {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    final JSONObject result;
                    if (mExistingIssueNumber == -1) {
                        result = GitHub.gCreateIssue(
                                mRepo, issueTitle, issueDesc, mSettings.getGitHubToken()
                        );
                    } else {
                        result = GitHub.gCommentIssue(
                                mRepo, mExistingIssueNumber, issueDesc, mSettings.getGitHubToken()
                        );
                    }

                    if (result == null)
                        throw new InvalidObjectException("Invalid GitHub repository.");

                    String postedUrl = result.optString("html_url", "");
                    if (postedUrl.isEmpty())
                        throw new InvalidObjectException("Invalid GitHub repository.");

                    if (mExistingIssueNumber == -1) {
                        mExistingIssueNumber = result.optInt("number");
                        mRepo.settings.addCreatedIssue(mLocale, mExistingIssueNumber);
                    }

                    setSuccess(
                            getString(R.string.create_issue_success),
                            postedUrl,
                            CreateUrlSuccessActivity.createIntent(
                                    mContext, getString(R.string.create_issue_success), postedUrl)
                    );
                    return true;

                } catch (InvalidObjectException ignored) {
                    // This wasn't a GitHub repository. How did we get here?
                    return false;
                }
            }
        }.setRunning(getString(R.string.creating_issue_ellipsis), getString(R.string.creating_issue_long_ellipsis))
                .setFailure(getString(R.string.create_issue_error))
                .execute();

        finish();
    }

    //endregion

}

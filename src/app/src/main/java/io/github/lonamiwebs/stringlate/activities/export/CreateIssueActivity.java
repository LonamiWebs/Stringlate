package io.github.lonamiwebs.stringlate.activities.export;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.locales.LocaleString;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.ContextUtils;
import io.github.lonamiwebs.stringlate.utilities.RepoHandlerHelper;

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
        mRepo = RepoHandlerHelper.fromBundle(intent.getBundleExtra(EXTRA_REPO));
        mLocale = intent.getStringExtra(EXTRA_LOCALE);

        Integer issue = mRepo.settings.getCreatedIssues().get(mLocale);
        mExistingIssueNumber = issue == null ? -1 : issue;

        String display = LocaleString.getEnglishDisplay(mLocale);
        if (mExistingIssueNumber == -1) {
            mIssueTitleEditText.setText(getString(R.string.updated_x_translation, mLocale, display));
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
            // GitHub seems to need two empty newlines after HTML tags if we want Markdown again.
            // It doesn't seem to be necessary for the closing tag, but keep it for safety.
            String xml = mRepo.mergeDefaultTemplate(
                    mLocale,
                    "<details><summary>",
                    "</summary>\n\n```xml\n",
                    "\n```\n\n</details>\n"
            );
            description = description.replace("%x", xml);
        }
        if (!new ContextUtils(this).isConnectedToInternet(R.string.no_internet_connection))
            return;

        CreateUrlActivity.launchIntent(this, Exporter.createIssueExporter(
                mRepo, mExistingIssueNumber, mLocale,
                title, description, mSettings.getGitHubToken()
        ));

        finish();
    }

    //endregion

}

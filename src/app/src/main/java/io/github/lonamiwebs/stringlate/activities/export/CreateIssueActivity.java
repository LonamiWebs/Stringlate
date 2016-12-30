package io.github.lonamiwebs.stringlate.activities.export;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.LocaleString;
import io.github.lonamiwebs.stringlate.interfaces.Callback;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.GitHub;
import io.github.lonamiwebs.stringlate.utilities.RepoHandler;

import static android.view.View.GONE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_XML_CONTENT;

public class CreateIssueActivity extends AppCompatActivity {

    //region Members

    private AppSettings mSettings;

    private RepoHandler mRepo;
    private String mXmlContent;
    private String mLocale;

    private String mPostedUrl;

    private LinearLayout mIssueCreationLayout;
    private LinearLayout mIssueCreatedLayout;

    private EditText mIssueTitleEditText;
    private EditText mIssueDescriptionEditText;

    private EditText mIssueUrlEditText;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_issue);

        mSettings = new AppSettings(this);

        mIssueCreationLayout = (LinearLayout)findViewById(R.id.issueCreationLayout);
        mIssueCreatedLayout = (LinearLayout)findViewById(R.id.issueCreatedLayout);

        mIssueTitleEditText = (EditText)findViewById(R.id.issueTitleEditText);
        mIssueDescriptionEditText = (EditText)findViewById(R.id.issueDescriptionEditText);

        mIssueUrlEditText = (EditText)findViewById(R.id.issueUrlEditText);

        // Retrieve the strings.xml content to be exported
        Intent intent = getIntent();
        mRepo = RepoHandler.fromBundle(this, intent.getBundleExtra(EXTRA_REPO));
        mXmlContent = intent.getStringExtra(EXTRA_XML_CONTENT);
        mLocale = intent.getStringExtra(EXTRA_LOCALE);

        String display = LocaleString.getDisplay(mLocale);
        mIssueTitleEditText.setText(getString(R.string.added_x_translation, mLocale, display));
        mIssueDescriptionEditText.setText(getString(R.string.new_issue_template, mLocale, display));
    }

    //endregion

    //region Button events

    // Before creating the issue
    public void onCreateIssue(final View v) {
        String title = mIssueTitleEditText.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "The issue title cannot be empty.", Toast.LENGTH_SHORT);
            return;
        }
        String description = mIssueDescriptionEditText.getText().toString().trim();
        if (!description.contains("%x")) {
            Toast.makeText(this, "The issue description must contain \"%x\".", Toast.LENGTH_SHORT).show();
            return;
        } else {
            description = description.replace("%x", String.format("```xml\n%s\n```", mXmlContent));
        }
        if (!GitHub.gCanCall()) {
            Toast.makeText(this,
                    R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        mIssueCreationLayout.setVisibility(GONE);
        final ProgressDialog progress = ProgressDialog.show(this,
                getString(R.string.creating_issue_ellipsis),
                getString(R.string.creating_issue_long_ellipsis), true);

        new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... desc) {
                return GitHub.gCreateIssue(mRepo, desc[0], desc[1], mSettings.getGitHubToken());
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                progress.dismiss();
                onIssueCreated(result);
            }
        }.execute(title, description);
    }

    // After creating the issue
    public void onOpenUrl(final View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mPostedUrl));
        startActivity(browserIntent);
    }

    public void onCopyUrl(final View v) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("url", mPostedUrl));
        Toast.makeText(this, R.string.url_copied, Toast.LENGTH_SHORT).show();
    }

    public void onShareUrl(final View v) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, mPostedUrl);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_url)));
    }

    public void onExit(final View v) {
        finish();
    }

    //endregion

    //region Check posted issue

    private void onIssueCreated(JSONObject jsonObject) {
        try {
            mPostedUrl = jsonObject.getString("html_url");

            Toast.makeText(this, R.string.create_issue_success, Toast.LENGTH_SHORT).show();
            mIssueUrlEditText.setText(mPostedUrl);
            mIssueCreatedLayout.setVisibility(View.VISIBLE);
        }
        catch (JSONException e) {
            Toast.makeText(this, R.string.create_issue_error, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    //endregion
}

package io.github.lonamiwebs.stringlate.activities.export;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.interfaces.Callback;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.GitHub;

import static android.view.View.GONE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_FILENAME;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_XML_CONTENT;

public class CreateGistActivity extends AppCompatActivity {

    //region Members

    private AppSettings mSettings;

    private String mXmlContent;
    private String mFilename;

    private String mPostedUrl;

    private LinearLayout mGistCreationLayout;
    private LinearLayout mGistCreatedLayout;

    private EditText mDescriptionEditText;
    private CheckBox mIsPublicCheckBox;
    private CheckBox mIsAnonymousCheckBox;
    private EditText mFilenameEditText;

    private EditText mGistUrlEditText;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_gist);

        mSettings = new AppSettings(this);

        mGistCreationLayout = (LinearLayout)findViewById(R.id.gistCreationLayout);
        mGistCreatedLayout = (LinearLayout)findViewById(R.id.gistCreatedLayout);

        mDescriptionEditText = (EditText)findViewById(R.id.gistDescriptionEditText);
        mIsPublicCheckBox = (CheckBox)findViewById(R.id.gistIsPublicCheckBox);
        mIsAnonymousCheckBox = (CheckBox)findViewById(R.id.gistIsAnonymousCheckBox);
        mFilenameEditText = (EditText)findViewById(R.id.gistFilenameEditText);

        mGistUrlEditText = (EditText)findViewById(R.id.gistUrlEditText);

        // Retrieve the strings.xml content to be exported
        Intent intent = getIntent();
        mXmlContent = intent.getStringExtra(EXTRA_XML_CONTENT);
        mFilename = intent.getStringExtra(EXTRA_FILENAME);

        mFilenameEditText.setText(mFilename);
        setTitle(getString(R.string.posting_gist_title, mFilename));

        // Check whether the Gist can be non-anonymous
        boolean notAuth = !mSettings.hasGitHubAuthorization();
        mIsAnonymousCheckBox.setChecked(notAuth);
        if (notAuth) {
            mIsAnonymousCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton cb, boolean b) {
                    Toast.makeText(getApplicationContext(), R.string.gist_login_needed, Toast.LENGTH_LONG).show();
                    mIsAnonymousCheckBox.setChecked(true);
                }
            });
        }
    }

    //endregion

    //region Button events

    // Before posting Gist
    public void onPostGist(final View v) {
        String filename = mFilenameEditText.getText().toString().trim();
        if (filename.length() == 0) {
            mFilenameEditText.setError(getString(R.string.error_gist_filename_empty));
            return;
        }
        if (!GitHub.gCanCall()) {
            Toast.makeText(getApplicationContext(),
                    R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        mGistCreationLayout.setVisibility(GONE);
        final ProgressDialog progress = ProgressDialog.show(this,
                getString(R.string.posting_gist_ellipsis),
                getString(R.string.posting_gist_ellipsis_long), true);

        String description = mDescriptionEditText.getText().toString().trim();
        boolean isPublic = mIsPublicCheckBox.isChecked();
        boolean isAnonymous = mIsAnonymousCheckBox.isChecked() ||
                !mSettings.hasGitHubAuthorization();

        String token = isAnonymous ? null : mSettings.getGitHubToken();
        GitHub.gCreateGist(description, isPublic, filename, mXmlContent, token,
                new Callback<Object>() {
                    @Override
                    public void onCallback(Object jsonObject) {
                        progress.dismiss();
                        onGistPosted((JSONObject)jsonObject);
                    }
                });
    }

    // After posting Gist
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

    //region Check posted Gist

    private void onGistPosted(JSONObject jsonObject) {
        try {
            mPostedUrl = jsonObject.getString("html_url");

            Toast.makeText(this, R.string.post_gist_success, Toast.LENGTH_SHORT).show();
            mGistUrlEditText.setText(mPostedUrl);
            mGistCreatedLayout.setVisibility(View.VISIBLE);
        }
        catch (JSONException e) {
            Toast.makeText(this, R.string.post_gist_error, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    //endregion
}

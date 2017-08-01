package io.github.lonamiwebs.stringlate.activities.export;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.git.GitHub;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.Utils;

import static android.view.View.GONE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;

public class CreateGistActivity extends AppCompatActivity {

    //region Members

    private AppSettings mSettings;

    private RepoHandler mRepo;
    private String mLocale;

    private EditText mDescriptionEditText;
    private CheckBox mIsPublicCheckBox;
    private CheckBox mIsAnonymousCheckBox;
    private EditText mFilenameEditText;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_gist);

        mSettings = new AppSettings(this);

        mDescriptionEditText = (EditText) findViewById(R.id.gistDescriptionEditText);
        mIsPublicCheckBox = (CheckBox) findViewById(R.id.gistIsPublicCheckBox);
        mIsAnonymousCheckBox = (CheckBox) findViewById(R.id.gistIsAnonymousCheckBox);
        mFilenameEditText = (EditText) findViewById(R.id.gistFilenameEditText);

        // Retrieve the strings.xml content to be exported
        Intent intent = getIntent();
        mRepo = RepoHandler.fromBundle(this, intent.getBundleExtra(EXTRA_REPO));
        mLocale = intent.getStringExtra(EXTRA_LOCALE);

        File[] defaultResources = mRepo.getDefaultResourcesFiles();
        if (defaultResources.length > 1) {
            // More than one file, don't allow to change the filename - use the original filename
            mFilenameEditText.setVisibility(GONE);
            setTitle(getString(R.string.posting_gist_title, ""));
        } else {
            // Only one file, let the user change it if they want to
            String filename = defaultResources[0].getName();
            mFilenameEditText.setText(filename);
            setTitle(getString(R.string.posting_gist_title, filename));
        }

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

    public void onPostGist(final View v) {
        final HashMap<String, String> fileContents = new HashMap<>();

        File[] defaultResources = mRepo.getDefaultResourcesFiles();
        if (defaultResources.length > 1) {
            for (File template : defaultResources) {
                String content = mRepo.applyTemplate(template, mLocale);
                if (!content.isEmpty())
                    fileContents.put(template.getName(), content);
            }
        } else {
            final String filename = mFilenameEditText.getText().toString().trim();
            if (filename.length() == 0) {
                mFilenameEditText.setError(getString(R.string.error_gist_filename_empty));
                return;
            } else {
                fileContents.put(filename,
                        mRepo.applyTemplate(defaultResources[0], mLocale));
            }
        }
        if (Utils.isNotConnected(this, true))
            return;

        final ProgressDialog progress = ProgressDialog.show(this,
                getString(R.string.posting_gist_ellipsis),
                getString(R.string.posting_gist_ellipsis_long), true);

        final String description = mDescriptionEditText.getText().toString().trim();
        final boolean isPublic = mIsPublicCheckBox.isChecked();
        final boolean isAnonymous = mIsAnonymousCheckBox.isChecked() ||
                !mSettings.hasGitHubAuthorization();

        final String token = isAnonymous ? "" : mSettings.getGitHubToken();
        new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... params) {
                return GitHub.gCreateGist(description, isPublic, fileContents, token);
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                progress.dismiss();
                onGistPosted(result);
            }
        }.execute();
    }

    //endregion

    //region Check posted Gist

    private void onGistPosted(JSONObject jsonObject) {
        try {
            String postedUrl = jsonObject.getString("html_url");
            finish();
            CreateUrlSuccessActivity.launchIntent(
                    this, getString(R.string.post_gist_success), postedUrl);
        } catch (JSONException e) {
            Toast.makeText(this, R.string.post_gist_error, Toast.LENGTH_SHORT).show();
        }
    }

    //endregion
}

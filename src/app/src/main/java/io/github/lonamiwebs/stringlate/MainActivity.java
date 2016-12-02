package io.github.lonamiwebs.stringlate;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.Interfaces.Callback;
import io.github.lonamiwebs.stringlate.Interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.Utilities.GitHub;
import io.github.lonamiwebs.stringlate.Utilities.RepoHandler;

public class MainActivity extends AppCompatActivity {

    //region Members

    public final static String EXTRA_REPO_OWNER = "io.github.lonamiwebs.stringlate.REPO_OWNER";
    public final static String EXTRA_REPO_NAME = "io.github.lonamiwebs.stringlate.REPO_NAME";

    private AutoCompleteTextView mOwnerEditText, mRepositoryEditText;
    private AutoCompleteTextView mUrlEditText;

    private Pattern mOwnerProjectPattern; // Match user and repository name from a GitHub url

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOwnerEditText = (AutoCompleteTextView)findViewById(R.id.ownerEditText);
        mRepositoryEditText = (AutoCompleteTextView)findViewById(R.id.repositoryEditText);
        mOwnerEditText.addTextChangedListener(onOwnerChanged);

        mUrlEditText = (AutoCompleteTextView)findViewById(R.id.urlEditText);

        mOwnerProjectPattern = Pattern.compile(
                "(?:https?://github\\.com/|git@github.com:)([\\w-]+)/([\\w-]+)(?:/|\\.git)?");

        // Check if we opened the application because a GitHub link was clicked
        if(getIntent().getData() != null){
            Uri data = getIntent().getData();
            String scheme = data.getScheme();
            String fullPath = data.getEncodedSchemeSpecificPart();

            mUrlEditText.setText(scheme+":"+fullPath);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOwnerAutocomplete();
        loadUrlAutocomplete();
    }

    //endregion

    //region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.discoverRepositories:
                startActivity(new Intent(getApplicationContext(), DiscoverActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //endregion

    //region UI events

    public void onNextClick(final View v) {
        String owner, repository;
        String url;

        owner = repository = null;
        url = mUrlEditText.getText().toString().trim();

        // If an URL was entered, try to extract the owner and repository
        if (!url.isEmpty()) {
            Matcher m = mOwnerProjectPattern.matcher(url);
            if (m.matches()) {
                owner = m.group(1).trim();
                repository = m.group(2).trim();
            }
        }
        // If we don't have any owner (and thus repository) yet, retrieve it from the EditTexts
        if (owner == null) {
            owner = mOwnerEditText.getText().toString().trim();
            repository = mRepositoryEditText.getText().toString().trim();
        }

        if (owner.isEmpty() || repository.isEmpty()) {
            Toast.makeText(this, R.string.repo_or_url_required,
                    Toast.LENGTH_SHORT).show();
        } else {
            // Determine whether we already have this repo or if it's a new one
            if (new RepoHandler(this, owner, repository).isEmpty())
                checkRepositoryOK(owner, repository);
            else
                launchTranslateActivity(owner, repository);
        }
    }

    //endregion

    //region Checking and adding a new local "repository"

    // Step 1
    private void checkRepositoryOK(final String owner, final String repository) {
        final ProgressDialog progress = ProgressDialog.show(this,
                getString(R.string.checking_repo_ok),
                getString(R.string.checking_repo_ok_long), true);

        // TODO It says the repository is invalid when it is valid but there is no internet!
        GitHub.gCheckOwnerRepoOK(owner, repository, new Callback<Boolean>() {
            @Override
            public void onCallback(Boolean ok) {
                if (ok) {
                    scanDownloadStrings(owner, repository, progress);
                } else {
                    Toast.makeText(getApplicationContext(),
                            R.string.invalid_repo, Toast.LENGTH_SHORT).show();
                    progress.dismiss();
                }
            }
        });
    }

    // Steps 2 a 3
    private void scanDownloadStrings(final String owner, final String repository,
                                     final ProgressDialog progress) {
        RepoHandler handler = new RepoHandler(this, owner, repository);
        handler.syncResources(new ProgressUpdateCallback() {
            @Override
            public void onProgressUpdate(String title, String description) {
                progress.setTitle(title);
                progress.setMessage(description);
            }

            @Override
            public void onProgressFinished(String description, boolean status) {
                progress.dismiss();
                if (description != null)
                    Toast.makeText(getApplicationContext(), description, Toast.LENGTH_SHORT).show();
                if (status)
                    launchTranslateActivity(owner, repository);
            }
        });
    }

    //endregion

    //region Local repository handling

    private TextWatcher onOwnerChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            loadRepositoryAutocomplete();
        }

        @Override
        public void afterTextChanged(Editable editable) { }
    };

    private void loadOwnerAutocomplete() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, RepoHandler.listOwners(this));

        mOwnerEditText.setAdapter(adapter);
    }

    private void loadRepositoryAutocomplete() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                RepoHandler.listRepositories(this, mOwnerEditText.getText().toString()));

        mRepositoryEditText.setAdapter(adapter);
    }

    private void loadUrlAutocomplete() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, RepoHandler.listRepositories(this));

        mUrlEditText.setAdapter(adapter);
    }

    //endregion

    //region Utilities

    private void launchTranslateActivity(String owner, String repository) {
        Intent intent = new Intent(getApplicationContext(), TranslateActivity.class);
        intent.putExtra(EXTRA_REPO_OWNER, owner);
        intent.putExtra(EXTRA_REPO_NAME, repository);
        startActivity(intent);
    }

    //endregion
}

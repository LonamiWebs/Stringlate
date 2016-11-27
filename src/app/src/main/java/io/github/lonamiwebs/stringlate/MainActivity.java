package io.github.lonamiwebs.stringlate;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.Interfaces.Callback;
import io.github.lonamiwebs.stringlate.Interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.Utilities.GitHub;
import io.github.lonamiwebs.stringlate.Utilities.RepoHandler;

public class MainActivity extends AppCompatActivity {

    public final static String EXTRA_REPO_OWNER = "io.github.lonamiwebs.stringlate.REPO_OWNER";
    public final static String EXTRA_REPO_NAME = "io.github.lonamiwebs.stringlate.REPO_NAME";

    EditText mOwnerEditText, mRepositoryEditText;
    EditText mUrlEditText;

    Pattern mOwnerProjectPattern; // Match user and repository name from a GitHub url

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOwnerEditText = (EditText)findViewById(R.id.ownerEditText);
        mRepositoryEditText = (EditText)findViewById(R.id.repositoryEditText);

        mUrlEditText = (EditText)findViewById(R.id.urlEditText);

        mOwnerProjectPattern = Pattern.compile(
                "(?:https?://github\\.com/|git@github.com:)([\\w-]+)/([\\w-]+)(?:\\.git)?");
    }

    public void onContinueClick(final View v) {
        String owner, repository;
        String url;

        url = mUrlEditText.getText().toString().trim();

        if (!url.isEmpty()) {
            Matcher m = mOwnerProjectPattern.matcher(url);
            if (m.matches()) {
                owner = m.group(1);
                repository = m.group(2);
            } else {
                owner = repository = "";
            }
        } else {
            owner = mOwnerEditText.getText().toString().trim();
            repository = mRepositoryEditText.getText().toString().trim();
        }

        if (owner.isEmpty() || repository.isEmpty()) {
            Toast.makeText(this, "Please enter a project owner and name or an URL.",
                    Toast.LENGTH_SHORT).show();
        } else {
            // Determine whether we already have this repo or if it's a new one
            if (new RepoHandler(this, owner, repository).isEmpty())
                checkRepositoryOK(owner, repository);
            else
                launchTranslateActivity(owner, repository);
        }
    }

    // Step 1
    private void checkRepositoryOK(final String owner, final String repository) {
        final ProgressDialog progress = ProgressDialog.show(this,
                "Checking values correctness...",
                "Checking for the owner and repository existence...", true);

        GitHub.gCheckOwnerRepoOK(owner, repository, new Callback<Boolean>() {
            @Override
            public void onCallback(Boolean ok) {
                if (ok) {
                    scanDownloadStrings(owner, repository, progress);
                } else {
                    showToast("The input repository doesn't seem valid.");
                    progress.dismiss();
                }
            }
        });
    }

    private void scanDownloadStrings(final String owner, final String repository,
                                     final ProgressDialog progress) {
        RepoHandler handler = new RepoHandler(this, owner, repository).init();
        handler.updateStrings(new ProgressUpdateCallback() {
            @Override
            public void onProgressUpdate(String title, String description) {
                progress.setTitle(title);
                progress.setMessage(description);
            }

            @Override
            public void onProgressFinished(String description, boolean status) {
                progress.dismiss();
                showToast(description);
                if (status)
                    launchTranslateActivity(owner, repository);
            }
        });
    }

    void launchTranslateActivity(String owner, String repository) {
        Intent intent = new Intent(getApplicationContext(), TranslateActivity.class);
        intent.putExtra(EXTRA_REPO_OWNER, owner);
        intent.putExtra(EXTRA_REPO_NAME, repository);
        startActivity(intent);
    }

    void showToast(String text) {
        if (text != null)
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}

package io.github.lonamiwebs.stringlate;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.Interfaces.Callback;
import io.github.lonamiwebs.stringlate.Utilities.GitHub;
import io.github.lonamiwebs.stringlate.Utilities.RepoHandler;

public class MainActivity extends AppCompatActivity {

    public final static String EXTRA_REPO_OWNER = "io.github.lonamiwebs.stringlate.REPO_OWNER";
    public final static String EXTRA_REPO_NAME = "io.github.lonamiwebs.stringlate.REPO_NAME";

    EditText mOwnerEditText, mRepositoryEditText;
    EditText mUrlEditText;

    Pattern mOwnerProjectPattern; // Match user and repository name from a GitHub url
    Pattern mValuesLocalePattern; // Match locale from "res/values-(...)/strings.xml"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOwnerEditText = (EditText)findViewById(R.id.ownerEditText);
        mRepositoryEditText = (EditText)findViewById(R.id.repositoryEditText);

        mUrlEditText = (EditText)findViewById(R.id.urlEditText);

        mOwnerProjectPattern = Pattern.compile(
                "(?:https?://github\\.com/|git@github.com:)([\\w-]+)/([\\w-]+)(?:\\.git)?");

        mValuesLocalePattern = Pattern.compile(
                "res/values(?:-([\\w-]+))?/strings\\.xml");
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
            checkRepositoryOK(owner, repository);
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
                    scanStringsXml(owner, repository, progress);
                } else {
                    showToast("The input repository doesn't seem valid.");
                    progress.dismiss();
                }
            }
        });
    }

    // Step 2
    private void scanStringsXml(final String owner, final String repository,
                                final ProgressDialog progress) {
        progress.setTitle("Scanning repository...");
        progress.setMessage("Looking for strings.xml files in the repository...");
        GitHub.gGetTopTree(owner, repository, true, new Callback<Object>() {
            @Override
            public void onCallback(Object o) {
                ArrayList<String> paths = new ArrayList<>();
                ArrayList<String> locales = new ArrayList<>();
                try {
                    JSONObject json = (JSONObject) o;
                    JSONArray tree = json.getJSONArray("tree");
                    for (int i = 0; i < tree.length(); i++) {
                        JSONObject item = tree.getJSONObject(i);
                        Matcher m = mValuesLocalePattern.matcher(item.getString("path"));
                        if (m.find()) {
                            paths.add(item.getString("path"));
                            locales.add(m.group(1));
                        }
                    }
                    if (paths.size() == 0) {
                        showToast("No strings.xml files were found in this repository.");
                        progress.dismiss();
                    } else {
                        downloadLocales(owner, repository, paths, locales, progress);
                    }
                } catch (JSONException e) {
                    showToast("Error parsing the JSON from the API request.");
                    progress.dismiss();
                }
            }
        });
    }

    // Step 3
    private void downloadLocales(final String owner, final String repository,
                                 final ArrayList<String> paths, final ArrayList<String> locales,
                                 final ProgressDialog progress) {
        progress.setTitle("Downloading locales...");
        progress.setMessage("Downloading strings.xml files for you to translate...");
        final RepoHandler repoHandler = new RepoHandler(this, owner, repository);

        new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                for (int i = 0; i < paths.size(); i++) {
                    publishProgress(i);
                    repoHandler.downloadLocale(paths.get(i), locales.get(i));
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                int i = values[0];
                String locale = locales.get(i) == null ? "default" : locales.get(i);

                progress.setTitle(String.format("Downloading %d/%d locale (%s)...",
                        i+1, paths.size(), locale));

                super.onProgressUpdate(values);
            }

            @Override
            protected void onPostExecute(Void v) {
                progress.dismiss();

                Intent intent = new Intent(getApplicationContext(), TranslateActivity.class);
                intent.putExtra(EXTRA_REPO_OWNER, owner);
                intent.putExtra(EXTRA_REPO_NAME, repository);
                startActivity(intent);

                super.onPostExecute(v);
            }
        }.execute();
    }

    void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}

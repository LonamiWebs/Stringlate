package io.github.lonamiwebs.stringlate.activities.repositories;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.DiscoverActivity;
import io.github.lonamiwebs.stringlate.activities.translate.TranslateActivity;
import io.github.lonamiwebs.stringlate.classes.applications.ApplicationDetails;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.classes.sources.GitSource;
import io.github.lonamiwebs.stringlate.git.GitCloneProgressCallback;
import io.github.lonamiwebs.stringlate.git.GitWrapper;
import io.github.lonamiwebs.stringlate.utilities.StringlateApi;
import io.github.lonamiwebs.stringlate.utilities.Utils;

import static android.app.Activity.RESULT_OK;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_REPO_DISCOVERED;

public class AddNewRepositoryFragment extends Fragment {

    //region Members

    private EditText mOwnerEditText, mRepositoryEditText;
    private EditText mUrlEditText;

    // All details may not be filled out. Used as temporary data container till RepoHandler creation
    private ApplicationDetails mProjectDetails;

    //endregion

    //region Initialization

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_new_repository, container, false);

        mOwnerEditText = (EditText) rootView.findViewById(R.id.ownerEditText);
        mRepositoryEditText = (EditText) rootView.findViewById(R.id.repositoryEditText);

        mUrlEditText = (EditText) rootView.findViewById(R.id.urlEditText);
        mProjectDetails = new ApplicationDetails();

        // Set button events
        rootView.findViewById(R.id.discoverButton).setOnClickListener(onDiscoverClick);
        rootView.findViewById(R.id.nextButton).setOnClickListener(onNextClick);

        // Check if we opened the application because a GitHub link was clicked
        final Intent intent = getActivity().getIntent();
        final Uri data = intent.getData();
        if (data != null) {
            // Try to retrieve information from the special URL, i.e.
            // https://lonamiwebs.github.io/stringlate/translate?git=<encoded git url>
            final String paramGit = data.getQueryParameter("git");
            final String paramWeb = data.getQueryParameter("web");
            final String paramName = data.getQueryParameter("name");

            if (paramGit == null) {
                // If no URL was found, it may just be a git repository by itself
                String scheme = data.getScheme();
                String fullPath = data.getEncodedSchemeSpecificPart();
                setUrl(scheme + ":" + fullPath);
            } else {
                // We got the encoded git url, so set it
                setUrl(paramGit);
            }
            if (paramWeb != null) {
                mProjectDetails.setWebUrl(paramWeb);
            }
            if (paramName != null) {
                mProjectDetails.setName(paramName);
            }
        }
        // Or we were opened from the StringlateApi
        if (intent.getAction().equals(StringlateApi.ACTION_TRANSLATE) &&
                intent.hasExtra(StringlateApi.EXTRA_GIT_URL)) {
            setUrl(intent.getStringExtra(StringlateApi.EXTRA_GIT_URL));
            mProjectDetails.setWebUrl(intent.getStringExtra(StringlateApi.EXTRA_PROJECT_HOMEPAGE));
            mProjectDetails.setName(intent.getStringExtra(StringlateApi.EXTRA_PROJECT_NAME));
        }

        // If the user presses enter on an EditText, select the next one
        mOwnerEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int kc, KeyEvent e) {
                if (e.getAction() == KeyEvent.ACTION_DOWN && kc == KeyEvent.KEYCODE_ENTER) {
                    mRepositoryEditText.requestFocus();
                    return true;
                }
                return false;
            }
        });

        // Or, if we're on the repository EditText, hide the keyboard
        mRepositoryEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int kc, KeyEvent e) {
                if (e.getAction() == KeyEvent.ACTION_DOWN && kc == KeyEvent.KEYCODE_ENTER) {
                    InputMethodManager imm = (InputMethodManager)
                            getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                    imm.hideSoftInputFromWindow(mRepositoryEditText.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        return rootView;
    }

    //endregion

    //region UI events

    private final View.OnClickListener onDiscoverClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivityForResult(new Intent(getContext(),
                    DiscoverActivity.class), RESULT_REPO_DISCOVERED);
        }
    };

    private final View.OnClickListener onNextClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String owner, repository;
            String url;

            url = mUrlEditText.getText().toString().trim();
            owner = mOwnerEditText.getText().toString().trim();
            repository = mRepositoryEditText.getText().toString().trim();

            if (url.isEmpty() && (owner.isEmpty() || repository.isEmpty())) {
                Toast.makeText(getContext(), R.string.repo_or_url_required,
                        Toast.LENGTH_SHORT).show();
            } else {
                // Determine whether we already have this repo or if it's a new one
                RepoHandler repo = url.isEmpty() ?
                        new RepoHandler(getContext(), GitWrapper.buildGitHubUrl(owner, repository)) :
                        new RepoHandler(getContext(), GitWrapper.getGitUri(url));

                /* TODO Set repo details if available
                if (!TextUtils.isEmpty(mProjectDetails.getWebUrl())) {
                    repo.getRepoSettings().setProjectHomepageUrl(mProjectDetails.getWebUrl());
                }
                if (!TextUtils.isEmpty(mProjectDetails.getName())) {
                    repo.getRepoSettings().setProjectName(mProjectDetails.getName());
                }
                */

                if (repo.isEmpty()) {
                    scanDownloadStrings(repo);
                } else {
                    launchTranslateActivity(repo);
                }
            }
        }
    };

    //endregion

    //region Activity results

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_REPO_DISCOVERED:
                    setUrl(data.getStringExtra("url"));
                    mProjectDetails.setWebUrl(data.getStringExtra("web"));
                    mProjectDetails.setName(data.getStringExtra("name"));
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //endregion

    //region Checking and adding a new local "repository"

    private void scanDownloadStrings(final RepoHandler repo) {
        if (Utils.isNotConnected(getContext(), true))
            return;

        final ProgressDialog progress = ProgressDialog.show(getContext(), "…", "…", true);
        // TODO Don't assume GitSource
        repo.syncResources(new GitSource(repo.getSource(), ""), new GitCloneProgressCallback(getActivity()) {
            @Override
            public void onProgressUpdate(final String title, final String description) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Important: Do not create a runnable for every progress update
                        progress.setTitle(title);
                        progress.setMessage(description);
                    }
                });
            }

            @Override
            public void onProgressFinished(String description, boolean status) {
                progress.dismiss();
                if (description != null)
                    Toast.makeText(getContext(), description, Toast.LENGTH_SHORT).show();
                if (status)
                    launchTranslateActivity(repo);
            }
        });
    }

    //endregion

    //region Utilities

    // Sets the URL EditText, clearing any value on the owner and repo fields
    private void setUrl(String url) {
        mOwnerEditText.setText("");
        mRepositoryEditText.setText("");
        mUrlEditText.setText(url);
    }

    private void launchTranslateActivity(RepoHandler repo) {
        Intent intent = new Intent(getContext(), TranslateActivity.class);
        intent.putExtra(EXTRA_REPO, repo.toBundle());
        startActivity(intent);
    }

    //endregion
}

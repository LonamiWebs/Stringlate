package io.github.lonamiwebs.stringlate.activities.repositories;

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

import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.DiscoverActivity;
import io.github.lonamiwebs.stringlate.activities.translate.TranslateActivity;
import io.github.lonamiwebs.stringlate.classes.RepoSyncTask;
import io.github.lonamiwebs.stringlate.classes.applications.ApplicationDetails;
import io.github.lonamiwebs.stringlate.classes.git.GitHub;
import io.github.lonamiwebs.stringlate.classes.git.GitWrapper;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.classes.sources.GitSource;
import io.github.lonamiwebs.stringlate.utilities.ContextUtils;
import io.github.lonamiwebs.stringlate.utilities.RepoHandlerHelper;
import io.github.lonamiwebs.stringlate.utilities.StringlateApi;

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
        ButterKnife.bind(this, rootView);

        mOwnerEditText = rootView.findViewById(R.id.github_ownerEditText);
        mRepositoryEditText = rootView.findViewById(R.id.github_repositoryEditText);

        mUrlEditText = rootView.findViewById(R.id.urlEditText);
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
            final String paramMail = data.getQueryParameter("mail");

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
                mProjectDetails.setProjectWebUrl(paramWeb);
            }
            if (paramName != null) {
                mProjectDetails.setProjectName(paramName);
            }
            if (paramMail != null) {
                mProjectDetails.setProjectMail(paramMail);
            }
        }
        // Or we were opened from the StringlateApi
        if (intent.getAction().equals(StringlateApi.ACTION_TRANSLATE) &&
                intent.hasExtra(StringlateApi.EXTRA_GIT_URL)) {
            setUrl(intent.getStringExtra(StringlateApi.EXTRA_GIT_URL));
            mProjectDetails.setProjectWebUrl(intent.getStringExtra(StringlateApi.EXTRA_PROJECT_WEB));
            mProjectDetails.setProjectMail(intent.getStringExtra(StringlateApi.EXTRA_PROJECT_MAIL));
            mProjectDetails.setProjectName(intent.getStringExtra(StringlateApi.EXTRA_PROJECT_NAME));
        }

        // If the user presses enter on an EditText, select the next one
        mOwnerEditText.setOnKeyListener((v, kc, e) -> {
            if (e.getAction() == KeyEvent.ACTION_DOWN && kc == KeyEvent.KEYCODE_ENTER) {
                mRepositoryEditText.requestFocus();
                return true;
            }
            return false;
        });

        // Or, if we're on the repository EditText, hide the keyboard
        mRepositoryEditText.setOnKeyListener((v, kc, e) -> {
            if (e.getAction() == KeyEvent.ACTION_DOWN && kc == KeyEvent.KEYCODE_ENTER) {
                InputMethodManager imm = (InputMethodManager)
                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                imm.hideSoftInputFromWindow(mRepositoryEditText.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        return rootView;
    }

    //endregion

    //region UI events

    @OnTextChanged(callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED, value =
            {R.id.github_ownerEditText, R.id.github_repositoryEditText})
    public void onGitHubRepoEditChanged(CharSequence newText) {
        final String owner = mOwnerEditText.getText().toString().trim();
        final String repository = mRepositoryEditText.getText().toString().trim();
        if (!owner.isEmpty() || !repository.isEmpty()) {
            mUrlEditText.setText(GitHub.buildGitHubUrl(owner, repository));
        } else {
            mUrlEditText.setText("");
        }
    }

    private final View.OnClickListener onDiscoverClick = view -> startActivityForResult(new Intent(getContext(),
            DiscoverActivity.class), RESULT_REPO_DISCOVERED);

    private final View.OnClickListener onNextClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String url;

            url = mUrlEditText.getText().toString().trim();

            if (url.isEmpty()) {
                Toast.makeText(getContext(), R.string.repo_or_url_required,
                        Toast.LENGTH_SHORT).show();
            } else {
                // Determine whether we already have this repo or if it's a new one
                RepoHandler repo = RepoHandlerHelper.fromContext(getContext(), GitWrapper.getGitUri(url));

                if (!TextUtils.isEmpty(mProjectDetails.getProjectWebUrl())) {
                    repo.settings.setProjectWebUrl(mProjectDetails.getProjectWebUrl());
                }
                if (!TextUtils.isEmpty(mProjectDetails.getProjectName())) {
                    repo.settings.setProjectName(mProjectDetails.getProjectName());
                }
                if (!TextUtils.isEmpty(mProjectDetails.getProjectMail())) {
                    repo.settings.setProjectMail(mProjectDetails.getProjectMail());
                }

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
                    mProjectDetails.setProjectWebUrl(data.getStringExtra("web"));
                    mProjectDetails.setProjectName(data.getStringExtra("name"));
                    mProjectDetails.setProjectMail(data.getStringExtra("mail"));
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
        if (new ContextUtils(getContext()).isConnectedToInternet(R.string.no_internet_connection))
            return;

        new RepoSyncTask(getContext(), repo,
                new GitSource(repo.settings.getSource(), "HEAD"), true).start();

        if (getActivity() instanceof RepositoriesActivity) {
            // Take the user to the repositories history if the parent activity matches
            ((RepositoriesActivity) getActivity()).goToHistory();
        }
    }

    //endregion

    //region Utilities

    // Sets the URL EditText, clearing any value on the owner and repo fields
    private void setUrl(final String url) {
        mOwnerEditText.setText("");
        mRepositoryEditText.setText("");
        mUrlEditText.setText(url);
    }

    private void launchTranslateActivity(final RepoHandler repo) {
        Intent intent = new Intent(getContext(), TranslateActivity.class);
        intent.putExtra(EXTRA_REPO, RepoHandlerHelper.toBundle(repo));
        startActivity(intent);
    }

    //endregion
}

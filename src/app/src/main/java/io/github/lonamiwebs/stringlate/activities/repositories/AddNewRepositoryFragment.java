package io.github.lonamiwebs.stringlate.activities.repositories;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.DiscoverActivity;
import io.github.lonamiwebs.stringlate.activities.translate.TranslateActivity;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.git.GitHub;
import io.github.lonamiwebs.stringlate.utilities.RepoHandler;

import static android.app.Activity.RESULT_OK;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_REPO_DISCOVERED;

public class AddNewRepositoryFragment extends Fragment {

    //region Members

    private AutoCompleteTextView mOwnerEditText, mRepositoryEditText;
    private AutoCompleteTextView mUrlEditText;

    //endregion

    //region Initialization

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_new_repository, container, false);

        mOwnerEditText = (AutoCompleteTextView)rootView.findViewById(R.id.ownerEditText);
        mRepositoryEditText = (AutoCompleteTextView)rootView.findViewById(R.id.repositoryEditText);

        mUrlEditText = (AutoCompleteTextView)rootView.findViewById(R.id.urlEditText);

        // Set button events
        rootView.findViewById(R.id.discoverButton).setOnClickListener(onDiscoverClick);
        rootView.findViewById(R.id.nextButton).setOnClickListener(onNextClick);

        // Check if we opened the application because a GitHub link was clicked
        Uri data = getActivity().getIntent().getData();
        if (data != null) {
            String scheme = data.getScheme();
            String fullPath = data.getEncodedSchemeSpecificPart();
            setUrl(scheme+":"+fullPath);
        }

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
                        new RepoHandler(getContext(), owner, repository) :
                        new RepoHandler(getContext(), url);

                if (repo.isEmpty())
                    scanDownloadStrings(repo);
                else
                    launchTranslateActivity(repo);
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
        if (!GitHub.gCanCall()) {
            Toast.makeText(getContext(),
                    R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progress = ProgressDialog.show(getContext(), "…", "…", true);
        repo.syncResources(new ProgressUpdateCallback() {
            @Override
            public void onProgressUpdate(String title, String description) {
                progress.setTitle(title);
                progress.setMessage(description);
            }

            @Override
            public void onProgressFinished(String description, boolean status) {
                progress.dismiss();
                if (description != null)
                    Toast.makeText(getContext(), description, Toast.LENGTH_SHORT).show();
                if (status)
                    launchTranslateActivity(repo);
            }
        }, false);
        // false, do not keep any previous modification (there should not be any)
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

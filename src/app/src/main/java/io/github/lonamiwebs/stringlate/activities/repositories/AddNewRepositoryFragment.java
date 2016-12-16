package io.github.lonamiwebs.stringlate.activities.repositories;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.DiscoverActivity;
import io.github.lonamiwebs.stringlate.activities.TranslateActivity;
import io.github.lonamiwebs.stringlate.interfaces.Callback;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.utilities.GitHub;
import io.github.lonamiwebs.stringlate.utilities.RepoHandler;

import static android.app.Activity.RESULT_OK;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO_NAME;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO_OWNER;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_REPO_DISCOVERED;

public class AddNewRepositoryFragment extends Fragment {

    //region Members

    private AutoCompleteTextView mOwnerEditText, mRepositoryEditText;
    private AutoCompleteTextView mUrlEditText;

    private Pattern mOwnerProjectPattern; // Match user and repository name from a GitHub url

    //endregion

    //region Initialization

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_new_repository, container, false);

        mOwnerEditText = (AutoCompleteTextView)rootView.findViewById(R.id.ownerEditText);
        mRepositoryEditText = (AutoCompleteTextView)rootView.findViewById(R.id.repositoryEditText);
        mOwnerEditText.addTextChangedListener(onOwnerChanged);

        mUrlEditText = (AutoCompleteTextView)rootView.findViewById(R.id.urlEditText);

        mOwnerProjectPattern = Pattern.compile(
                "(?:https?://github\\.com/|git@github.com:)([\\w-]+)/([\\w-]+)(?:/|\\.git)?");

        // Set button events
        rootView.findViewById(R.id.discoverButton).setOnClickListener(onDiscoverClick);
        rootView.findViewById(R.id.nextButton).setOnClickListener(onNextClick);

        // Check if we opened the application because a GitHub link was clicked
        Uri data = getActivity().getIntent().getData();
        if (data != null) {
            String scheme = data.getScheme();
            String fullPath = data.getEncodedSchemeSpecificPart();
            mUrlEditText.setText(scheme+":"+fullPath);
        }

        changeListener.onRepositoryCountChanged();

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RepoHandler.addChangeListener(changeListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RepoHandler.removeChangeListener(changeListener);
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
                Toast.makeText(getContext(), R.string.repo_or_url_required,
                        Toast.LENGTH_SHORT).show();
            } else {
                // Determine whether we already have this repo or if it's a new one
                if (new RepoHandler(getContext(), owner, repository).isEmpty())
                    checkRepositoryOK(owner, repository);
                else
                    launchTranslateActivity(owner, repository);
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
                    mUrlEditText.setText(data.getStringExtra("url"));
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //endregion

    //region Listeners

    RepoHandler.ChangeListener changeListener = new RepoHandler.ChangeListener() {
        @Override
        public void onRepositoryCountChanged() {
            loadOwnerAutocomplete();
            loadUrlAutocomplete();
        }
    };

    //endregion

    //region Checking and adding a new local "repository"

    // Step 1
    private void checkRepositoryOK(final String owner, final String repository) {
        final ProgressDialog progress = ProgressDialog.show(getContext(),
                getString(R.string.checking_repo_ok),
                getString(R.string.checking_repo_ok_long), true);

        if (!GitHub.gCanCall()) {
            Toast.makeText(getContext(),
                    R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            return;
        }
        GitHub.gCheckOwnerRepoOK(owner, repository, new Callback<Boolean>() {
            @Override
            public void onCallback(Boolean ok) {
                if (ok) {
                    scanDownloadStrings(owner, repository, progress);
                } else {
                    Toast.makeText(getContext(),
                            R.string.invalid_repo, Toast.LENGTH_SHORT).show();
                    progress.dismiss();
                }
            }
        });
    }

    // Steps 2 a 3
    private void scanDownloadStrings(final String owner, final String repository,
                                     final ProgressDialog progress) {
        RepoHandler handler = new RepoHandler(getContext(), owner, repository);
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
                    Toast.makeText(getContext(), description, Toast.LENGTH_SHORT).show();
                if (status)
                    launchTranslateActivity(owner, repository);
            }
        }, true);
        // true, overwrite any previous file (there won't be any)
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, RepoHandler.listOwners(getContext()));

        mOwnerEditText.setAdapter(adapter);
    }

    private void loadRepositoryAutocomplete() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line,
                RepoHandler.listRepositories(getContext(), mOwnerEditText.getText().toString()));

        mRepositoryEditText.setAdapter(adapter);
    }

    private void loadUrlAutocomplete() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line,
                RepoHandler.listRepositories(getContext(), true));

        mUrlEditText.setAdapter(adapter);
    }

    //endregion

    //region Utilities

    private void launchTranslateActivity(String owner, String repository) {
        Intent intent = new Intent(getContext(), TranslateActivity.class);
        intent.putExtra(EXTRA_REPO_OWNER, owner);
        intent.putExtra(EXTRA_REPO_NAME, repository);
        startActivity(intent);
    }

    //endregion
}

package io.github.lonamiwebs.stringlate.activities.repositories;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.translate.TranslateActivity;
import io.github.lonamiwebs.stringlate.utilities.RepoHandler;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;

public class HistoryFragment extends Fragment {

    //region Members

    private ListView mRepositoryListView;
    private TextView mHistoryMessageTextView;
    private TextView mRepositoriesTitle;

    //endregion

    //region Initialization

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history, container, false);

        mRepositoryListView = (ListView)rootView.findViewById(R.id.repositoryListView);
        mRepositoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String[] ownerRepo = ((String)adapterView.getItemAtPosition(i)).split("/");
                launchTranslateActivity(new RepoHandler(getContext(), ownerRepo[0], ownerRepo[1]));
            }
        });

        mHistoryMessageTextView = (TextView)rootView.findViewById(R.id.historyMessageTextView);
        mRepositoriesTitle = (TextView)rootView.findViewById(R.id.repositoriesTitle);

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

    //region Listeners

    RepoHandler.ChangeListener changeListener = new RepoHandler.ChangeListener() {
        @Override
        public void onRepositoryCountChanged() {
            ArrayList<String> repositories = RepoHandler.listRepositories(getContext(), false);
            Collections.sort(repositories);

            if (repositories.isEmpty()) {
                mRepositoriesTitle.setVisibility(GONE);
                mHistoryMessageTextView.setText(getString(
                        R.string.history_no_repos_hint, getString(R.string.add_new_repo)));
                mRepositoryListView.setAdapter(null);
            } else {
                mRepositoriesTitle.setVisibility(VISIBLE);
                mHistoryMessageTextView.setText(R.string.history_contains_repos_hint);
                mRepositoryListView.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_list_item_1, repositories));
            }
        }
    };

    //endregion

    //region Utilities

    private void launchTranslateActivity(RepoHandler repo) {
        Intent intent = new Intent(getContext(), TranslateActivity.class);
        intent.putExtra(EXTRA_REPO, repo.toBundle());
        startActivity(intent);
    }

    //endregion
}

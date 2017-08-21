package io.github.lonamiwebs.stringlate.activities.repositories;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Collections;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.translate.TranslateActivity;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandlerAdapter;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_CREATE_FILE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_OPEN_FILE;

public class HistoryFragment extends Fragment {

    //region Members

    private ListView mRepositoryListView;
    private TextView mHistoryMessageTextView;
    private TextView mRepositoriesTitle;

    // Not the best solution. How else could extra data by passed to activity results?
    private RepoHandler mLastSelectedRepo;

    //endregion

    //region Initialization

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history, container, false);

        mRepositoryListView = rootView.findViewById(R.id.repositoryListView);
        mRepositoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RepoHandler repo = (RepoHandler) adapterView.getItemAtPosition(i);
                TranslateActivity.launch(getContext(), repo);
            }
        });
        registerForContextMenu(mRepositoryListView);

        mHistoryMessageTextView = rootView.findViewById(R.id.historyMessageTextView);
        mRepositoriesTitle = rootView.findViewById(R.id.repositoriesTitle);

        changeListener.onRepositoryCountChanged();

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RepoHandler.addChangeListener(changeListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Assume the count changed every time we're back on this fragment.
        // This is because, although translating strings doesn't change the
        // repository count, it does affect the progress bar.
        changeListener.onRepositoryCountChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RepoHandler.removeChangeListener(changeListener);
    }

    //endregion

    //region Menu

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.repositoryListView) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.menu_history, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        mLastSelectedRepo = (RepoHandler) mRepositoryListView.getItemAtPosition(info.position);
        switch (item.getItemId()) {
            case R.id.importRepo:
                importFromSd();
                return true;
            case R.id.exportRepo:
                exportToSd();
                return true;
            case R.id.deleteRepo:
                promptDeleteRepo(mLastSelectedRepo);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void promptDeleteRepo(final RepoHandler repo) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.sure_question)
                .setMessage(getString(R.string.delete_repository_confirm_long, repo.toString()))
                .setPositiveButton(getString(R.string.delete_repository), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        repo.delete();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    //endregion

    //region Importing and exporting

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_CREATE_FILE:
                    doExportToSd(data.getData());
                    break;
                case RESULT_OPEN_FILE:
                    doImportFromSd(data.getData());
                    break;
            }
        }
    }

    // Ripped off from TranslateActivity.java
    // The following 3 methods make use of mLastSelectedRepo - will fail if it is null
    private void exportToSd() {
        String filename = mLastSelectedRepo.getProjectName() + ".zip";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            startActivityForResult(intent, RESULT_CREATE_FILE);
        } else {
            File output = new File(getCreateExportRoot(), filename);
            doExportToSd(Uri.fromFile(output));
        }
    }

    private File getCreateExportRoot() {
        String path;
        try {
            path = getString(R.string.app_name) + "/" + mLastSelectedRepo.toOwnerRepo();
        } catch (InvalidObjectException ignored) {
            path = getString(R.string.app_name);
        }
        File root = new File(Environment.getExternalStorageDirectory(), path);
        if (root.isDirectory())
            root.mkdirs();
        return root;
    }

    private void doExportToSd(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor());
            mLastSelectedRepo.exportZip(out);
            out.close();
            pfd.close();
            Toast.makeText(getContext(), getString(R.string.export_file_success, uri.getPath()),
                    Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), R.string.export_file_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void importFromSd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            startActivityForResult(intent, RESULT_OPEN_FILE);
        } else {
            // Not sure this will work.
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, RESULT_OPEN_FILE);
        }
    }

    private void doImportFromSd(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(uri, "r");
            FileInputStream in = new FileInputStream(pfd.getFileDescriptor());
            mLastSelectedRepo.importZip(in);
            in.close();
            pfd.close();
            Toast.makeText(getContext(), R.string.import_file_success, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), R.string.import_file_failed, Toast.LENGTH_SHORT).show();
        }
    }

    //endregion

    //region Listeners

    private final RepoHandler.ChangeListener changeListener = new RepoHandler.ChangeListener() {
        @Override
        public void onRepositoryCountChanged() {
            ArrayList<RepoHandler> repositories = RepoHandler.listRepositories(getContext());
            Collections.sort(repositories);

            if (repositories.isEmpty()) {
                mRepositoriesTitle.setVisibility(GONE);
                mHistoryMessageTextView.setText(getString(
                        R.string.history_no_repos_hint, getString(R.string.add_project)));
                mRepositoryListView.setAdapter(null);
            } else {
                mRepositoriesTitle.setVisibility(VISIBLE);
                mHistoryMessageTextView.setText(R.string.history_contains_repos_hint);
                mRepositoryListView.setAdapter(
                        new RepoHandlerAdapter(getContext(), repositories));
            }
        }
    };

    //endregion
}

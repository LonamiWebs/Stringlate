package io.github.lonamiwebs.stringlate.activities.translate;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.export.CreateGistActivity;
import io.github.lonamiwebs.stringlate.activities.export.CreateIssueActivity;
import io.github.lonamiwebs.stringlate.activities.export.CreatePullRequestActivity;
import io.github.lonamiwebs.stringlate.classes.LocaleString;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;
import io.github.lonamiwebs.stringlate.git.GitCloneProgressCallback;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.Utils;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_CREATE_FILE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_OPEN_TREE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_STRING_SELECTED;

public class TranslateActivity extends AppCompatActivity {

    //region Members

    private AppSettings mSettings;

    private EditText mOriginalStringEditText;
    private EditText mTranslatedStringEditText;

    private Spinner mLocaleSpinner;
    private Spinner mStringIdSpinner;

    private Button mPreviousButton;
    private Button mNextButton;
    private ProgressBar mProgressProgressBar;
    private TextView mProgressTextView;

    private LinearLayout mFilterAppliedLayout;
    private TextView mFilterAppliedTextView;

    private String mSelectedLocale;
    private ResTag mSelectedResource;
    private boolean mShowTranslated;
    private MenuItem mShowTranslatedMenuItem;

    private Resources mDefaultResources;
    private Resources mSelectedLocaleResources;

    private RepoHandler mRepo;

    private boolean loaded;

    //endregion

    //region Initialization

    public static void launch(final Context ctx, final RepoHandler repo) {
        Intent intent = new Intent(ctx, TranslateActivity.class);
        intent.putExtra(EXTRA_REPO, repo.toBundle());
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loaded = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        mSettings = new AppSettings(this);

        mOriginalStringEditText = (EditText)findViewById(R.id.originalStringEditText);
        mTranslatedStringEditText = (EditText)findViewById(R.id.translatedStringEditText);
        mTranslatedStringEditText.addTextChangedListener(onTranslationChanged);

        mLocaleSpinner = (Spinner)findViewById(R.id.localeSpinner);
        mStringIdSpinner = (Spinner)findViewById(R.id.stringIdSpinner);

        mPreviousButton = (Button)findViewById(R.id.previousButton);
        mNextButton = (Button)findViewById(R.id.nextButton);
        mProgressProgressBar = (ProgressBar)findViewById(R.id.progressProgressBar);
        mProgressTextView = (TextView)findViewById(R.id.progressTextView);

        mFilterAppliedLayout = (LinearLayout)findViewById(R.id.filterAppliedLayout);
        mFilterAppliedTextView = (TextView)findViewById(R.id.filterAppliedTextView);

        mLocaleSpinner.setOnItemSelectedListener(eOnLocaleSelected);
        mStringIdSpinner.setOnItemSelectedListener(eOnStringIdSelected);

        mRepo = RepoHandler.fromBundle(this, getIntent().getBundleExtra(EXTRA_REPO));
        setTitle(mRepo.getName(false));

        loadResources();
        onFilterUpdated(mRepo.getStringFilter());

        // loadStringIDsSpinner is called too often at startup, use this flag to avoid it
        loaded = true;
        loadStringIDsSpinner();
    }

    private void loadResources() {
        if (mRepo.hasDefaultLocale()) {
            mDefaultResources = mRepo.loadDefaultResources();
            // TODO Better way to handle the filters? But mRepo might need it unfiltered internally
            mDefaultResources.setFilter(mRepo.getStringFilter());

            loadLocalesSpinner();
            checkTranslationVisibility();
        } else {
            // This should never happen since it's checked when creating the repository
            Toast.makeText(this, R.string.no_strings_found_update,
                    Toast.LENGTH_LONG).show();
        }
    }

    //endregion

    //region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_translate, menu);

        // Only GitHub repositories are valid ones when exporting to issue or pull request
        boolean isGitHubRepository = mRepo.isGitHubRepository();
        menu.findItem(R.id.exportToIssue).setVisible(isGitHubRepository);
        menu.findItem(R.id.exportToPr).setVisible(isGitHubRepository);

        mShowTranslatedMenuItem = menu.findItem(R.id.showTranslatedCheckBox);
        mShowTranslated = mShowTranslatedMenuItem.isChecked();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // http://stackoverflow.com/a/22288914/4759433
        if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
            try {
                final Method m = menu.getClass().getDeclaredMethod(
                        "setOptionalIconsVisible", Boolean.TYPE);
                m.setAccessible(true);
                m.invoke(menu, true);
            }
            catch (Exception e) {
                Log.w("TranslateActivity", "Could not make the menu icons visible.", e);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Synchronizing repository
            case R.id.updateStrings:
                askUpdateStrings();
                return true;

            // Search strings
            case R.id.searchStrings:
                launchStringSearchActivity();
                return true;

            // Adding locales
            case R.id.addLocale:
                promptAddLocale();
                return true;

            // Sorting modes
            case R.id.sortAlphabetically:
                mSettings.setStringSortMode(AppSettings.SORT_ALPHABETICALLY);
                loadStringIDsSpinner();
                return true;

            case R.id.sortLength:
                mSettings.setStringSortMode(AppSettings.SORT_STRING_LENGTH);
                loadStringIDsSpinner();
                return true;

            // Exporting resources
            case R.id.exportToSdcard:
            case R.id.exportToGist:
            case R.id.exportToIssue:
            case R.id.exportToPr:
            case R.id.exportShare:
            case R.id.exportCopy:
                // Since all the exports check these conditions, check them here only
                if (!isLocaleSelected(true))
                    return true;

                if (mSelectedLocaleResources.isEmpty()) {
                    Toast.makeText(this, R.string.no_strings_to_export, Toast.LENGTH_SHORT).show();
                    return true;
                }

                // Perform the requested export
                switch (item.getItemId()) {
                    case R.id.exportToSdcard: exportToSd(); break;
                    case R.id.exportToGist: exportToGist(); break;
                    case R.id.exportToIssue: exportToIssue(); break;
                    case R.id.exportToPr: exportToPullRequest(); break;
                    case R.id.exportShare: exportToShare(); break;
                    case R.id.exportCopy: exportToCopy(); break;
                }
                return true;

            // Deleting resources
            case R.id.deleteString:
                deleteString();
                return true;
            case R.id.deleteLocale:
                promptDeleteLocale();
                return true;
            case R.id.deleteRepo:
                promptDeleteRepo();
                return true;

            // Toggling visibility
            case R.id.showTranslatedCheckBox:
                toggleShowTranslated(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //endregion

    //region UI events

    //region Activity events

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_CREATE_FILE:
                    doExportToSd(data.getData());
                    break;
                case RESULT_STRING_SELECTED:
                    onFilterUpdated(data.getStringExtra("filter"));
                    setStringId(data.getStringExtra("id"));
                    break;
                case RESULT_OPEN_TREE:
                    doExportManyToSd(data.getData());
                    break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    //endregion

    //region Menu events

    //region Repository synchronizing menu events

    // Synchronize our local strings.xml files with the remote GitHub
    // repository, previously saving the strings.xml and asking whether
    // files should be overwritten after synchronizing (if any change was made)
    private void askUpdateStrings() {
        save();
        if (mRepo.anyModified()) {
            // Do not mistake unsaved changes (modifications, .isSaved())
            // with the file being ever modified (.wasModified())
            new AlertDialog.Builder(this)
                    .setTitle(R.string.pulling_strings)
                    .setMessage(R.string.pulling_strings_long)
                    .setPositiveButton(R.string.pulling_strings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            updateStrings();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            // No file has been modified, simply update the strings discarding changes
            updateStrings();
        }
    }

    // Synchronize our local strings.xml files with the remote GitHub repository
    private void updateStrings() {
        if (Utils.isNotConnected(this, true))
            return;

        final ProgressDialog progress = ProgressDialog.show(this,
                getString(R.string.loading_ellipsis), null, true);

        mRepo.syncResources(new GitCloneProgressCallback(this) {
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

                loadResources();
            }
        });
    }

    //endregion

    //region Searching for strings

    private void launchStringSearchActivity() {
        if (isLocaleSelected(true)) {
            Intent intent = new Intent(this, SearchStringActivity.class);
            intent.putExtra(EXTRA_REPO, mRepo.toBundle());
            intent.putExtra(EXTRA_LOCALE, mSelectedLocale);
            startActivityForResult(intent, RESULT_STRING_SELECTED);
        }
    }

    //endregion

    //region Adding locales menu events

    private void promptAddLocale() {
        promptAddLocale("");
    }

    // Prompts the user to add a new locale. If it exists,
    // no new file is created but the entered locale is selected.
    private void promptAddLocale(final String presetLocaleCode) {

        final LocaleListBuilder localeList = new LocaleListBuilder(this,
                new LocaleListBuilder.LocalePickCallback(){
                    @Override
                    public void onChoice(final String localeCode) {
                        promptAddLocale(localeCode);
                    }
                });

        final EditText et = new EditText(this);
        et.setText(presetLocaleCode);
        new AlertDialog.Builder(this)
                .setTitle(R.string.enter_locale)
                .setMessage(getString(R.string.enter_locale_long, Locale.getDefault().getLanguage()))
                .setView(et)
                .setNeutralButton(R.string.choose_locale_from_list, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        localeList.show();
                    }
                })
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String locale = LocaleString.sanitizeLocale(et.getText().toString());
                        if (LocaleString.isValid(locale)) {
                            if (mRepo.createLocale(locale)) {
                                loadLocalesSpinner();
                                setCurrentLocale(locale);
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        R.string.create_locale_error,
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // The input locale is not a valid locale
                            Toast.makeText(getApplicationContext(),
                                    R.string.invalid_locale,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    //endregion

    //region Exporting menu events

    // NOTE THAT NEITHER OF THESE METHODS CHECK WHETHER A LOCALE IS SELECTED OR NOT
    // They all also assume that at least there is *one* string for any template

    // Exports the currently selected locale resources to the SD card
    private void exportToSd() {
        File[] files = mRepo.getDefaultResourcesFiles();
        if (files.length == 1) {
            // Export a single file
            String filename = files[0].getName();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                intent.setType("text/xml");
                intent.putExtra(Intent.EXTRA_TITLE, filename);
                startActivityForResult(intent, RESULT_CREATE_FILE);
            } else {
                File output = new File(getCreateExportRoot(), files[0].getName());
                doExportToSd(Uri.fromFile(output));
            }
        } else {
            // Export multiple files
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // I don't know why "document tree" doesn't work with ≥ KitKat
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, RESULT_OPEN_TREE);
            } else {
                File root = getCreateExportRoot();
                doExportManyToSd(Uri.fromFile(root));
            }
        }
    }

    private File getCreateExportRoot() {
        String path;
        try {
            path = getString(R.string.app_name) + "/" + mRepo.toOwnerRepo();
        } catch (InvalidObjectException ignored) {
            path = getString(R.string.app_name);
        }
        File root = new File(Environment.getExternalStorageDirectory(), path);
        if (root.isDirectory())
            root.mkdirs();
        return root;
    }

    // This method will only work if there is one template
    private void doExportToSd(Uri uri) {
        try {
            doExportToSd(uri, mRepo.getDefaultResourcesFiles()[0]);
            Toast.makeText(this, getString(R.string.export_file_success, uri.getPath()),
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.export_file_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void doExportManyToSd(Uri uri) {
        boolean ok = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // DocumentFile.fromTreeUri:
            //   "This is only useful on devices running LOLLIPOP or later,
            //    and will return null when called on earlier platform versions."
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, uri);
            try {
                for (File template : mRepo.getDefaultResourcesFiles()) {
                    // Do not bother creating a file unless there is some strings for it
                    if (mRepo.canApplyTemplate(template, mSelectedLocale)) {
                        DocumentFile outFile = pickedDir.createFile("text/xml", template.getName());
                        doExportToSd(outFile.getUri(), template);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                ok = false;
            }
        } else {
            try {
                File root = new File(uri.getPath());
                if (!root.isDirectory() && !root.mkdirs())
                    throw new IOException("Could not create the root directory.");

                for (File template : mRepo.getDefaultResourcesFiles()) {
                    // Do not bother creating a file unless there is some strings for it
                    if (mRepo.canApplyTemplate(template, mSelectedLocale)) {
                        File outFile = new File(root, template.getName());
                        doExportToSd(Uri.fromFile(outFile), template);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                ok = false;
            }
        }
        if (ok) {
            Toast.makeText(this, getString(R.string.export_file_success, uri.getPath()),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.export_file_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void doExportToSd(Uri uri, File template)
            throws IOException {

        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
        FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor());
        if (!mRepo.applyTemplate(template, mSelectedLocale, out))
            throw new IOException("Apply default template failed.");

        out.close();
        pfd.close();
    }

    // Exports the currently selected locale resources to a GitHub Gist
    private void exportToGist() {
        Intent intent = new Intent(this, CreateGistActivity.class);
        intent.putExtra(EXTRA_REPO, mRepo.toBundle());
        intent.putExtra(EXTRA_LOCALE, mSelectedLocale);
        startActivity(intent);
    }

    // Exports the currently selected locale resources to a GitHub issue
    private void exportToIssue() {
        if (!new AppSettings(this).hasGitHubAuthorization()) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, CreateIssueActivity.class);
        intent.putExtra(EXTRA_REPO, mRepo.toBundle());
        intent.putExtra(EXTRA_LOCALE, mSelectedLocale);
        startActivity(intent);
    }

    // Exports the currently selected locale resources to a GitHub Pull Request
    private void exportToPullRequest() {
        if (!mRepo.hasRemoteUrls()) {
            // TODO Remove this check by version 1.0 or so? Or can we be really missing the urls somehow?
            Toast.makeText(this, R.string.sync_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!new AppSettings(this).hasGitHubAuthorization()) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_LONG).show();
            return;
        }
        if (Utils.isNotConnected(this, true))
            return;

        Intent intent = new Intent(this, CreatePullRequestActivity.class);
        intent.putExtra(EXTRA_REPO, mRepo.toBundle());
        intent.putExtra(EXTRA_LOCALE, mSelectedLocale);
        startActivity(intent);
    }

    // Exports the currently selected locale resources to a plain text share intent
    private void exportToShare() {
        String xml = mRepo.mergeDefaultTemplate(mSelectedLocale);
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, xml);
        startActivity(Intent.createChooser(sharingIntent,
                getString(R.string.export_share)));
    }

    // Exports the currently selected locale resources to the primary clipboard
    private void exportToCopy() {
        String filename = mSelectedLocaleResources.getFilename();
        String xml = mRepo.mergeDefaultTemplate(mSelectedLocale);

        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(filename, xml));
        Toast.makeText(this, getString(R.string.xml_copied_to_clipboard, filename),
                Toast.LENGTH_SHORT).show();
    }

    //endregion

    //region Deleting menu events

    // Deletes the currently selected string ID, this needs no warning
    private void deleteString() {
        if (!isLocaleSelected(true)) return;

        mSelectedLocaleResources.deleteId((String)mStringIdSpinner.getSelectedItem());
        mTranslatedStringEditText.setText("");
    }

    // Prompts the user whether they want to delete the selected locale or not
    // This does need warning since deleting a whole locale is a big deal
    private void promptDeleteLocale() {
        if (mLocaleSpinner.getCount() == 0) {
            Toast.makeText(this, R.string.delete_no_locale_bad_joke, Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.sure_question)
                .setMessage(getString(R.string.delete_locale_confirm_long, mSelectedLocale))
                .setPositiveButton(getString(R.string.delete_locale), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mRepo.deleteLocale(mSelectedLocale);
                        loadLocalesSpinner();
                        checkTranslationVisibility();

                        // We need to clear the selected locale if it's now empty
                        if (mLocaleSpinner.getCount() == 0)
                            setCurrentLocale(null);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    // Prompts the user whether they want to delete the current "repository" clone or not
    // There is no need for me to tell whoever reading this that this does need confirmation
    private void promptDeleteRepo() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.sure_question)
                .setMessage(getString(R.string.delete_repository_confirm_long, mRepo.toString()))
                .setPositiveButton(getString(R.string.delete_repository), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mRepo.delete();
                        finish();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    //endregion

    //region Toggling visibility menu events

    // Toggles the "Show translated strings" checkbox and updates the spinner
    private void toggleShowTranslated(MenuItem item) {
        mShowTranslated = !mShowTranslated;
        if (item != null) {
            item.setChecked(mShowTranslated);
        }
        String lastId = mSelectedResource == null ? null : mSelectedResource.getId();
        loadStringIDsSpinner();

        // Set the last string that was being used
        if (mShowTranslated) {
            setStringId(lastId);
        } else {
            // Not all strings are being shown, so in case we don't
            // find it under non-translatable, select the next available
            // string. If there isn't any, select previous one, or none.
            String id; // Current ResTag id
            String selectId = null; // Id that will be selected at the end
            boolean hasTranslation;
            boolean selectNext = false;
            for (ResTag rs : mDefaultResources) {
                id = rs.getId();
                hasTranslation = mSelectedLocaleResources.contains(rs.getId());

                if (hasTranslation) {
                    // If we do have a translation for the string we want to select
                    // (but these are not shown), then we need to select the next one
                    if (lastId.equals(id)) {
                        selectNext = true;
                    }
                } else {
                    // If we don't have a translation for the string we want to select,
                    // then that's perfect, simply select it and exit the loop
                    //
                    // Or otherwise, if we don't have a translation for it,
                    // selectNext will be true, so we need to select the next string
                    if (lastId.equals(id) || selectNext) {
                        selectId = id;
                        break;
                    } else {
                        // We won't be able to select the next string if there are
                        // no more, this is why we also need to remember the last string
                        selectId = id;
                    }
                }
            }
            if (selectId != null) {
                setStringId(selectId);
            }
        }
    }

    //endregion

    //endregion

    //region Button events

    public void onClearFilterClick(final View v) {
        onFilterUpdated("");
    }

    public void onPreviousClick(final View v) {
        incrementStringIdIndex(-1);
    }

    public void onNextClick(final View v) {
        incrementStringIdIndex(+1);
    }

    private void save() {
        if (isLocaleSelected(false)) {
            if (mSelectedLocaleResources.save()) {
                updateProgress();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.save_error)
                        .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                save();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        }
    }

    //endregion

    //region EditText events

    private final TextWatcher onTranslationChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (mSelectedLocaleResources != null) {
                String content = mTranslatedStringEditText.getText().toString();
                mSelectedLocaleResources.setContent(mSelectedResource, content);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) { }
    };

    //endregion

    //region Spinner events

    private final AdapterView.OnItemSelectedListener
            eOnLocaleSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
            final LocaleString selectedLocale = (LocaleString)parent.getItemAtPosition(i);
            if (isLocaleSelected(false)) {
                save();
                setCurrentLocale(selectedLocale.getCode());
            } else {
                // If it's the first time we're selecting a locale,
                // we don't care unsaved changes (because there isn't any)
                setCurrentLocale(selectedLocale.getCode());
            }
            updateProgress();
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    private final AdapterView.OnItemSelectedListener
            eOnStringIdSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
            updateSelectedResourceId((String)parent.getItemAtPosition(i));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    //endregion

    //endregion

    //region UI

    // Checks whether the translation layout (EditText and previous/next buttons)
    // should be visible (there is at least one non-default locale) or not.
    private void checkTranslationVisibility() {
        if (mLocaleSpinner.getCount() == 0) {
            Toast.makeText(this, R.string.add_locale_to_start, Toast.LENGTH_SHORT).show();
            findViewById(R.id.translationLayout).setVisibility(View.GONE);
        } else {
            findViewById(R.id.translationLayout).setVisibility(View.VISIBLE);
        }
    }

    private void updateProgress() {
        if (mSelectedLocaleResources == null) {
            mProgressProgressBar.setMax(1);
            mProgressProgressBar.setProgress(0);
            mProgressTextView.setText("");
        } else {
            int stringsCount = mDefaultResources.count();
            // Keep track of the translated strings count and the characters of the
            // original strings + those same characters if a translation is available.
            // This will be used to make a weighted progress (if you translated only
            // long strings, then this will be closer to 100% than if you translated small ones).
            int translatedCount = 0;
            int currentChars = 0;
            int totalChars = 0;
            int chars;
            for (ResTag rs : mDefaultResources) {
                chars = rs.getContentLength();
                totalChars += chars;
                if (mSelectedLocaleResources.contains(rs.getId())) {
                    translatedCount++;
                    currentChars += chars;
                }
            }

            // The progress bar will be using the weighted value
            mProgressProgressBar.setMax(totalChars);
            mProgressProgressBar.setProgress(currentChars);
            float percentage = (100.0f * (float)currentChars) / (float)totalChars;

            // The text view will show the string count and the weighted percentage
            mProgressTextView.setText(getString(R.string.translation_progress,
                    translatedCount, stringsCount, percentage));
        }
    }

    private void onFilterUpdated(@NonNull final String filter) {
        // Update the filter, it might have been changed from the Search activity
        // and JSON doesn't load the changes from the file but rather keeps a copy
        mRepo.setStringFilter(filter);

        if (mDefaultResources != null)
            mDefaultResources.setFilter(filter);

        if (mSelectedLocaleResources != null) {
            mSelectedLocaleResources.setFilter(filter);

            String lastId = mSelectedResource == null ? null : mSelectedResource.getId();
            loadStringIDsSpinner();
            setStringId(lastId);
        }

        if (filter.isEmpty()) {
            mFilterAppliedLayout.setVisibility(View.GONE);
        } else {
            mFilterAppliedLayout.setVisibility(View.VISIBLE);
            mFilterAppliedTextView.setText(getString(R.string.filtering_strings_with, filter));
        }
    }

    //endregion

    //region Spinner loading

    private void loadLocalesSpinner() {
        ArrayList<LocaleString> spinnerArray = new ArrayList<>();
        for (String locale : mRepo.getLocales())
            if (!locale.equals(RepoHandler.DEFAULT_LOCALE))
                spinnerArray.add(new LocaleString(locale));

        ArrayAdapter<LocaleString> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLocaleSpinner.setAdapter(adapter);
        setCurrentLocale(mRepo.getLastLocale());
    }

    private void loadStringIDsSpinner() {
        if (!loaded || !isLocaleSelected(false)) return;

        ArrayList<String> spinnerArray = new ArrayList<>();
        final Iterator<ResTag> it = mDefaultResources.sortIterator(mSettings.getStringsComparator());
        if (mShowTranslated) {
            while (it.hasNext())
                spinnerArray.add(it.next().getId());
        } else {
            // If we're not showing the strings with a translation, we also need to
            // make sure that the currently selected locale doesn't already have them
            while (it.hasNext()) {
                ResTag rt = it.next();
                if (!mSelectedLocaleResources.contains(rt.getId()))
                    spinnerArray.add(rt.getId());
            }

            // Show a warning so the user (or developer) knows that things are working
            if (spinnerArray.size() == 0)
                Toast.makeText(this, R.string.no_strings_left, Toast.LENGTH_SHORT).show();
        }

        ArrayAdapter<String> idAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        idAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStringIdSpinner.setAdapter(idAdapter);
    }

    //endregion

    //region String and locale handling

    // Sets the current locale also updating the spinner selection
    private void setCurrentLocale(String locale) {
        // Clear the previous EditText fields
        mOriginalStringEditText.setText("");
        mTranslatedStringEditText.setText("");

        // Update the selected locale
        mSelectedLocale = locale;
        mRepo.setLastLocale(locale);

        if (locale != null) {
            int i = getItemIndex(mLocaleSpinner, LocaleString.getDisplay(locale));
            mLocaleSpinner.setSelection(i);
            mSelectedLocaleResources = mRepo.loadResources(locale);
            mSelectedLocaleResources.setFilter(mRepo.getStringFilter());
        } else {
            mSelectedLocaleResources = null;
        }

        checkTranslationVisibility();
        loadStringIDsSpinner();

        // There might be no strings, in which case we need to hide some buttons
        checkPreviousNextVisibility();
    }

    //endregion

    //region Utilities

    // Ensures that there is at least a locale selected
    private boolean isLocaleSelected(boolean showWarning) {
        boolean localeSelected = mSelectedLocaleResources != null;
        if (!localeSelected && showWarning) {
            Toast.makeText(this, R.string.no_locale_selected, Toast.LENGTH_SHORT).show();
        }
        return localeSelected;
    }

    // Increments the mStringIdSpinner index by delta i (di),
    // clamping the value if it's less than 0 or value ≥ IDs count.
    private void incrementStringIdIndex(int di) {
        int i = mStringIdSpinner.getSelectedItemPosition() + di;
        if (i > -1) {
            if (i < mStringIdSpinner.getCount()) {
                mStringIdSpinner.setSelection(i);
                updateSelectedResourceId((String)mStringIdSpinner.getSelectedItem());
            } else {
                Toast.makeText(this, R.string.no_strings_left, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setStringId(String id) {
        int i = getItemIndex(mStringIdSpinner, id);
        if (i > -1) {
            mStringIdSpinner.setSelection(i);
            updateSelectedResourceId((String)mStringIdSpinner.getSelectedItem());
        } else if (!mShowTranslated) {
            toggleShowTranslated(mShowTranslatedMenuItem);
            setStringId(id);
        }
    }

    // Updates the selected resource ID and also the EditTexts for its contents
    private void updateSelectedResourceId(@NonNull String resourceId) {
        if (resourceId.isEmpty()) {
            mSelectedResource = null;
            mOriginalStringEditText.setText("");
            mTranslatedStringEditText.setText("");
        } else {
            mSelectedResource = mDefaultResources.getTag(resourceId);
            mOriginalStringEditText.setText(mSelectedResource.getContent());
            mTranslatedStringEditText.setText(mSelectedLocaleResources.getContent(resourceId));
        }
        checkPreviousNextVisibility();
        updateProgress();
    }

    private void checkPreviousNextVisibility() {
        int countM1 = mStringIdSpinner.getCount()-1;
        int i = mStringIdSpinner.getSelectedItemPosition();
        mPreviousButton.setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);
        mNextButton.setVisibility(i == countM1 ? View.INVISIBLE : View.VISIBLE);
    }

    // Sadly, the spinners don't provide any method to retrieve
    // an item position given its value. This method helps that
    private int getItemIndex(Spinner spinner, String str) {
        if (str == null || str.isEmpty())
            return -1;

        for (int i = 0; i < spinner.getCount(); i++)
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(str))
                return i;
        return -1;
    }

    //endregion
}

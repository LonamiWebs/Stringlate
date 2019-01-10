package io.github.lonamiwebs.stringlate.activities.translate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import android.text.TextUtils;
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
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.gsantner.opoc.util.GeneralUtils;
import net.gsantner.opoc.util.ShareUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.info.BrowserActivity;
import io.github.lonamiwebs.stringlate.activities.export.CreateGistActivity;
import io.github.lonamiwebs.stringlate.activities.export.CreateIssueActivity;
import io.github.lonamiwebs.stringlate.activities.export.CreatePullRequestActivity;
import io.github.lonamiwebs.stringlate.classes.RepoSyncTask;
import io.github.lonamiwebs.stringlate.classes.locales.LocaleString;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.classes.repos.RepoProgress;
import io.github.lonamiwebs.stringlate.classes.resources.ResourceStringComparator;
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesTranslation;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;
import io.github.lonamiwebs.stringlate.classes.sources.GitSource;
import io.github.lonamiwebs.stringlate.dialogs.LocaleSelectionDialog;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.ContextUtils;
import io.github.lonamiwebs.stringlate.utilities.RepoHandlerHelper;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_ID;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_CREATE_FILE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_OPEN_TREE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_STRING_SELECTED;

public class TranslateActivity extends AppCompatActivity implements LocaleSelectionDialog.OnLocaleSelected {

    //region Members

    private AppSettings mSettings;

    private TextView mOriginalStringTextView;
    private EditText mTranslatedStringEditText;
    private TextView mCopyStringTextView;

    private Spinner mLocaleSpinner;
    private Spinner mStringIdSpinner;

    private Button mPreviousButton;
    private Button mNextButton;
    private ProgressBar mProgressProgressBar;
    private TextView mProgressTextView;

    private FrameLayout mFilterAppliedLayout;
    private TextView mFilterAppliedTextView;
    private TextView mUsesTranslationServiceTextView;

    private String mSelectedLocale;
    private ResTag mSelectedResource;
    private boolean mShowTranslated;
    private boolean mShowIdentical;
    private MenuItem mShowTranslatedMenuItem;
    private MenuItem mShowIdenticalMenuItem;

    private Resources mDefaultResources;
    private Resources mSelectedLocaleResources;

    private RepoHandler mRepo;

    private boolean mLoaded;

    // Since the string filter (search) applies to both the original and the
    // translated strings we can't just put the same filter on different sets.
    // Instead, find the matching strings and save their IDs (so this new ID
    // filter can be applied to any language indeed).
    private Set<String> mFilteredIDs = new HashSet<>();

    //endregion

    //region Initialization

    public static void launch(final Context ctx, final RepoHandler repo) {
        if (repo.isSyncing()) {
            Toast.makeText(ctx, R.string.wait_until_sync, Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(ctx, TranslateActivity.class);
            intent.putExtra(EXTRA_REPO, RepoHandlerHelper.toBundle(repo));
            ctx.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLoaded = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        mSettings = new AppSettings(this);

        mOriginalStringTextView = findViewById(R.id.originalStringEditText);
        mTranslatedStringEditText = findViewById(R.id.translatedStringEditText);
        mTranslatedStringEditText.addTextChangedListener(onTranslationChanged);

        mCopyStringTextView = findViewById(R.id.copyString);
        mCopyStringTextView.setOnClickListener(copyStringListener);

        mLocaleSpinner = findViewById(R.id.localeSpinner);
        mStringIdSpinner = findViewById(R.id.stringIdSpinner);

        mPreviousButton = findViewById(R.id.previousButton);
        mNextButton = findViewById(R.id.nextButton);
        mProgressProgressBar = findViewById(R.id.progressProgressBar);
        mProgressTextView = findViewById(R.id.progressTextView);

        mFilterAppliedLayout = findViewById(R.id.filter_applied_overlay_layout);
        mFilterAppliedTextView = findViewById(R.id.filterAppliedTextView);
        mUsesTranslationServiceTextView = findViewById(R.id.usesTranslationServiceTextView);

        mLocaleSpinner.setOnItemSelectedListener(eOnLocaleSelected);
        mStringIdSpinner.setOnItemSelectedListener(eOnStringIdSelected);

        mRepo = RepoHandlerHelper.fromBundle(getIntent().getBundleExtra(EXTRA_REPO));
        setTitle(mRepo.getProjectName());

        String a = mSettings.getEditingFont();
        Typeface font = Typeface.create(mSettings.getEditingFont(), Typeface.NORMAL);
        mOriginalStringTextView.setTypeface(font);
        mTranslatedStringEditText.setTypeface(font);
        mTranslatedStringEditText.postDelayed(() -> mTranslatedStringEditText.requestFocus(), 200);

        loadResources();
        onFilterUpdated(mRepo.settings.getStringFilter());

        // loadStringIDsSpinner is called too often at startup, use this flag to avoid it
        mLoaded = true;
        loadStringIDsSpinner();

        // Show the notice if this repository uses (or might use) a web translation service
        if (!mRepo.getSourceName().equals("git") || mRepo.getUsedTranslationService().isEmpty()) {
            mUsesTranslationServiceTextView.setVisibility(View.GONE);
        } else {
            mUsesTranslationServiceTextView.setVisibility(View.VISIBLE);
            mUsesTranslationServiceTextView.setText(getString(
                    R.string.application_may_use_translation_platform,
                    GeneralUtils.toTitleCase(mRepo.getUsedTranslationService())
            ));
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("translated_string_text", mTranslatedStringEditText.getText().toString());
        state.putString("selected_string", mSelectedResource == null ? null : mSelectedResource.getId());
        // TODO Should the state of all resources be saved (i.e. so the user can go back and
        // modify the translations they had made), or consider them as "translated" already?
    }

    @Override
    protected void onRestoreInstanceState(final Bundle state) {
        super.onRestoreInstanceState(state);

        final String selectedString = state.getString("selected_string");
        final String stringText = state.getString("translated_string_text");

        mSelectedLocaleResources.deleteId(selectedString);
        loadStringIDsSpinner();
        setStringId(selectedString);
        mTranslatedStringEditText.setText(stringText);
    }

    private void loadResources() {
        if (mRepo.hasDefaultLocale()) {
            mDefaultResources = mRepo.loadDefaultResources();
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
        menu.findItem(R.id.github_export_to_issue).setVisible(isGitHubRepository);
        menu.findItem(R.id.github_export_to_pr).setVisible(isGitHubRepository);

        mShowTranslatedMenuItem = menu.findItem(R.id.showTranslatedCheckBox);
        mShowIdenticalMenuItem = menu.findItem(R.id.showIdenticalCheckBox);

        mShowTranslated = mShowTranslatedMenuItem.isChecked();
        mShowIdentical = mShowIdenticalMenuItem.isChecked();

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
            } catch (Exception e) {
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

            // Peek translations
            case R.id.peekTranslations:
                launchPeekTranslationsActivity();
                return true;

            // Adding locales
            case R.id.addLocale:
                promptAddLocale();
                return true;

            // Sorting modes
            case R.id.sortAlphabetically:
                mSettings.setStringSortMode(ResourceStringComparator.SORT_ALPHABETICALLY);
                loadStringIDsSpinner();
                return true;

            case R.id.sortLength:
                mSettings.setStringSortMode(ResourceStringComparator.SORT_STRING_LENGTH);
                loadStringIDsSpinner();
                return true;

            case R.id.action_open_project_homepage: {
                Intent browserIntent = new Intent(this, BrowserActivity.class);
                browserIntent.putExtra(
                        BrowserActivity.EXTRA_LOAD_URL, mRepo.settings.getProjectWebUrl()
                );
                startActivity(browserIntent);
                return true;
            }

            // Exporting resources
            case R.id.exportToSdcard:
            case R.id.export_to_gist:
            case R.id.github_export_to_issue:
            case R.id.github_export_to_pr:
            case R.id.exportShare:
            case R.id.export_mail:
            case R.id.export_hastebin:
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
                    case R.id.exportToSdcard:
                        exportToSd();
                        break;
                    case R.id.export_to_gist:
                        exportToGist();
                        break;
                    case R.id.github_export_to_issue:
                        exportToIssue();
                        break;
                    case R.id.github_export_to_pr:
                        exportToPullRequest();
                        break;
                    case R.id.exportShare:
                        exportToShare();
                        break;
                    case R.id.exportCopy:
                        exportToCopy();
                        break;
                    case R.id.export_hastebin:
                        exportToHastebin();
                        break;
                    case R.id.export_mail:
                        exportToEmail();
                        break;
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
            case R.id.showIdenticalCheckBox:
                toggleShowIdentical(item);
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
                            askBranchUpdateStrings();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            // No file has been modified, simply onCountChanged the strings discarding changes
            askBranchUpdateStrings();
        }
    }

    private void askBranchUpdateStrings() {
        final ArrayList<String> branchesArray = mRepo.getRemoteBranches();
        final CharSequence[] branches = new CharSequence[branchesArray.size()];
        for (int i = 0; i < branches.length; ++i) {
            branches[i] = branchesArray.get(i).contains("/") ?
                    branchesArray.get(i).substring(branchesArray.get(i).lastIndexOf('/') + 1)
                    :
                    branchesArray.get(i);
        }

        if (branches.length > 1) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.select_branch)
                    .setPositiveButton(getString(R.string.ignore), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            updateStrings("HEAD");
                        }
                    })
                    .setItems(branches, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            updateStrings(branches[item].toString());
                        }
                    })
                    .show();
        } else {
            updateStrings("HEAD");
        }
    }

    // Synchronize our local strings.xml files with the remote GitHub repository
    private void updateStrings(@NonNull final String branch) {
        if (!new ContextUtils(this).isConnectedToInternet(R.string.no_internet_connection))
            return;

        // Don't let the users stay while we're synchronizing resources.
        //
        // With some work, we could actually keep track of both the resources the user
        // was using before the synchronization started, and what strings were being shown
        // (important when already-translated strings are hidden). Problems arise when
        // the user may leave the screen, since the user state would have to be saved to
        // permanent storage. Not only this, after synchronization finished, the old
        // resources would have to merge the new synchronized strings with what the user had.
        //
        // In summary, just kick the user.
        save();
        finish();

        // TODO Don't assume GitSource
        new RepoSyncTask(this, mRepo,
                new GitSource(mRepo.settings.getSource(), branch), false).start();
    }

    //endregion

    //region Searching for strings and peeking translations

    private void launchStringSearchActivity() {
        if (isLocaleSelected(true)) {
            Intent intent = new Intent(this, SearchStringActivity.class);
            intent.putExtra(EXTRA_REPO, RepoHandlerHelper.toBundle(mRepo));
            intent.putExtra(EXTRA_LOCALE, mSelectedLocale);
            startActivityForResult(intent, RESULT_STRING_SELECTED);
        }
    }

    private void launchPeekTranslationsActivity() {
        if (isLocaleSelected(true) && mSelectedResource != null) {
            Intent intent = new Intent(this, PeekTranslationsActivity.class);
            intent.putExtra(EXTRA_REPO, RepoHandlerHelper.toBundle(mRepo));
            intent.putExtra(EXTRA_LOCALE, mSelectedLocale);
            intent.putExtra(EXTRA_ID, mSelectedResource.getId());
            startActivity(intent);
        }
    }

    //endregion

    //region Adding locales menu events

    // Prompts the user to add a new locale. If it exists,
    // no new file is created but the entered locale is selected.
    private void promptAddLocale() {
        final LocaleSelectionDialog dialog = LocaleSelectionDialog.newInstance();
        dialog.show(getFragmentManager(), LocaleSelectionDialog.TAG);
    }

    @Override
    public void onLocaleSelected(final Locale which) {
        if (which == null)
            return;

        final String locale = LocaleString.getFullCode(which);
        if (mRepo.createLocale(locale)) {
            loadLocalesSpinner();
            setCurrentLocale(locale);
        } else {
            Toast.makeText(TranslateActivity.this, R.string.create_locale_error, Toast.LENGTH_SHORT).show();
        }
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
        intent.putExtra(EXTRA_REPO, RepoHandlerHelper.toBundle(mRepo));
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
        intent.putExtra(EXTRA_REPO, RepoHandlerHelper.toBundle(mRepo));
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
        if (!new ContextUtils(this).isConnectedToInternet(R.string.no_internet_connection))
            return;

        Intent intent = new Intent(this, CreatePullRequestActivity.class);
        intent.putExtra(EXTRA_REPO, RepoHandlerHelper.toBundle(mRepo));
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

        new ShareUtil(this).setClipboard(xml);
        Toast.makeText(this, getString(R.string.xml_copied_to_clipboard, filename),
                Toast.LENGTH_SHORT).show();
    }

    // Exports the currently selected locale resources to hastebin and sets the primary clipboard
    private void exportToHastebin() {
        String xml = mRepo.mergeDefaultTemplate(mSelectedLocale);

        final ShareUtil shu = new ShareUtil(this);
        shu.pasteOnHastebin(xml, (ok, url) -> {
            if (ok) {
                shu.setClipboard(url);
            }
            Toast.makeText(TranslateActivity.this,
                    ok ? R.string.exported_to_hastebin : R.string.export_unsuccessful,
                    Toast.LENGTH_SHORT).show();
        });
    }

    // Start drafting an email
    private void exportToEmail() {
        String xml = mRepo.mergeDefaultTemplate(mSelectedLocale);
        String subject = mRepo.getProjectName() + " - "
                + getString(R.string.updated_x_translation, mSelectedLocale,
                LocaleString.getEnglishDisplay(mSelectedLocale));

        new ShareUtil(this).draftEmail(subject, xml, mRepo.settings.getProjectMail());
    }


    //endregion

    //region Deleting menu events

    // Deletes the currently selected string ID, this needs no warning
    private void deleteString() {
        if (!isLocaleSelected(true)) return;

        mSelectedLocaleResources.deleteId((String) mStringIdSpinner.getSelectedItem());
        mTranslatedStringEditText.setText("");
    }

    // Prompts the user whether they want to delete the selected locale or not
    // This does need warning since deleting a whole locale is a big deal
    private void promptDeleteLocale() {
        if (mLocaleSpinner.getCount() == 0) {
            Toast.makeText(this, R.string.delete_no_locale_bad_joke, Toast.LENGTH_LONG).show();
            return;
        }
        if (mSelectedLocaleResources.count() == 0) {
            // No translation, no need to confirm
            deleteCurrentLocale();
        } else {
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
    }

    private void deleteCurrentLocale() {
        mRepo.deleteLocale(mSelectedLocale);
        loadLocalesSpinner();
        checkTranslationVisibility();

        // We need to clear the selected locale if it's now empty
        if (mLocaleSpinner.getCount() == 0)
            setCurrentLocale(null);
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
        if (mShowIdentical)
            return; // Only showing identical strings, we don't care more

        String lastId = mSelectedResource == null ? null : mSelectedResource.getId();
        loadStringIDsSpinner();

        // Set the last string that was being used
        if (mShowTranslated) {
            setStringId(lastId);
        } else if (lastId != null) {
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

    // Toggles the "Show identical strings" checkbox and updates the spinner
    private void toggleShowIdentical(MenuItem item) {
        mShowIdentical = !mShowIdentical;
        if (item != null) {
            item.setChecked(mShowIdentical);
        }

        String lastId = mSelectedResource == null ? null : mSelectedResource.getId();
        loadStringIDsSpinner();

        if (!mShowIdentical) {
            setStringId(lastId);
        } else if (lastId != null) {
            // Same as toggleShowTranslated but for identical strings
            String id;
            String selectId = null;
            boolean isIdentical;
            boolean selectNext = false;
            for (ResTag rs : mDefaultResources) {
                id = rs.getId();
                isIdentical = mSelectedLocaleResources.getContent(id).equals(rs.getContent());

                if (!isIdentical) {
                    if (lastId.equals(id)) {
                        selectNext = true;
                    }
                } else {
                    if (lastId.equals(id) || selectNext) {
                        selectId = id;
                        break;
                    } else {
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
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (mSelectedLocaleResources != null) {
                String content = mTranslatedStringEditText.getText().toString();
                mSelectedLocaleResources.setContent(mSelectedResource, content);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    //endregion

    //region Spinner events

    private final AdapterView.OnItemSelectedListener
            eOnLocaleSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
            final LocaleString selectedLocale = (LocaleString) parent.getItemAtPosition(i);
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
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private final AdapterView.OnItemSelectedListener
            eOnStringIdSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
            updateSelectedResourceId((String) parent.getItemAtPosition(i));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    //endregion

    //endregion

    //region UI

    // Checks whether the translation layout (EditText and previous/next buttons)
    // should be visible (there is at least one non-default locale) or not.
    private void checkTranslationVisibility() {
        if (mLocaleSpinner.getCount() == 0) {
            Toast.makeText(this, R.string.add_locale_to_start, Toast.LENGTH_SHORT).show();
            //findViewById(R.id.translationLayout).setVisibility(View.GONE);
        } else {
            //findViewById(R.id.translationLayout).setVisibility(View.VISIBLE);
        }
    }

    private void updateProgress() {
        if (mSelectedLocaleResources == null) {
            mProgressProgressBar.setMax(1);
            mProgressProgressBar.setProgress(0);
            mProgressTextView.setText("");
        } else {
            // Keep track of the translated strings count and the characters of the
            // original strings + those same characters if a translation is available.
            // This will be used to make a weighted progress (if you translated only
            // long strings, then this will be closer to 100% than if you translated small ones).
            RepoProgress progress = new RepoProgress();
            progress.stringsCount = mDefaultResources.count();

            int chars;
            for (ResTag rs : mDefaultResources) {
                chars = rs.getContentLength();
                progress.totalChars += chars;
                if (mSelectedLocaleResources.contains(rs.getId())) {
                    progress.translatedCount += 1;
                    progress.currentChars += chars;
                }
            }

            // The progress bar will be using the weighted value
            mProgressProgressBar.setMax(progress.totalChars);
            mProgressProgressBar.setProgress(progress.currentChars);

            // The text view will show the string count and the weighted percentage
            mProgressTextView.setText(getString(R.string.translation_progress,
                    progress.translatedCount, progress.stringsCount, 100f * progress.getProgress()
            ));

            // Save the progress for the history fragment to reuse without recalculating
            mRepo.saveProgress(progress);
        }
    }

    private void onFilterUpdated(@NonNull final String filter) {
        // Update the filter, it might have been changed from the Search activity
        // and JSON doesn't load the changes from the file but rather keeps a copyFile
        mRepo.settings.setStringFilter(filter);
        mFilteredIDs.clear();
        for (ResourcesTranslation translation :
                ResourcesTranslation.fromPairs(mDefaultResources, mSelectedLocaleResources, filter)) {
            mFilteredIDs.add(translation.getId());
        }

        if (mSelectedLocaleResources != null) {
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
        setCurrentLocale(mRepo.settings.getLastLocale());
    }

    private void loadStringIDsSpinner() {
        if (!mLoaded || !isLocaleSelected(false)) return;

        ArrayList<String> spinnerArray = new ArrayList<>();
        final Iterator<ResTag> it = mDefaultResources.sortIterator(
                ResourceStringComparator.getStringsComparator(mSettings.getStringSortMode()),
                mFilteredIDs
        );
        if (mShowIdentical) {
            // Only show those which translation is identical to the original text
            while (it.hasNext()) {
                ResTag rt = it.next();
                if (mSelectedLocaleResources.getContent(rt.getId()).equals(rt.getContent()))
                    spinnerArray.add(rt.getId());
            }
        } else {
            if (mShowTranslated) {
                while (it.hasNext()) {
                    spinnerArray.add(it.next().getId());
                }
            } else {
                // If we're not showing the strings with a translation, we also need to
                // make sure that the currently selected locale doesn't already have them
                while (it.hasNext()) {
                    ResTag rt = it.next();
                    if (!mSelectedLocaleResources.contains(rt.getId()))
                        spinnerArray.add(rt.getId());
                }
            }
        }

        ArrayAdapter<String> idAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        idAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStringIdSpinner.setAdapter(idAdapter);

        // Show a warning so the user (or developer) knows that things are working
        if (spinnerArray.size() == 0) {
            Toast.makeText(this, R.string.no_strings_left, Toast.LENGTH_SHORT).show();
            checkPreviousNextVisibility();
        }
    }

    //endregion

    //region String and locale handling

    // Sets the current locale also updating the spinner selection
    private void setCurrentLocale(String locale) {
        if (TextUtils.equals(mSelectedLocale, locale)) {
            return;
        }

        if (!TextUtils.equals(mSettings.getDefaultLocale(), locale)) {
            mSettings.setDefaultLocale(locale);
        }

        // Clear the previous EditText fields
        mOriginalStringTextView.setText("");
        mTranslatedStringEditText.setText("");

        // Update the selected locale
        mSelectedLocale = locale;
        mRepo.settings.setLastLocale(locale);

        if (locale != null) {
            int i = getItemIndex(mLocaleSpinner, LocaleString.getDisplay(locale));
            mLocaleSpinner.setSelection(i);
            mSelectedLocaleResources = mRepo.loadResources(locale);
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
        save(); // Save every time the user changes to a new string for safety reasons
        int i = mStringIdSpinner.getSelectedItemPosition() + di;
        if (i > -1) {
            if (i < mStringIdSpinner.getCount()) {
                mStringIdSpinner.setSelection(i);
                updateSelectedResourceId((String) mStringIdSpinner.getSelectedItem());
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.next_steps,
                                getString(R.string.export_ellipsis),
                                getString(R.string.show_translated_strings)))
                        .setPositiveButton(R.string.done, null)
                        .show();
            }
        }
        mTranslatedStringEditText.requestFocus();
    }

    private void setStringId(String id) {
        if (id == null)
            return;

        int i = getItemIndex(mStringIdSpinner, id);
        if (i > -1) {
            save(); // Save every time the user changes to a new string for safety reasons
            mStringIdSpinner.setSelection(i);
            updateSelectedResourceId((String) mStringIdSpinner.getSelectedItem());
        } else if (!mShowTranslated) {
            toggleShowTranslated(mShowTranslatedMenuItem);
            setStringId(id);
        }
    }

    private final View.OnClickListener copyStringListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mTranslatedStringEditText.setText(mOriginalStringTextView.getText());
        }
    };

    // Updates the selected resource ID and also the EditTexts for its contents
    private void updateSelectedResourceId(@NonNull String resourceId) {
        if (resourceId.isEmpty()) {
            mSelectedResource = null;
            mOriginalStringTextView.setText("");
            mTranslatedStringEditText.setText("");
        } else {
            mSelectedResource = mDefaultResources.getTag(resourceId);
            mOriginalStringTextView.setText(mSelectedResource.getContent());
            mTranslatedStringEditText.setText(mSelectedLocaleResources.getContent(resourceId));
        }
        checkPreviousNextVisibility();
        updateProgress();
    }

    private void checkPreviousNextVisibility() {
        int count = mStringIdSpinner.getCount();
        boolean showDone;
        if (count == 0) {
            mPreviousButton.setVisibility(View.INVISIBLE);
            showDone = true;
            mOriginalStringTextView.setText("");
            if (mSelectedResource == null) // Ensure it's null
                mTranslatedStringEditText.setText("");
        } else {
            int i = mStringIdSpinner.getSelectedItemPosition();
            mPreviousButton.setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);
            showDone = i == (count - 1);
        }
        mNextButton.setText(showDone ? R.string.done : R.string.next);
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

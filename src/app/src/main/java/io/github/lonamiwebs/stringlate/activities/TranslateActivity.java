package io.github.lonamiwebs.stringlate.activities;

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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesString;
import io.github.lonamiwebs.stringlate.interfaces.Callback;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.utilities.GitHub;
import io.github.lonamiwebs.stringlate.utilities.RepoHandler;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_FILENAME;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO_NAME;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO_OWNER;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_XML_CONTENT;
import static io.github.lonamiwebs.stringlate.utilities.Constants.RESULT_CREATE_FILE;

public class TranslateActivity extends AppCompatActivity {

    //region Members

    private EditText mOriginalStringEditText;
    private EditText mTranslatedStringEditText;

    private Spinner mLocaleSpinner;
    private Spinner mStringIdSpinner;

    private String mSelectedLocale;
    private String mSelectedResourceId;
    private boolean mShowTranslated;

    private Resources mDefaultResources;
    private Resources mSelectedLocaleResources;

    private RepoHandler mRepo;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        mOriginalStringEditText = (EditText)findViewById(R.id.originalStringEditText);
        mTranslatedStringEditText = (EditText)findViewById(R.id.translatedStringEditText);
        mTranslatedStringEditText.addTextChangedListener(onTranslationChanged);

        mLocaleSpinner = (Spinner)findViewById(R.id.localeSpinner);
        mStringIdSpinner = (Spinner)findViewById(R.id.stringIdSpinner);

        mLocaleSpinner.setOnItemSelectedListener(eOnLocaleSelected);
        mStringIdSpinner.setOnItemSelectedListener(eOnStringIdSelected);

        // Retrieve the owner and repository name
        Intent intent = getIntent();
        String owner = intent.getStringExtra(EXTRA_REPO_OWNER);
        String repoName = intent.getStringExtra(EXTRA_REPO_NAME);

        mRepo = new RepoHandler(this, owner, repoName);
        setTitle(mRepo.toString());

        if (mRepo.hasLocale(null)) {
            mDefaultResources = mRepo.loadResources(null);
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
        inflater.inflate(R.menu.translate_menu, menu);

        mShowTranslated = menu.findItem(R.id.showTranslatedCheckBox).isChecked();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Synchronizing repository
            case R.id.updateStrings:
                updateStrings();
                return true;

            // Adding locales
            case R.id.addLocale:
                promptAddLocale();
                return true;

            // Exporting resources
            case R.id.exportToSdcard:
                exportToSd();
                return true;
            case R.id.exportToGist:
                exportToGist();
                return true;
            case R.id.exportToPr:
                exportToPullRequest();
                return true;
            case R.id.exportShare:
                exportToShare();
                return true;
            case R.id.exportCopy:
                exportToCopy();
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

    @Override
    public void onBackPressed() {
        checkResourcesSaved(new Callback<Boolean>() {
            @Override
            public void onCallback(Boolean saved) {
                finish();
            }
        });
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
            }
        }
    }

    //endregion

    //region Menu events

    //region Repository synchronizing menu events

    // Synchronize our local strings.xml files with the remote GitHub repository,
    // previously checking if the strings.xml was saved and asking whether
    // files should be overwritten after synchronizing (if any change was made)
    private void updateStrings() {
        // We need to save the context for the inner AlertBuilder
        final Context context = this;

        checkResourcesSaved(new Callback<Boolean>() {
            @Override
            public void onCallback(Boolean saved) {
                // We need to save the files before syncing, or it will ask
                // again after the synchronization finished (and it looks out of place)
                if (!saved) {
                    Toast.makeText(context, R.string.save_before_sync_required, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mRepo.anyModified()) {
                    // Do not mistake unsaved changes (modifications, .isSaved())
                    // with the file being ever modified (.wasModified())
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.files_modified)
                            .setMessage(R.string.files_modified_keep_changes)
                            .setPositiveButton(R.string.keep_changes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    updateStrings(true);
                                }
                            })
                            .setNegativeButton(R.string.discard_changes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    updateStrings(false);
                                }
                            })
                            .show();
                } else {
                    // No file has been modified, simply update the strings discarding changes
                    updateStrings(false);
                }
            }
        });
    }

    // Synchronize our local strings.xml files with the remote GitHub repository
    private void updateStrings(boolean keepChanges) {
        if (!GitHub.gCanCall()) {
            Toast.makeText(getApplicationContext(),
                    R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progress = ProgressDialog.show(this,
                getString(R.string.loading_ellipsis), null, true);

        mRepo.syncResources(new ProgressUpdateCallback() {
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

                mDefaultResources = mRepo.loadResources(null);
                loadLocalesSpinner();
            }
        }, keepChanges);
    }

    //endregion

    //region Adding locales menu events

    // Prompts the user to add a new locale. If it exists,
    // no new file is created but the entered locale is selected.
    private void promptAddLocale() {
        final EditText et = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.enter_locale)
                .setMessage(getString(R.string.enter_locale_long, Locale.getDefault().getLanguage()))
                .setView(et)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String locale = et.getText().toString().trim();
                        if (isValidLocale(locale)) {
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

    // There is no need to check if the resources are saved when exporting.
    // The exported values are always the in-memory values, which are also
    // always up-to-date.

    // Exports the currently selected locale resources to the SD card
    private void exportToSd() {
        String filename = mSelectedLocaleResources.getFilename();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.setType("text/xml");
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            startActivityForResult(intent, RESULT_CREATE_FILE);
        } else {
            File output = new File(Environment.getExternalStorageDirectory(),
                    mSelectedLocaleResources.getFilename());
            doExportToSd(Uri.fromFile(output));
        }
    }

    private void doExportToSd(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor());
            mSelectedLocaleResources.save(out);
            Toast.makeText(this, getString(R.string.export_file_success, uri.getPath()),
                    Toast.LENGTH_SHORT).show();

            out.close();
            pfd.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.export_file_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // Exports the currently selected locale resources to a GitHub Gist
    private void exportToGist() {
        Intent intent = new Intent(getApplicationContext(), CreateGistActivity.class);
        intent.putExtra(EXTRA_XML_CONTENT, mSelectedLocaleResources.toString(true));
        intent.putExtra(EXTRA_FILENAME, mSelectedLocaleResources.getFilename());
        startActivity(intent);
    }

    // Exports the currently selected locale resources to a GitHub Pull Request
    private void exportToPullRequest() {
        Toast.makeText(this, "Not implemented. Sorry about that!", Toast.LENGTH_SHORT).show();
    }

    // Exports the currently selected locale resources to a plain text share intent
    private void exportToShare() {
        String xml = mSelectedLocaleResources.toString(true);
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, xml);
        startActivity(Intent.createChooser(sharingIntent,
                getString(R.string.export_share)));
    }

    // Exports the currently selected locale resources to the primary clipboard
    private void exportToCopy() {
        String filename = mSelectedLocaleResources.getFilename();
        String xml = mSelectedLocaleResources.toString(true);

        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(filename, xml));
        Toast.makeText(this, getString(R.string.xml_copied_to_clipboard, filename),
                Toast.LENGTH_SHORT).show();
    }

    //endregion

    //region Deleting menu events

    // Deletes the currently selected string ID, this needs no warning
    private void deleteString() {
        if (!isLocaleSelected()) {
            showNoLocaleSelected();
            return;
        }

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
        item.setChecked(mShowTranslated);
        loadStringIDsSpinner();
    }

    //endregion

    //endregion

    //region Button events

    public void onPreviousClick(final View v) {
        incrementStringIdIndex(-1);
    }

    public void onNextClick(final View v) {
        incrementStringIdIndex(+1);
    }

    public void onSaveClick(final View v) {
        if (mSelectedLocaleResources.save())
            Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, R.string.save_error, Toast.LENGTH_SHORT).show();
    }

    //endregion

    //region EditText events

    private TextWatcher onTranslationChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (mSelectedLocaleResources != null) {
                String content = mTranslatedStringEditText.getText().toString();
                mSelectedLocaleResources.setContent(mSelectedResourceId, content);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) { }
    };

    //endregion

    //region Spinner events

    AdapterView.OnItemSelectedListener
            eOnLocaleSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
            final String selectedLocale = (String)parent.getItemAtPosition(i);
            if (isLocaleSelected()) {
                checkResourcesSaved(new Callback<Boolean>() {
                    @Override
                    public void onCallback(Boolean saved) {
                        setCurrentLocale(selectedLocale);
                    }
                });
            } else {
                // If it's the first time we're selecting a locale,
                // we don't care unsaved changes (because there isn't any)
                setCurrentLocale(selectedLocale);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    AdapterView.OnItemSelectedListener
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

    //region Spinner loading

    private void loadLocalesSpinner() {
        ArrayList<String> spinnerArray = new ArrayList<>();
        for (String locale : mRepo.getLocales())
            if (!locale.equals(RepoHandler.DEFAULT_LOCALE))
                spinnerArray.add(locale);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLocaleSpinner.setAdapter(adapter);
    }

    private void loadStringIDsSpinner() {
        if (!isLocaleSelected())
            return;

        ArrayList<String> spinnerArray = new ArrayList<>();
        if (mShowTranslated) {
            for (ResourcesString rs : mDefaultResources)
                if (rs.isTranslatable())
                    spinnerArray.add(rs.getId());
        } else {
            // If we're not showing the strings with a translation, we also need to
            // make sure that the currently selected locale doesn't already have them
            for (ResourcesString rs : mDefaultResources)
                if (!mSelectedLocaleResources.contains(rs.getId()) && rs.isTranslatable())
                    spinnerArray.add(rs.getId());

            // Show a warning so the user (or developer) knows that things are working
            if (spinnerArray.size() == 0)
                Toast.makeText(this, R.string.no_strings_left, Toast.LENGTH_SHORT).show();
        }

        ArrayAdapter<String> idAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        idAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ((Spinner) findViewById(R.id.stringIdSpinner)).setAdapter(idAdapter);
    }

    //endregion

    //region String and locale handling

    // Sets the current locale also updating the spinner selection
    private void setCurrentLocale(String locale) {
        mSelectedLocale = locale;

        if (locale != null) {
            mLocaleSpinner.setSelection(getItemIndex(mLocaleSpinner, locale));
            mSelectedLocaleResources = mRepo.loadResources(locale);
        } else {
            mSelectedLocaleResources = null;
        }

        checkTranslationVisibility();
        loadStringIDsSpinner();
    }

    //endregion

    //region Utilities

    // Checks whether the translation layout (EditText and previous/next buttons)
    // should be visible (there is at least one non-default locale) or not.
    void checkTranslationVisibility() {
        if (mLocaleSpinner.getCount() == 0) {
            Toast.makeText(this, R.string.add_locale_to_start, Toast.LENGTH_SHORT).show();
            findViewById(R.id.translationLayout).setVisibility(View.GONE);
        } else {
            findViewById(R.id.translationLayout).setVisibility(View.VISIBLE);
        }
    }

    // Checks whether the current resources are saved or not
    // If they're not, the user is asked to save them first
    void checkResourcesSaved(final Callback<Boolean> callback) {
        if (!isLocaleSelected()) {
            callback.onCallback(false);
            return;
        }

        if (mSelectedLocaleResources.areSaved())
            callback.onCallback(true);
        else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.save_resources_question)
                    .setMessage(R.string.save_resources_question_long)
                    .setCancelable(false)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (mSelectedLocaleResources.save())
                                callback.onCallback(true);
                            else {
                                Toast.makeText(getApplicationContext(),
                                        R.string.save_error, Toast.LENGTH_SHORT).show();

                                callback.onCallback(false);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            callback.onCallback(false);
                        }
                    })
                    .show();
        }
    }

    // Ensures that there is at least a locale selected
    boolean isLocaleSelected() {
        return mSelectedLocaleResources != null;
    }

    // Shows the "No locale selected" warning
    void showNoLocaleSelected() {
        Toast.makeText(this, R.string.no_locale_selected, Toast.LENGTH_SHORT).show();
    }

    boolean isValidLocale(String locale) {
        if (locale.contains("-")) {
            // If there is an hyphen, then a country was also specified
            for (Locale l : Locale.getAvailableLocales())
                if (!l.getCountry().isEmpty())
                    if (locale.equals(l.getLanguage()+"-"+l.getCountry()))
                        return true;
        } else {
            for (Locale l : Locale.getAvailableLocales())
                if (locale.equals(l.getLanguage()))
                    return true;
        }
        return false;
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

    // Updates the selected resource ID and also the EditTexts for its contents
    private void updateSelectedResourceId(String resourceId) {
        mSelectedResourceId = resourceId;
        mOriginalStringEditText.setText(mDefaultResources.getContent(resourceId));
        mTranslatedStringEditText.setText(mSelectedLocaleResources.getContent(resourceId));
    }

    // Sadly, the spinners don't provide any method to retrieve
    // an item position given its value. This method helps that
    private int getItemIndex(Spinner spinner, String str) {
        for (int i = 0; i < spinner.getCount(); i++)
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(str))
                return i;
        return -1;
    }

    //endregion
}

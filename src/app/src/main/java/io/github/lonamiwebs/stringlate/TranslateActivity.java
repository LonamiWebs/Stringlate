package io.github.lonamiwebs.stringlate;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.Interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.ResourcesStrings.Resources;
import io.github.lonamiwebs.stringlate.ResourcesStrings.ResourcesString;
import io.github.lonamiwebs.stringlate.Utilities.RepoHandler;

public class TranslateActivity extends AppCompatActivity {

    //region Members

    private EditText mOriginalStringEditText;
    private EditText mTranslatedStringEditText;

    private Spinner mLocaleSpinner;
    private Spinner mStringIdSpinner;

    private String mSelectedLocale;
    private boolean mShowTranslated;

    private Resources mDefaultResources;
    private Resources mSelectedLocaleResources;

    private RepoHandler mRepo;

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        mOriginalStringEditText = (EditText)findViewById(R.id.originalStringEditText);
        mTranslatedStringEditText = (EditText)findViewById(R.id.translatedStringEditText);

        mLocaleSpinner = (Spinner)findViewById(R.id.localeSpinner);
        mStringIdSpinner = (Spinner)findViewById(R.id.stringIdSpinner);

        mLocaleSpinner.setOnItemSelectedListener(eOnLocaleSelected);
        mStringIdSpinner.setOnItemSelectedListener(eOnStringIdSelected);

        Intent intent = getIntent();
        String owner = intent.getStringExtra(MainActivity.EXTRA_REPO_OWNER);
        String repoName = intent.getStringExtra(MainActivity.EXTRA_REPO_NAME);

        setTitle(owner+"/"+repoName);

        mRepo = new RepoHandler(this, owner, repoName);
        if (mRepo.hasLocale(null)) {
            mDefaultResources = mRepo.loadResources(null);
            loadLocalesSpinner();
            checkTranslationVisibility();
        } else {
            // This should never happen since it's checked when creating the repository
            Toast.makeText(this, R.string.no_strings_found_update, Toast.LENGTH_LONG).show();
        }
    }

    void checkTranslationVisibility() {
        if (mLocaleSpinner.getCount() == 0) {
            Toast.makeText(this, R.string.add_locale_to_start, Toast.LENGTH_SHORT).show();
        } else {
            findViewById(R.id.translationLayout).setVisibility(View.VISIBLE);
        }
    }

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
            case R.id.updateStrings:
                updateStrings();
                return true;
            case R.id.addLocale:
                final EditText et = new EditText(this);
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle(R.string.enter_locale)
                        .setMessage(R.string.enter_locale_long)
                        .setView(et)
                        .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String locale = et.getText().toString();
                                if (mRepo.createLocale(locale)) {
                                    loadLocalesSpinner();
                                    setCurrentLocale(locale);
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            R.string.create_locale_error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, null);

                AlertDialog ad = builder.create();
                ad.show();

                return true;
            case R.id.showTranslatedCheckBox:
                mShowTranslated = !mShowTranslated;
                item.setChecked(mShowTranslated);
                loadStringIDsSpinner();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onPreviousClick(final View v) {
        incrementStringIdIndex(-1);
    }

    public void onSaveClick(final View v) {
        // TODO hmm when changing locale it will ask Save changes?
        if (mRepo.saveResources(mSelectedLocaleResources, mSelectedLocale))
            Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, R.string.save_error, Toast.LENGTH_SHORT).show();
    }

    public void onNextClick(final View v) {
        incrementStringIdIndex(+1);
    }

    private void incrementStringIdIndex(int di) {
        int i = mStringIdSpinner.getSelectedItemPosition() + di;
        if (i > -1) {
            if (i < mStringIdSpinner.getCount()) {
                String resourceId = (String)mStringIdSpinner.getSelectedItem();
                String content = mTranslatedStringEditText.getText().toString();
                mSelectedLocaleResources.setContent(resourceId, content);

                mStringIdSpinner.setSelection(i);
            } else {
                Toast.makeText(this, R.string.no_strings_left, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateStrings() {
        final ProgressDialog progress = ProgressDialog.show(this, getString(R.string.loading_ellipsis), null, true);

        mRepo.updateStrings(new ProgressUpdateCallback() {
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
        });
    }

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
        if (mSelectedLocaleResources == null)
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
        }

        ArrayAdapter<String> idAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        idAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ((Spinner) findViewById(R.id.stringIdSpinner)).setAdapter(idAdapter);
    }

    //region Events

    AdapterView.OnItemSelectedListener
            eOnLocaleSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
            setCurrentLocale((String)parent.getItemAtPosition(i));
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    private void setCurrentLocale(String locale) { setCurrentLocale(locale, true); }

    private void setCurrentLocale(String locale, boolean setSpinnerSelection) {
        if (setSpinnerSelection) {
            mLocaleSpinner.setSelection(getItemIndex(mLocaleSpinner, locale));
        }
        mSelectedLocale = locale;
        mSelectedLocaleResources = mRepo.loadResources(mSelectedLocale);
        checkTranslationVisibility();
        loadStringIDsSpinner();
    }

    private int getItemIndex(Spinner spinner, String str) {
        for (int i = 0; i < spinner.getCount(); i++)
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(str))
                return i;
        return -1;
    }

    AdapterView.OnItemSelectedListener
            eOnStringIdSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
            String id = (String)parent.getItemAtPosition(i);
            mOriginalStringEditText.setText(mDefaultResources.getContent(id));
            mTranslatedStringEditText.setText(mSelectedLocaleResources.getContent(id));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    //endregion
}

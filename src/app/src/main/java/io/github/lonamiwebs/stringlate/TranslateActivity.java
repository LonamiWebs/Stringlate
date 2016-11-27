package io.github.lonamiwebs.stringlate;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.Interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.ResourcesStrings.ResourcesString;
import io.github.lonamiwebs.stringlate.Utilities.RepoHandler;

public class TranslateActivity extends AppCompatActivity {

    private EditText mOriginalStringEditText;
    private EditText mTranslatedStringEditText;

    private RepoHandler mRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        mOriginalStringEditText = (EditText)findViewById(R.id.originalStringEditText);
        mTranslatedStringEditText = (EditText)findViewById(R.id.translatedStringEditText);

        Intent intent = getIntent();
        String owner = intent.getStringExtra(MainActivity.EXTRA_REPO_OWNER);
        String repoName = intent.getStringExtra(MainActivity.EXTRA_REPO_NAME);

        mRepo = new RepoHandler(this, owner, repoName);
        loadSpinners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.translate_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.updateStrings:
                updateStrings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateStrings() {
        final ProgressDialog progress = ProgressDialog.show(this,
                getString(R.string.loading_ellipsis), null, true);

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
            }
        });
    }

    private void loadSpinners() {
        // Load the string IDs spinner ("name" attribute on strings.xml)
        try {
            ArrayList<String> idSpinnerArray = new ArrayList<>();
            for (ResourcesString rs : mRepo.loadResources(null))
                if (rs.isTranslatable())
                    idSpinnerArray.add(rs.getId());

            ArrayAdapter<String> idAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, idSpinnerArray);

            idAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ((Spinner) findViewById(R.id.stringIdSpinner)).setAdapter(idAdapter);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, R.string.no_strings_found_update, Toast.LENGTH_LONG).show();
        }

        // Load the locales spinner
        ArrayList<String> localeSpinnerArray = new ArrayList<>();
        for (String locale : mRepo.getLocales())
            if (!locale.equals(RepoHandler.DEFAULT_LOCALE))
                localeSpinnerArray.add(locale);

        ArrayAdapter<String> localeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, localeSpinnerArray);

        localeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ((Spinner)findViewById(R.id.localeSpinner)).setAdapter(localeAdapter);
    }
}

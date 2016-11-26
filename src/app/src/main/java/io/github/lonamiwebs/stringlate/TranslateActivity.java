package io.github.lonamiwebs.stringlate;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import io.github.lonamiwebs.stringlate.Utilities.RepoHandler;

public class TranslateActivity extends AppCompatActivity {

    EditText mOriginalStringEditText;
    EditText mTranslatedStringEditText;

    RepoHandler mRepo;

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
    }
}

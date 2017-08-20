package io.github.lonamiwebs.stringlate.activities.translate;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.TranslationPeekAdapter;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesTranslation;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesTranslationAdapter;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;

public class PeekTranslationsActivity extends AppCompatActivity {

    //region Members

    private RepoHandler mRepo;
    private String mLocale;

    private ListView mTranslationsListView;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peek_translations);

        mTranslationsListView = (ListView) findViewById(R.id.translationsListView);

        Intent intent = getIntent();
        mRepo = RepoHandler.fromBundle(this, intent.getBundleExtra(EXTRA_REPO));
        mLocale = intent.getStringExtra(EXTRA_LOCALE);

        setTitle(String.format("%s/%s", mRepo.getName(), getString(R.string.peek_translations)));

        refreshTranslationsListView();
        mTranslationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                finish();
            }
        });
    }

    //endregion

    //region ListView refreshing

    private void refreshTranslationsListView() {
    }

    //endregion
}

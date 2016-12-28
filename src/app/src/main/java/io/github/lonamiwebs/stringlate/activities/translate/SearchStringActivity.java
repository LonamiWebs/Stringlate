package io.github.lonamiwebs.stringlate.activities.translate;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.LocaleString;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesTranslation;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesTranslationAdapter;
import io.github.lonamiwebs.stringlate.utilities.RepoHandler;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REPO;

public class SearchStringActivity extends AppCompatActivity {

    //region Members

    private RepoHandler mRepo;
    private String mLocale;

    private ListView mResourcesListView;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_string);

        EditText searchEditText = (EditText)findViewById(R.id.searchEditText);
        mResourcesListView = (ListView)findViewById(R.id.resourcesListView);

        Intent intent = getIntent();
        mRepo = RepoHandler.fromBundle(this, intent.getBundleExtra(EXTRA_REPO));
        mLocale = intent.getStringExtra(EXTRA_LOCALE);

        setTitle(String.format("%s/%s (%s)", mRepo.toString(true),
                LocaleString.getDisplay(mLocale), mLocale));

        refreshResourcesListView(null);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable editable) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                refreshResourcesListView(charSequence.toString());
            }
        });

        mResourcesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ResourcesTranslation rt = (ResourcesTranslation)mResourcesListView.getItemAtPosition(i);
                Intent data = new Intent();
                data.putExtra("id", rt.getId());
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    //endregion

    //region ListView refreshing

    private void refreshResourcesListView(String filter) {
        ArrayList<ResourcesTranslation> rts = ResourcesTranslation.fromPairs(
                mRepo.loadResources(null), mRepo.loadResources(mLocale), filter);

        mResourcesListView.setAdapter(new ResourcesTranslationAdapter(
                this, R.layout.item_resource_list, rts));
    }

    //endregion
}

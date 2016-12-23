package io.github.lonamiwebs.stringlate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

import java.util.Locale;

import io.github.lonamiwebs.stringlate.R;

import static io.github.lonamiwebs.stringlate.utilities.Constants.ONLINE_HELP_DEFAULT_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.ONLINE_HELP_INDEX;
import static io.github.lonamiwebs.stringlate.utilities.Constants.ONLINE_HELP_LOCALES;

public class OnlineHelpActivity extends AppCompatActivity {

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_help);

        WebView webview = new WebView(this);
        setContentView(webview);

        String locale = Locale.getDefault().getLanguage();
        if (!helpAvailableForLocale(locale))
            locale = ONLINE_HELP_DEFAULT_LOCALE;

        webview.loadUrl(String.format("file:///android_res/raw/%s.html", locale));
    }

    //endregion

    //region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_help, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Online help
            case R.id.openOnline:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ONLINE_HELP_INDEX)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //endregion

    //region Utilities

    private static boolean helpAvailableForLocale(String locale) {
        for (String l : ONLINE_HELP_LOCALES)
            if (locale.startsWith(l))
                return true;
        return false;
    }

    //endregion
}

package io.github.lonamiwebs.stringlate.activities.info;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

import io.github.lonamiwebs.stringlate.R;

import static io.github.lonamiwebs.stringlate.utilities.Constants.ONLINE_HELP_DEFAULT_LOCALE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.ONLINE_HELP_INDEX;
import static io.github.lonamiwebs.stringlate.utilities.Constants.ONLINE_HELP_LOCALES;

public class BrowserActivity extends AppCompatActivity {
    public final static String EXTRA_DO_SHOW_STRINGLATE_HELP = "EXTRA_DO_SHOW_STRINGLATE_HELP";
    public final static String EXTRA_LOAD_URL = "EXTRA_LOAD_URL";

    //region Initialization
    private WebView webview;
    private boolean isShowingStringlateHelp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_help);
        webview = findViewById(R.id.webview);

        WebSettings webSettings = webview.getSettings();
        webSettings.setAllowFileAccess(false);
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; U; Android 4.4.4; Nexus 5 Build/KTU84P) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        Intent intent = getIntent();
        if (isShowingStringlateHelp = intent.getBooleanExtra(EXTRA_DO_SHOW_STRINGLATE_HELP, false)) {
            webSettings.setAllowFileAccess(true);
            String locale = Locale.getDefault().getLanguage();
            if (!helpAvailableForLocale(locale))
                locale = ONLINE_HELP_DEFAULT_LOCALE;
            webview.loadUrl(String.format("file:///android_res/raw/%s.html", locale));
        }
        if (!TextUtils.isEmpty(intent.getStringExtra(EXTRA_LOAD_URL))) {
            webview.loadUrl(intent.getStringExtra(EXTRA_LOAD_URL));
        }
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
            case R.id.action_open_online_help:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ONLINE_HELP_INDEX)));
                return true;
            case R.id.action_open_in_external_browser:
                if (isShowingStringlateHelp) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ONLINE_HELP_INDEX)));
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webview.getUrl())));
                }
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

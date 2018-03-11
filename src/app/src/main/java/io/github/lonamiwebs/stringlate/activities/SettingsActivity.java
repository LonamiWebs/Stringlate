package io.github.lonamiwebs.stringlate.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.gsantner.opoc.preference.GsPreferenceFragmentCompat;
import net.gsantner.opoc.util.AppSettingsBase;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.git.GitHub;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.utilities.ContextUtils;

@SuppressWarnings("WeakerAccess")
public class SettingsActivity extends AppCompatActivity {
    public static class RESULT {
        public static final int NOCHANGE = -1;
        public static final int CHANGE = 1;
        public static final int CHANGE_RESTART = 2;
    }

    //region Members
    protected Toolbar toolbar;
    public static int activityRetVal = RESULT.NOCHANGE;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        activityRetVal = RESULT.NOCHANGE;
        showFragment(SettingsFragmentMaster.TAG, false);

        // Check if the authorization succeeded
        Uri data = getIntent().getData();
        if (data != null && !TextUtils.isEmpty(data.getQueryParameter("code"))) {
            Toast.makeText(this, R.string.completing_auth_ellipsis, Toast.LENGTH_SHORT).show();
            new GitHubCompleteAuthTask(findViewById(android.R.id.content), data.getQueryParameter("code")).execute();
        }
    }


    protected void showFragment(String tag, boolean addToBackStack) {
        GsPreferenceFragmentCompat fragment = (GsPreferenceFragmentCompat) getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            switch (tag) {
                case SettingsFragmentMaster.TAG:
                default:
                    fragment = new SettingsFragmentMaster();
                    toolbar.setTitle(R.string.settings);
                    break;
            }
        }
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        if (addToBackStack) {
            t.addToBackStack(tag);
        }
        t.replace(R.id.settings__activity__fragment_placeholder, fragment, tag).commit();
    }

    public static abstract class StringlateSettingsFragment extends GsPreferenceFragmentCompat {
        protected AppSettings _as;

        @Override
        protected AppSettingsBase getAppSettings(Context context) {
            if (_as == null) {
                _as = new AppSettings(context);
            }
            return _as;
        }

        @Override
        public Integer getIconTintColor() {
            return Color.BLACK;
        }

        @Override
        protected void onPreferenceScreenChanged(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
            super.onPreferenceScreenChanged(preferenceFragmentCompat, preferenceScreen);
            if (!TextUtils.isEmpty(preferenceScreen.getTitle())) {
                SettingsActivity a = (SettingsActivity) getActivity();
                if (a != null) {
                    a.toolbar.setTitle(preferenceScreen.getTitle());
                }
            }
        }
    }


    public static class SettingsFragmentMaster extends StringlateSettingsFragment {
        public static final String TAG = "SettingsFragmentMaster";

        @Override
        public int getPreferenceResourceForInflation() {
            return R.xml.preferences_master;
        }

        @Override
        public String getFragmentTag() {
            return TAG;
        }

        @Override
        public Boolean onPreferenceClicked(Preference preference) {
            if (isAdded() && preference.hasKey()) {
                String key = preference.getKey();

                if (key.equals(getString(R.string.pref_key__github_authentication_request))) {
                    Uri url = Uri.parse(GitHub.Authentication.getAuthRequestUrl());
                    startActivity(new Intent(Intent.ACTION_VIEW, url));
                }

                if (key.equals(getString(R.string.pref_key__language))) {
                    activityRetVal = RESULT.CHANGE_RESTART;
                }
            }
            return false;
        }

        @Override
        public void updateSummaries() {
            if (isAdded() && !isDetached()) {
                updateSummary(R.string.pref_key__github_authentication_request, getString(_as.hasGitHubAuthorization()
                        ? R.string.github_yes_logged_long : R.string.github_not_logged_long)
                );
            }
        }

        @Override
        protected void onPreferenceChanged(SharedPreferences prefs, String key) {
            updateSummaries();
            activityRetVal = activityRetVal != RESULT.CHANGE_RESTART ? RESULT.CHANGE : activityRetVal;
        }
    }
}

class GitHubCompleteAuthTask extends AsyncTask<Void, Void, GitHub.Authentication.CompleteAuthenticationResult> {
    private final View mViewRoot;
    private final Context mContext;
    private final String mCode;

    GitHubCompleteAuthTask(View viewRoot, String code) {
        mCode = code;
        mViewRoot = viewRoot;
        mContext = viewRoot.getContext();
    }

    @Override
    protected GitHub.Authentication.CompleteAuthenticationResult doInBackground(Void... params) {
        AppSettings appSettings = new AppSettings(mContext);
        return GitHub.Authentication.completeGitHubAuth(
                appSettings, appSettings.getGitHubClientId(),
                appSettings.getGitHubClientSecret(), mCode
        );
    }

    @Override
    protected void onPostExecute(GitHub.Authentication.CompleteAuthenticationResult result) {
        // NOTE: Preference Summary UI gets updated by preference changed event
        Spanned message = new ContextUtils(mViewRoot.getContext()).htmlToSpanned("<strong>GitHub " +
                mViewRoot.getContext().getString(result.ok ? R.string.auth_success : R.string.auth_failure)
                + "</strong>" + (result.message != null ? ("<br/>" + result.message) : "")
        );

        Snackbar snackbar = Snackbar.make(mViewRoot, message, Snackbar.LENGTH_LONG);
        TextView snackTextView = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackTextView.setMaxLines(6);
        snackbar.show();
    }
}
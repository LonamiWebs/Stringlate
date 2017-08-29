package io.github.lonamiwebs.stringlate.activities;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import io.github.gsantner.opoc.util.Helpers;
import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.git.GitHub;
import io.github.lonamiwebs.stringlate.settings.AppSettings;

import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_CLIENT_ID;
import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_CLIENT_SECRET;

@SuppressWarnings("WeakerAccess")
public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static class RESULT {
        public static final int NOCHANGE = -1;
        public static final int CHANGE = 1;
        public static final int CHANGE_RESTART = 2;
    }

    //region Members
    protected AppSettings mSettings;
    protected Toolbar toolbar;
    public static int activityRetVal = RESULT.NOCHANGE;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSettings = new AppSettings(this);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_black_48px));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SettingsActivity.this.onBackPressed();
            }
        });
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
        PreferenceFragment fragment = (PreferenceFragment) getFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            switch (tag) {
                case SettingsFragmentMaster.TAG:
                default:
                    fragment = new SettingsFragmentMaster();
                    toolbar.setTitle(R.string.settings);
                    break;
            }
        }
        FragmentTransaction t = getFragmentManager().beginTransaction();
        if (addToBackStack) {
            t.addToBackStack(tag);
        }
        t.replace(R.id.settings__fragment_container, fragment, tag).commit();
    }

    @Override
    protected void onResume() {
        mSettings.registerPreferenceChangedListener(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mSettings.unregisterPreferenceChangedListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (activityRetVal == RESULT.NOCHANGE) {
            activityRetVal = RESULT.CHANGE;
        }
    }


    public static class SettingsFragmentMaster extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public static final String TAG = "SettingsFragmentMaster";
        private AppSettings mSettings;

        public void onCreate(Bundle savedInstances) {
            super.onCreate(savedInstances);
            mSettings = new AppSettings(getActivity());
            getPreferenceManager().setSharedPreferencesName(mSettings.getDefaultPreferencesName());
            addPreferencesFromResource(R.xml.preferences_master);
            SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);

            // UpdateSummaries may not work immediately
            final Activity activity = getActivity();
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(150);
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                updateSummaries();
                            }
                        });
                    } catch (Exception ignored) {
                    }
                }
            }.start();
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
            if (isAdded() && preference.hasKey()) {
                String key = preference.getKey();

                if (key.equals(getString(R.string.pref_key__github_authentication_request))) {
                    Uri url = Uri.parse(GitHub.Authentication.gGetAuthRequestUrl());
                    startActivity(new Intent(Intent.ACTION_VIEW, url));
                }

                if (key.equals(getString(R.string.pref_key__language))) {
                    activityRetVal = RESULT.CHANGE_RESTART;
                }
            }
            return super.onPreferenceTreeClick(screen, preference);
        }

        private void updateSummaries() {
            if (isAdded() && !isDetached()) {
                findPreference(getString(R.string.pref_key__github_authentication_request)).setSummary(
                        mSettings.hasGitHubAuthorization()
                                ? R.string.github_yes_logged_long
                                : R.string.github_not_logged_long
                );
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            updateSummaries();
        }
    }
}

class GitHubCompleteAuthTask extends AsyncTask<Void, Void, GitHub.Authentication.CompleteAuthenticationResult> {
    private final View mViewRoot;
    private final String mCode;

    GitHubCompleteAuthTask(View viewRoot, String code) {
        mCode = code;
        mViewRoot = viewRoot;
    }

    @Override
    protected GitHub.Authentication.CompleteAuthenticationResult doInBackground(Void... params) {
        return GitHub.Authentication.gCompleteGitHubAuth(
                new AppSettings(mViewRoot.getContext()), GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, mCode
        );
    }

    @Override
    protected void onPostExecute(GitHub.Authentication.CompleteAuthenticationResult result) {
        // NOTE: Preference Summary UI gets updated by preference changed event
        Spanned message = new Helpers(mViewRoot.getContext()).htmlToSpanned("<strong>GitHub " +
                mViewRoot.getContext().getString(result.ok ? R.string.auth_success : R.string.auth_failure)
                + "</strong>" + (result.message != null ? ("<br/>" + result.message) : "")
        );

        Snackbar snackbar = Snackbar.make(mViewRoot, message, Snackbar.LENGTH_LONG);
        TextView snackTextView = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackTextView.setMaxLines(6);
        snackbar.show();
    }
}
package io.github.lonamiwebs.stringlate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.settings.AppSettings;
import io.github.lonamiwebs.stringlate.tasks.HttpPostTask;

import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_AUTH_URL;
import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_CLIENT_ID;
import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_CLIENT_SECRET;
import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_COMPLETE_AUTH_URL;
import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_WANTED_SCOPES;

public class GitHubLoginActivity extends AppCompatActivity {

    //region Members

    AppSettings mSettings;
    TextView mInfoTextView;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_github_login);

        mSettings = new AppSettings(this);
        mInfoTextView = (TextView)findViewById(R.id.infoTextView);

        // Check if the authorization succeeded
        Uri data = getIntent().getData();
        if (data != null) {
            completeAuth(data.getQueryParameter("code"));
        } else {
            checkAuthorization();
        }
    }

    //endregion

    //region Button events

    public void onRequestAuthClick(final View view) {
        Uri url = Uri.parse(String.format(GITHUB_AUTH_URL,
                TextUtils.join("%20", GITHUB_WANTED_SCOPES), GITHUB_CLIENT_ID));

        startActivity(new Intent(Intent.ACTION_VIEW, url));
    }

    //endregion

    //region Private utilities

    // Update the UI accordingly to the saved settings
    private void checkAuthorization() {
        if (mSettings.getGitHubScopes().length == GITHUB_WANTED_SCOPES.length) {
            mInfoTextView.setText(R.string.github_yes_logged_long);
        } else {
            mInfoTextView.setText(R.string.github_not_logged_long);
        }
    }

    // Complete the authorization process
    private void completeAuth(String code) {
        Toast.makeText(this, R.string.completing_auth_ellipsis, Toast.LENGTH_SHORT).show();
        HashMap<String, String> map = new HashMap<>();

        map.put("client_id", GITHUB_CLIENT_ID);
        map.put("client_secret", GITHUB_CLIENT_SECRET);
        map.put("code", code);

        new HttpPostTask(map) {
            @Override
            protected void onPostExecute(HashMap<String, String> map) {
                super.onPostExecute(map);
                if (map.containsKey("error")) {
                    Toast.makeText(getApplicationContext(), map.get("error_description"), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.auth_success, Toast.LENGTH_SHORT).show();
                    String token = map.get("access_token");
                    String grantedScopes = map.get("scope");
                    mSettings.setGitHubAccess(token, grantedScopes);
                    checkAuthorization();
                }
            }
        }.execute(GITHUB_COMPLETE_AUTH_URL);
    }

    //endregion
}

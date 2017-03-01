package io.github.lonamiwebs.stringlate.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import io.github.lonamiwebs.stringlate.utilities.Api;

public class StringlateClientDemoActivity extends Activity {
    private static final String LOG_CONTEXT = "StringlateDemo";
    private static final int INSTALL_REQUEST_CODE = 2281;

    private EditText edUri          ;
    private TextView lblStatus      ;
    private Button   btnTranslate   ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stringlate_client_demo);

        edUri          = (EditText) findViewById(R.id.editUrl);
        lblStatus      = (TextView) findViewById(R.id.lblStatus);
        btnTranslate   = (Button  ) findViewById(R.id.btnTranslate);

        btnTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTranslate();
            }
        });
    }

    private void onTranslate() {
        if (!Api.isInstalled(this)) {
            // either ask or catch ActivityNotFoundException

            if (Api.canInstall(this)) {
                // either ask or catch ActivityNotFoundException
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.settings_translate_title);
                builder.setMessage(R.string.message_translate_not_installed)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            final DialogInterface dialog,
                                            final int id) {
                                        Log.i(LOG_CONTEXT, "SettingsActivity-Stringlate-start-install");
                                        Api.install(StringlateClientDemoActivity.this, INSTALL_REQUEST_CODE);
                                    }
                                }
                        )
                        .setNegativeButton(android.R.string.no,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            final DialogInterface dialog,
                                            final int id) {
                                        dialog.cancel();
                                    }
                                }
                        );

                builder.create().show();
            } else { // neither stringlate nor f-droid-app-store installed
                Log.i(LOG_CONTEXT, "SettingsActivity-Stringlate-install-appstore not found");
            }
        } else { // stringlate already installed
            translate();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INSTALL_REQUEST_CODE) {
            if (Api.isInstalled(this)) {
                Log.i(LOG_CONTEXT, "SettingsActivity-Stringlate-install-success");
                translate();
            } else {
                Log.i(LOG_CONTEXT, "SettingsActivity-Stringlate-install-error or canceled");
            }
        }
    }

    private void translate() {
        Log.i(LOG_CONTEXT, "SettingsActivity-Stringlate-install-success");
        Api.translate(this, edUri.getText().toString());
    }
}

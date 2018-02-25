package io.github.lonamiwebs.stringlate.activities.export;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.gsantner.opoc.util.Callback;

import io.github.lonamiwebs.stringlate.R;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_ID;

public class CreateUrlActivity extends AppCompatActivity {

    //region Members

    private String mPostedUrl;
    private Exporter.CallableExporter mPostUrlCallable;

    private LinearLayout mSuccessLinearLayout;
    private TextView mInfoTextView;
    private ProgressBar mIndeterminateBar;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_url);

        mSuccessLinearLayout = findViewById(R.id.successLinearLayout);
        mInfoTextView = findViewById(R.id.infoTextView);
        mIndeterminateBar = findViewById(R.id.indeterminateBar);

        Intent intent = getIntent();
        mPostUrlCallable = Exporter.getExporter(intent.getIntExtra(EXTRA_ID, 0));
        if (mPostUrlCallable == null) {
            Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            mInfoTextView.setText(mPostUrlCallable.getDescription(this));

            new AsyncTask<Void, String, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try {
                        return mPostUrlCallable.call(CreateUrlActivity.this, new Callback<String>() {
                            @Override
                            public void onCallback(String s) {
                                publishProgress(s);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                protected void onProgressUpdate(String... values) {
                    mInfoTextView.setText(values[0]);
                }

                @Override
                protected void onPostExecute(final String postedUrl) {
                    if (postedUrl == null) {
                        if (mPostUrlCallable.mFailureReason.isEmpty())
                            mPostUrlCallable.mFailureReason = getString(R.string.something_went_wrong);

                        Toast.makeText(CreateUrlActivity.this,
                                mPostUrlCallable.mFailureReason, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        mPostedUrl = postedUrl;
                        mIndeterminateBar.setVisibility(View.GONE);
                        mSuccessLinearLayout.setVisibility(View.VISIBLE);

                        mInfoTextView.setText(mPostUrlCallable.getSuccessDescription(CreateUrlActivity.this));
                        ((EditText) findViewById(R.id.postedUrlEditText)).setText(mPostedUrl);
                    }
                }
            }.execute();
        }
    }

    public static void launchIntent(final Context ctx, final int exporterHandle) {
        ctx.startActivity(new Intent(ctx, CreateUrlActivity.class)
                .putExtra(EXTRA_ID, exporterHandle));
    }

    //endregion

    //region Button events

    public void onOpenUrl(final View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mPostedUrl));
        startActivity(browserIntent);
    }

    public void onCopyUrl(final View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("url", mPostedUrl));
        Toast.makeText(this, R.string.url_copied, Toast.LENGTH_SHORT).show();
    }

    public void onShareUrl(final View v) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, mPostedUrl);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_url)));
    }

    public void onExit(final View v) {
        finish();
    }

    //endregion
}

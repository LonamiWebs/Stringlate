package io.github.lonamiwebs.stringlate.activities.export;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.github.lonamiwebs.stringlate.R;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_DESCRIPTION;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REMOTE_PATH;

public class CreateUrlSuccessActivity extends AppCompatActivity {

    //region Members

    String mPostedUrl;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_url_success);

        Intent intent = getIntent();
        mPostedUrl = intent.getStringExtra(EXTRA_REMOTE_PATH);
        ((EditText)findViewById(R.id.postedUrlEditText)).setText(mPostedUrl);
        ((TextView)findViewById(R.id.postSuccessTextView)).setText(
                intent.getStringExtra(EXTRA_DESCRIPTION));
    }

    public static void launchIntent(Context ctx, String description, String postedUrl) {
        Intent intent = new Intent(ctx, CreateUrlSuccessActivity.class);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        intent.putExtra(EXTRA_REMOTE_PATH, postedUrl);
        ctx.startActivity(intent);
    }

    //endregion

    //region Button events

    public void onOpenUrl(final View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mPostedUrl));
        startActivity(browserIntent);
    }

    public void onCopyUrl(final View v) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
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

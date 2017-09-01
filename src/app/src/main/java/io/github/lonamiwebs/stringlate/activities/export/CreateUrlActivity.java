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

import java.util.concurrent.Callable;

import io.github.lonamiwebs.stringlate.R;

import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_DESCRIPTION;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_ID;
import static io.github.lonamiwebs.stringlate.utilities.Constants.EXTRA_REMOTE_PATH;

public class CreateUrlActivity extends AppCompatActivity {

    //region Members

    private String mPostedUrl;
    private Callable<String> mPostUrlCallable;

    //endregion

    //region Initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_url_success);

        Intent intent = getIntent();
        mPostUrlCallable = Exporter.getExporter(intent.getIntExtra(EXTRA_ID, 0));
        if (mPostUrlCallable == null) {
            // TODO Notify something went wrong
        } else {
            // TODO Run the callable on a background task
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

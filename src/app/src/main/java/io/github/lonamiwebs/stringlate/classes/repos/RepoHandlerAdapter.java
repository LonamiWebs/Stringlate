package io.github.lonamiwebs.stringlate.classes.repos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import io.github.lonamiwebs.stringlate.R;

import static io.github.lonamiwebs.stringlate.utilities.Constants.MATERIAL_COLORS;

public class RepoHandlerAdapter extends ArrayAdapter<RepoHandler> {

    private final int mSize; // Used to generate the image
    private final List<Float> mCustomProgress; // Override the progress shown

    // https://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
    private static class ViewHolder {
        ImageView iconView;
        TextView pathTextView, hostTextView, translatedProgressTextView;
        ProgressBar translatedProgressBar;
    }

    public RepoHandlerAdapter(final Context context, final List<RepoHandler> repositories) {
        super(context, R.layout.item_repository_list, repositories);
        mSize = context.getResources().getDisplayMetrics().densityDpi;
        mCustomProgress = null;
    }

    public RepoHandlerAdapter(final Context context, final List<RepoHandler> repositories,
                              final List<Float> customProgress) {
        super(context, R.layout.item_repository_list, repositories);
        mSize = context.getResources().getDisplayMetrics().densityDpi;
        mCustomProgress = customProgress;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        RepoHandler repo = getItem(position);

        // This may be the first time we use the recycled view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_repository_list, parent, false);

            final ViewHolder holder = new ViewHolder();
            holder.iconView = convertView.findViewById(R.id.repositoryIcon);
            holder.pathTextView = convertView.findViewById(R.id.repositoryPath);
            holder.hostTextView = convertView.findViewById(R.id.repositoryHost);
            holder.translatedProgressTextView =
                    convertView.findViewById(R.id.translatedProgressText);

            holder.translatedProgressBar =
                    convertView.findViewById(R.id.translatedProgressBar);

            convertView.setTag(holder);
        }
        if (repo != null) {
            final ViewHolder holder = (ViewHolder) convertView.getTag();
            File iconFile = repo.settings.getIconFile();
            if (iconFile == null)
                holder.iconView.setImageBitmap(getBitmap(repo.getProjectName()));
            else
                holder.iconView.setImageURI(Uri.fromFile(iconFile));

            holder.pathTextView.setText(repo.getProjectName());
            holder.hostTextView.setText(repo.getHost());

            Float progressPercent;
            if (mCustomProgress == null) {
                RepoProgress progress = repo.loadProgress();
                progressPercent = progress == null ? null : progress.getPercentage();
            } else {
                progressPercent = mCustomProgress.get(position) * 100f;
            }

            if (progressPercent == null) {
                holder.translatedProgressBar.setVisibility(View.INVISIBLE);
                holder.translatedProgressTextView.setVisibility(View.GONE);
            } else {
                holder.translatedProgressBar.setVisibility(View.VISIBLE);
                holder.translatedProgressTextView.setVisibility(View.VISIBLE);

                // Just some very large number since the progressbar doesn't support floats
                holder.translatedProgressBar.setMax(1000000);
                holder.translatedProgressBar.setProgress((int)(progressPercent * 10000f));
                holder.translatedProgressTextView.setText(
                        String.format(Locale.ENGLISH, "%.1f%%", progressPercent));
            }
        }
        return convertView;
    }

    private Bitmap getBitmap(String name) {
        Random random = new Random(name.hashCode());
        Bitmap bitmap = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);

        // Let the name be the first and last capital letters
        Character first = null;
        Character last = null;
        char c;
        for (int i = 0; i < name.length(); i++) {
            c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (first == null)
                    first = c;
                else
                    last = c;
            }
        }
        if (first == null) {
            name = String.valueOf(name.charAt(0)).toUpperCase();
        } else {
            name = String.valueOf(first);
            if (last != null)
                name += String.valueOf(last);
        }

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(MATERIAL_COLORS[random.nextInt(MATERIAL_COLORS.length)]);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint);

        // Center text: http://stackoverflow.com/a/11121873/4759433
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setTextSize(mSize / (float) name.length());
        paint.setTextAlign(Paint.Align.CENTER);

        float xPos = (canvas.getWidth() / 2f);
        float yPos = (canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);

        canvas.drawText(name, xPos, yPos, paint);
        return bitmap;
    }
}

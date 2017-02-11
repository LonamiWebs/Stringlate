package io.github.lonamiwebs.stringlate.classes.repos;

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
import android.widget.TextView;

import java.io.File;
import java.util.List;
import java.util.Random;

import io.github.lonamiwebs.stringlate.R;

import static io.github.lonamiwebs.stringlate.utilities.Constants.MATERIAL_COLORS;

public class RepoHandlerAdapter extends ArrayAdapter<RepoHandler> {

    private final int mSize; // Used to generate the image

    // https://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
    private static class ViewHolder {
        ImageView iconView;
        TextView pathTextView, hostTextView;
    }

    public RepoHandlerAdapter(Context context, List<RepoHandler> repositories) {
        // Treat the repositories like applications
        // We can show an icon, the title, and the host as description
        super(context, R.layout.item_application_list, repositories);
        mSize = context.getResources().getDisplayMetrics().densityDpi;
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        RepoHandler repo = getItem(position);

        // This may be the first time we use the recycled view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_application_list, parent, false);

            final ViewHolder holder = new ViewHolder();
            holder.iconView = (ImageView)convertView.findViewById(R.id.appIcon);
            holder.pathTextView = (TextView)convertView.findViewById(R.id.appName);
            holder.hostTextView = (TextView)convertView.findViewById(R.id.appDescription);
            convertView.setTag(holder);
        }
        if (repo != null) {
            final ViewHolder holder = (ViewHolder)convertView.getTag();
            File iconFile = repo.getIconFile();
            if (iconFile == null)
                holder.iconView.setImageBitmap(getBitmap(repo.getName(false)));
            else
                holder.iconView.setImageURI(Uri.fromFile(iconFile));

            holder.pathTextView.setText(repo.getPath());
            holder.hostTextView.setText(repo.getHost());
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
        paint.setTextSize(mSize / (float)name.length());
        paint.setTextAlign(Paint.Align.CENTER);

        float xPos = (canvas.getWidth() / 2f);
        float yPos = (canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);

        canvas.drawText(name, xPos, yPos, paint);
        return bitmap;
    }
}

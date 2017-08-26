package io.github.lonamiwebs.stringlate.classes.repos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.activities.translate.TranslateActivity;

import static io.github.lonamiwebs.stringlate.utilities.Constants.MATERIAL_COLORS;

public class RepoHandlerAdapter extends RecyclerView.Adapter<RepoHandlerAdapter.ViewHolder> {

    private final int mSize; // Used to generate default images
    private final Context mContext;

    // TODO Use a better approach than using two separate lists
    // Cannot use HashMap<RepoHandler, Float> since the repositories have an order
    private final ArrayList<RepoHandler> mRepositories = new ArrayList<>();
    private final ArrayList<Float> mCustomProgress = new ArrayList<>();

    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout root;
        final ImageView iconView;
        final TextView pathTextView, hostTextView, translatedProgressTextView;
        final ProgressBar translatedProgressBar;

        ViewHolder(final LinearLayout root) {
            super(root);
            this.root = root;

            iconView = root.findViewById(R.id.repositoryIcon);
            pathTextView = root.findViewById(R.id.repositoryPath);
            hostTextView = root.findViewById(R.id.repositoryHost);
            translatedProgressTextView = root.findViewById(R.id.translatedProgressText);
            translatedProgressBar = root.findViewById(R.id.translatedProgressBar);
        }

        void update(RepoHandler repo, int bitmapDpiSize) {
            File iconFile = repo.settings.getIconFile();
            if (iconFile == null) {
                final String name = repo.getProjectName();
                if (!name.equals(iconView.getTag())) {
                    iconView.setImageBitmap(getBitmap(name, bitmapDpiSize));
                    iconView.setTag(name);
                }
            }
            else {
                if (!iconFile.equals(iconView.getTag())) {
                    iconView.setImageURI(Uri.fromFile(iconFile));
                    iconView.setTag(iconFile);
                }
            }

            pathTextView.setText(repo.getProjectName());
            hostTextView.setText(repo.getHost());
        }

        void updateProgress(Float progress) {
            if (progress == null) {
                translatedProgressBar.setVisibility(View.INVISIBLE);
                translatedProgressTextView.setVisibility(View.GONE);
            } else {
                translatedProgressBar.setVisibility(View.VISIBLE);
                translatedProgressTextView.setVisibility(View.VISIBLE);

                // Just some very large number since the progressbar doesn't support floats
                translatedProgressBar.setMax(1000000);
                translatedProgressBar.setProgress((int)(progress * 1000000));
                translatedProgressTextView.setText(
                        String.format(Locale.ENGLISH, "%.1f%%", 100f * progress)
                );
            }
        }
    }

    public RepoHandlerAdapter(final Context context) {
        mSize = context.getResources().getDisplayMetrics().densityDpi;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        LinearLayout root = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repository_list, parent, false);

        // TODO Set the view's size, margins, paddings and layout parameters, I gotta?
        return new ViewHolder(root);
    }

    @Override
    public void onBindViewHolder(final ViewHolder repoHandler, int i) {
        repoHandler.update(mRepositories.get(i), mSize);

        // Determine the right progress to be used
        Float progress = mCustomProgress.get(i);
        if (progress == null) {
            final RepoProgress repoProgress = mRepositories.get(i).loadProgress();
            progress = repoProgress == null ? null : repoProgress.getProgress();
        }
        if (progress != null)
            repoHandler.updateProgress(progress);

        repoHandler.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TranslateActivity.launch(mContext, mRepositories.get(
                        repoHandler.getAdapterPosition())
                );
            }
        });

        // TODO Context menu on long click
    }

    @Override
    public int getItemCount() {
        return mRepositories.size();
    }

    // Returns true if there are items left, or false otherwise
    public boolean notifyDataSetChanged(final ArrayList<RepoHandler> repositories) {
        mRepositories.clear();
        mCustomProgress.clear();
        for (RepoHandler repo : repositories) {
            mRepositories.add(repo);
            mCustomProgress.add(null);
        }
        Collections.sort(mRepositories);
        notifyDataSetChanged();
        return !mRepositories.isEmpty();
    }

    public void notifyItemAdded(final RepoHandler which) {
        // Latest one, add it to the top (as in "newest")
        mRepositories.add(0, which);
        mCustomProgress.add(0, null);
        notifyDataSetChanged();
    }

    // Returns true if there are items left, or false otherwise
    public boolean notifyItemRemoved(final RepoHandler which) {
        for (int i = mRepositories.size(); i-- != 0;) {
            if (mRepositories.get(i) == which) {
                mRepositories.remove(i);
                mCustomProgress.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }

        return !mRepositories.isEmpty();
    }

    // Note that when the progress reaches 1f, the item  will be removed
    // Returns true if there are items left, or false otherwise
    public boolean notifyItemProgressChanged(final RepoHandler which, float progress) {
        if (progress == 1f) {
            // Remove this repository
            return notifyItemRemoved(which);
        } else {
            if (mRepositories.contains(which)) {
                // Update the progress of an existing repository
                for (int i = mRepositories.size(); i-- != 0;) {
                    if (mRepositories.get(i) == which) {
                        mCustomProgress.set(i, progress);
                        notifyItemChanged(i);
                        break;
                    }
                }
            } else {
                // Latest one, add it to the top (as in "newest")
                mRepositories.add(0, which);
                mCustomProgress.add(0, progress);
                notifyItemInserted(0);
            }
            return true;
        }
    }

    private static Bitmap getBitmap(String name, int size) {
        Random random = new Random(name.hashCode());
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

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
        paint.setTextSize(size / (float) name.length());
        paint.setTextAlign(Paint.Align.CENTER);

        float xPos = (canvas.getWidth() / 2f);
        float yPos = (canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);

        canvas.drawText(name, xPos, yPos, paint);
        return bitmap;
    }
}

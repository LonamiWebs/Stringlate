package io.github.lonamiwebs.stringlate.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
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
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.classes.repos.RepoProgress;

import static io.github.lonamiwebs.stringlate.utilities.Constants.MATERIAL_COLORS;

public class RepoHandlerAdapter extends RecyclerView.Adapter<RepoHandlerAdapter.ViewHolder> {

    private final int mSize; // Used to generate default images
    private final Context mContext;

    private RepoHandler mContextMenuRepo;

    // We actually have two RecyclerViews in one with a custom separator.
    // Simply, if we have any repositories synchronizing, we'll add an extra item which
    // will behave as a separator, and this separator (which will always be on
    // position mRepositories.size()) will also be gone when no repositories are
    // being synchronized.
    private final ArrayList<RepoHandler> mRepositories = new ArrayList<>();
    private final ArrayList<Pair<RepoHandler, Float>> mSyncingRepositories = new ArrayList<>();

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        final LinearLayout root, repositoryLayout;
        final ImageView iconView;
        final TextView pathTextView, hostTextView, translatedProgressTextView, separatorTextView;
        final ProgressBar translatedProgressBar;

        boolean showMenu;

        ViewHolder(final LinearLayout root) {
            super(root);
            this.root = root;

            repositoryLayout = root.findViewById(R.id.repositoryLayout);
            iconView = root.findViewById(R.id.repositoryIcon);
            pathTextView = root.findViewById(R.id.repositoryPath);
            hostTextView = root.findViewById(R.id.repositoryHost);
            translatedProgressTextView = root.findViewById(R.id.translatedProgressText);
            translatedProgressBar = root.findViewById(R.id.translatedProgressBar);

            separatorTextView = root.findViewById(R.id.separatorTextView);
        }

        void update(RepoHandler repo, int bitmapDpiSize) {
            setIsSeparator(false);
            File iconFile = repo.settings.getIconFile();
            if (iconFile == null) {
                final String name = repo.getProjectName();
                if (!name.equals(iconView.getTag())) {
                    iconView.setImageBitmap(getBitmap(name, bitmapDpiSize));
                    iconView.setTag(name);
                }
            } else {
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
                translatedProgressBar.setProgress((int) (progress * 1000000));
                translatedProgressTextView.setText(
                        String.format(Locale.ENGLISH, "%.1f%%", 100f * progress)
                );
            }
        }

        void setIsSeparator(final boolean isSeparator) {
            if (isSeparator) {
                repositoryLayout.setVisibility(View.GONE);
                separatorTextView.setVisibility(View.VISIBLE);
                showMenu = false;
            } else {
                repositoryLayout.setVisibility(View.VISIBLE);
                separatorTextView.setVisibility(View.GONE);
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(Menu.NONE, R.id.syncRepo, Menu.NONE, R.string.update_strings);
            menu.add(Menu.NONE, R.id.importRepo, Menu.NONE, R.string.import_repository);
            menu.add(Menu.NONE, R.id.exportRepo, Menu.NONE, R.string.export_repository);
            menu.add(Menu.NONE, R.id.deleteRepo, Menu.NONE, R.string.delete_repository);
        }
    }

    public RepoHandlerAdapter(final Context context) {
        mSize = context.getResources().getDisplayMetrics().densityDpi;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int i) {
        return new ViewHolder((LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repository_list, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder view, int i) {
        if (i < mRepositories.size()) {
            final RepoProgress progress = mRepositories.get(i).loadProgress();
            view.update(mRepositories.get(i), mSize);
            view.updateProgress(progress == null ? null : progress.getProgress());
            view.showMenu = true;

            view.root.setOnClickListener(v -> TranslateActivity.launch(mContext, mRepositories.get(
                    view.getAdapterPosition())
            ));

            view.root.setOnLongClickListener(v -> {
                mContextMenuRepo = mRepositories.get(view.getAdapterPosition());
                return false;
            });
        } else if (i == mRepositories.size()) {
            // Separator view on the edge case
            view.setIsSeparator(true);
        } else {
            i -= mRepositories.size() + 1;
            final Pair<RepoHandler, Float> repo = mSyncingRepositories.get(i);
            view.update(repo.first, mSize);
            view.updateProgress(repo.second);
            view.root.setOnClickListener(view1 -> {
                AlertDialog.Builder dialog = new AlertDialog.Builder(mContext)
                        .setPositiveButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dialogInterface, i1) -> repo.first.cancel())
                        .setTitle(R.string.cancel_sync);
                dialog.show();
            });
            view.showMenu = false;
        }
    }

    @Override
    public void onViewRecycled(final ViewHolder view) {
        view.root.setOnClickListener(null);
        view.root.setOnLongClickListener(null);
        super.onViewRecycled(view);
    }

    @Override
    public int getItemCount() {
        if (mSyncingRepositories.isEmpty())
            return mRepositories.size();
        else
            // +1 for the separator view on the edge case
            return mRepositories.size() + 1 + mSyncingRepositories.size();
    }

    public RepoHandler getContextMenuRepository() {
        // Reason for saving menu position like this: https://stackoverflow.com/a/27886458
        return mContextMenuRepo;
    }

    // Returns true if there are items left, or false otherwise
    public boolean notifyDataSetChanged(final ArrayList<RepoHandler> repositories) {
        mRepositories.clear();
        mRepositories.addAll(repositories);

        Collections.sort(mRepositories);
        notifyDataSetChanged();
        return !mRepositories.isEmpty();
    }

    public void notifyRepoAdded(final RepoHandler which) {
        // If it was added already it, remove it, since we're moving it to the top
        if (mRepositories.contains(which))
            notifyRepoRemoved(which);

        // Latest one, add it to the top (as in "newest")
        mRepositories.add(0, which);
        notifyDataSetChanged();
    }

    // Returns true if there are items left, or false otherwise
    public boolean notifyRepoRemoved(final RepoHandler which) {
        for (int i = mRepositories.size(); i-- != 0; ) {
            if (mRepositories.get(i).equals(which)) {
                mRepositories.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }

        return !mRepositories.isEmpty();
    }

    public void notifySyncingProgressChanged(final RepoHandler which, float progress) {
        boolean updated = false;
        // Update the progress of an existing repository, if any
        for (int i = mSyncingRepositories.size(); i-- != 0; ) {
            if (mSyncingRepositories.get(i).first.equals(which)) {
                mSyncingRepositories.set(i, new Pair<>(
                        mSyncingRepositories.get(i).first, progress
                ));
                notifyItemChanged(mRepositories.size() + 1 + i);
                updated = true;
                break;
            }
        }
        if (!updated) {
            // Latest one, add it to the top (as in "newest")
            mSyncingRepositories.add(0, new Pair<>(
                    which, progress
            ));
            notifyDataSetChanged();
        }
    }

    // Returns true if there are items left, or false otherwise
    public boolean notifySyncingRepoFinished(final RepoHandler which) {
        for (int i = mSyncingRepositories.size(); i-- != 0; ) {
            if (mSyncingRepositories.get(i).first.equals(which)) {
                mSyncingRepositories.remove(i);
                notifyItemRemoved(mRepositories.size() + 1 + i);
                break;
            }
        }
        if (mSyncingRepositories.isEmpty()) {
            // Also notify that the separator has been removed
            notifyItemRemoved(mRepositories.size());
            return false;
        } else {
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
            if (name.isEmpty()) {
                name = "";
            } else {
                name = String.valueOf(name.charAt(0)).toUpperCase();
            }
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

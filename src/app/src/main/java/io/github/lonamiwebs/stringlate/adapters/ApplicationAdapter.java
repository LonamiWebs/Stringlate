package io.github.lonamiwebs.stringlate.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.applications.ApplicationDetails;
import io.github.lonamiwebs.stringlate.classes.applications.ApplicationList;
import io.github.lonamiwebs.stringlate.classes.applications.ApplicationsSyncTask;
import io.github.lonamiwebs.stringlate.classes.lazyloader.ImageLoader;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.github.lonamiwebs.stringlate.utilities.Constants.DEFAULT_APPS_LIMIT;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {
    private final ImageLoader mImageLoader;
    private final ApplicationList mApplicationList;
    private ArrayList<ApplicationDetails> appsSlice;

    public interface OnItemClick {
        void onClick(Intent data);
    }

    public OnItemClick onItemClick;

    class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout root;
        final ImageView iconView;
        final TextView appName, appDescription;
        final View installIndicator;

        ViewHolder(final LinearLayout root) {
            super(root);
            this.root = root;
            iconView = root.findViewById(R.id.appIcon);
            appName = root.findViewById(R.id.appName);
            appDescription = root.findViewById(R.id.appDescription);
            installIndicator = root.findViewById(R.id.installIndicatorView);
        }

        void update(final ApplicationDetails app) {
            appName.setText(app.getName());
            appDescription.setText(app.getDescription());
            installIndicator.setVisibility(app.isInstalled() ? VISIBLE : GONE);
            mImageLoader.loadImageAsync(iconView,
                    app.getIconUrl(), app.isInstalled() ? app.getPackageName() : null
            );
        }
    }

    public ApplicationAdapter(final Context context,
                              final boolean allowInternetDownload) {
        mImageLoader = new ImageLoader(context, allowInternetDownload);
        mApplicationList = new ApplicationList(context);
        if (mApplicationList.loadIndexXml())
            setNewFilter("");
        else
            appsSlice = new ArrayList<>();
    }

    public void beginSyncApplications() {
        ApplicationsSyncTask.startSync(mApplicationList);
    }

    // Returns true if loadMore() can be called
    public boolean setNewFilter(final String filter) {
        appsSlice = mApplicationList.newSlice(filter);
        return loadMore();
    }

    // Returns true if loadMore() can be called
    public boolean loadMore() {
        final boolean canLoadMore = mApplicationList.increaseSlice(DEFAULT_APPS_LIMIT);
        notifyDataSetChanged();
        return canLoadMore;
    }

    public void setAllowInternetDownload(final boolean allow) {
        mImageLoader.mAllowInternetDownload = allow;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int i) {
        return new ViewHolder((LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_application_list, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder view, final int i) {
        view.update(appsSlice.get(i));
        view.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClick != null) {
                    final ApplicationDetails app = appsSlice.get(view.getAdapterPosition());

                    Intent data = new Intent();
                    data.putExtra("url", app.getSourceCodeUrl());
                    data.putExtra("web", app.getWebUrl());
                    data.putExtra("name", app.getName());
                    onItemClick.onClick(data);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return appsSlice.size();
    }
}

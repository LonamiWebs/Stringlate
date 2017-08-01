package io.github.lonamiwebs.stringlate.classes.applications;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.lazyloader.ImageLoader;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ApplicationAdapter extends ArrayAdapter<ApplicationDetails> {
    private final ImageLoader mImageLoader;

    // https://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
    private static class ViewHolder {
        ImageView iconView;
        TextView appName, appDescription;
        View installIndicator;
    }

    public ApplicationAdapter(Context context, List<ApplicationDetails> apps,
                              boolean allowInternetDownload) {
        super(context, R.layout.item_application_list, apps);
        mImageLoader = new ImageLoader(context, allowInternetDownload);
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ApplicationDetails app = getItem(position);

        // This may be the first time we use the recycled view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_application_list, parent, false);

            final ViewHolder holder = new ViewHolder();
            holder.iconView = (ImageView)convertView.findViewById(R.id.appIcon);
            holder.appName = (TextView)convertView.findViewById(R.id.appName);
            holder.appDescription = (TextView)convertView.findViewById(R.id.appDescription);
            holder.installIndicator = convertView.findViewById(R.id.installIndicatorView);
            convertView.setTag(holder);
        }
        if (app != null) {
            final ViewHolder holder = (ViewHolder)convertView.getTag();
            mImageLoader.loadImageAsync(holder.iconView,
                    app.getIconUrl(), app.isInstalled() ? app.getPackageName() : null);

            holder.appName.setText(app.getName());
            holder.appDescription.setText(app.getDescription());
            int visibility = app.isInstalled() ? VISIBLE : GONE;
            holder.installIndicator.setVisibility(visibility);
        }

        return convertView;
    }
}

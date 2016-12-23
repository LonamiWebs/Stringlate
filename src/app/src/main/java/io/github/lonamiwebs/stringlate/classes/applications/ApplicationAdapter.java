package io.github.lonamiwebs.stringlate.classes.applications;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import io.github.lonamiwebs.stringlate.classes.lazyloader.ImageLoader;
import io.github.lonamiwebs.stringlate.R;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ApplicationAdapter extends ArrayAdapter<Application> {
    private ImageLoader mImageLoader;

    public ApplicationAdapter(Context context, int resource, List<Application> apps,
                              boolean allowInternetDownload) {
        super(context, resource, apps);
        mImageLoader = new ImageLoader(context, allowInternetDownload);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Application app = getItem(position);

        // This may be the first time we use the recycled view
        if (convertView == null)
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_application_list, parent, false);

        ImageView iconView = (ImageView)convertView.findViewById(R.id.appIcon);
        mImageLoader.loadImageAsync(iconView, app.getIconUrl(getContext()));

        ((TextView)convertView.findViewById(R.id.appName)).setText(app.getName());
        ((TextView)convertView.findViewById(R.id.appDescription)).setText(app.getDescription());

        int visibility = app.isInstalled() ? VISIBLE : GONE;
        convertView.findViewById(R.id.installIndicatorView).setVisibility(visibility);

        return convertView;
    }
}

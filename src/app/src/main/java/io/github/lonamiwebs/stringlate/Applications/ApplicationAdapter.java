package io.github.lonamiwebs.stringlate.Applications;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import io.github.lonamiwebs.stringlate.R;

public class ApplicationAdapter extends ArrayAdapter<Application> {

    ApplicationIconLoader mIconLoader;

    public ApplicationAdapter(Context context, int resource,
                              List<Application> apps, ApplicationIconLoader iconLoader) {
        super(context, resource, apps);
        mIconLoader = iconLoader;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Application app = getItem(position);

        // This may be the first time we use the recycled view
        if (convertView == null)
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_application_list, parent, false);

        ImageView iconView = (ImageView)convertView.findViewById(R.id.appIcon);
        File iconFile = mIconLoader.getIconFile(app);
        if (iconFile.isFile()) {
            iconView.setImageURI(Uri.fromFile(iconFile));
        } else {
            iconView.setImageResource(app.getIcon());
        }

        ((TextView)convertView.findViewById(R.id.appName)).setText(app.getName());
        ((TextView)convertView.findViewById(R.id.appDescription)).setText(app.getDescription());

        return convertView;
    }
}

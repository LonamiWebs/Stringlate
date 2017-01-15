package io.github.lonamiwebs.stringlate.classes.repos;

import android.content.Context;
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

import io.github.lonamiwebs.stringlate.R;

public class RepoHandlerAdapter extends ArrayAdapter<RepoHandler> {
    public RepoHandlerAdapter(Context context, List<RepoHandler> repositories) {
        // Treat the repositories like applications
        // We can show an icon, the title, and the host as description
        super(context, R.layout.item_application_list, repositories);
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        RepoHandler repo = getItem(position);

        // This may be the first time we use the recycled view
        if (convertView == null)
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_application_list, parent, false);

        ImageView iconView = (ImageView)convertView.findViewById(R.id.appIcon);
        File iconFile = repo.getIconFile();
        if (iconFile == null) {
            iconView.setVisibility(View.INVISIBLE);
        } else {
            iconView.setVisibility(View.VISIBLE);
            iconView.setImageURI(Uri.fromFile(iconFile));
        }

        TextView pathTextView = (TextView)convertView.findViewById(R.id.appName);
        pathTextView.setText(repo.getPath());

        TextView hostTextView = (TextView)convertView.findViewById(R.id.appDescription);
        hostTextView.setText(repo.getHost());

        return convertView;
    }
}

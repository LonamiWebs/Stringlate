package io.github.lonamiwebs.stringlate.classes.resources;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import io.github.lonamiwebs.stringlate.R;

public class ResourcesTranslationAdapter extends ArrayAdapter<ResourcesTranslation> {
    public ResourcesTranslationAdapter(Context context,
                                       List<ResourcesTranslation> resourcesTranslations) {
        super(context, R.layout.item_resource_list, resourcesTranslations);
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ResourcesTranslation rt = getItem(position);

        // This may be the first time we use the recycled view
        if (convertView == null)
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_resource_list, parent, false);

        TextView stringId = (TextView)convertView.findViewById(R.id.stringIdTextView);
        TextView originalValue = (TextView)convertView.findViewById(R.id.originalValueTextView);
        TextView translatedValue = (TextView)convertView.findViewById(R.id.translatedValueTextView);

        stringId.setText(rt.getId());
        originalValue.setText(rt.getOriginal());

        if (rt.hasTranslation()) {
            translatedValue.setText(rt.getTranslation());
            translatedValue.setTypeface(null, Typeface.NORMAL);
        } else {
            translatedValue.setText(R.string.not_translated);
            translatedValue.setTypeface(null, Typeface.ITALIC);
        }

        return convertView;
    }
}

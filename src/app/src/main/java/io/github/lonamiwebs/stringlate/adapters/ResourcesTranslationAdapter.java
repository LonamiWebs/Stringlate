package io.github.lonamiwebs.stringlate.adapters;

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
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesTranslation;

public class ResourcesTranslationAdapter extends ArrayAdapter<ResourcesTranslation> {

    // https://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
    private static class ViewHolder {
        TextView stringId, originalValue, translatedValue;
    }

    public ResourcesTranslationAdapter(Context context,
                                       List<ResourcesTranslation> resourcesTranslations) {
        super(context, R.layout.item_resource_list, resourcesTranslations);
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ResourcesTranslation rt = getItem(position);

        // This may be the first time we use the recycled view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_resource_list, parent, false);

            final ViewHolder holder = new ViewHolder();
            holder.stringId = convertView.findViewById(R.id.stringIdTextView);
            holder.originalValue = convertView.findViewById(R.id.originalValueTextView);
            holder.translatedValue = convertView.findViewById(R.id.translatedValueTextView);
            convertView.setTag(holder);
        }
        if (rt != null) {
            final ViewHolder holder = (ViewHolder) convertView.getTag();
            holder.stringId.setText(rt.getId());
            holder.originalValue.setText(rt.getOriginal());

            if (rt.hasTranslation()) {
                holder.translatedValue.setText(rt.getTranslation());
                holder.translatedValue.setTypeface(null, Typeface.NORMAL);
            } else {
                holder.translatedValue.setText(R.string.not_translated);
                holder.translatedValue.setTypeface(null, Typeface.ITALIC);
            }
        }
        return convertView;
    }
}

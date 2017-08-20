package io.github.lonamiwebs.stringlate.classes;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import io.github.lonamiwebs.stringlate.R;

public class TranslationPeekAdapter extends ArrayAdapter<TranslationPeekAdapter.Item> {

    // https://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
    private static class ViewHolder {
        TextView locale, content;
    }

    public static class Item {
        public final String locale, content;

        public Item(final String locale, final String content) {
            this.locale = locale;
            this.content = content;
        }
    }

    public TranslationPeekAdapter(Context context, List<Item> translations) {
        super(context, R.layout.item_translation_peek_list, translations);
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Item rt = getItem(position);

        // This may be the first time we use the recycled view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_translation_peek_list, parent, false);

            final ViewHolder holder = new ViewHolder();
            holder.locale = (TextView) convertView.findViewById(R.id.localeTextView);
            holder.content = (TextView) convertView.findViewById(R.id.translationContentTextView);
            convertView.setTag(holder);
        }
        if (rt != null) {
            final ViewHolder holder = (ViewHolder) convertView.getTag();
            holder.locale.setText(rt.locale);
            holder.content.setText(rt.content);
        }
        return convertView;
    }
}

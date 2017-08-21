package io.github.lonamiwebs.stringlate.activities.translate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Comparator;
import java.util.Locale;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.LocaleString;

/**
 * Created by stani on 2017-03-04.
 * <p>
 * This class creates a simple AlertDialog with a list of locales.
 * Use the callback method to retrieve the selected locale.
 */
class LocaleListBuilder {

    // https://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
    private static class ViewHolder {
        TextView displayLang, displayCountry, langCode;
    }

    private final AlertDialog mDialog;

    LocaleListBuilder(final Activity activity, final LocalePickCallback cb) {

        final Locale[] locales = Locale.getAvailableLocales();
        final LocaleArrayAdapter arrayAdapter =
                new LocaleArrayAdapter(activity, R.layout.item_locale_list, locales);

        arrayAdapter.sort(new Comparator<Locale>() {
            @Override
            public int compare(final Locale o1, final Locale o2) {
                final String lang1 = o1.getDisplayLanguage();
                final String lang2 = o2.getDisplayLanguage();
                return lang1.compareTo(lang2);
            }
        });

        final DialogInterface.OnClickListener clickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cb.onChoice(LocaleString.getFullCode(locales[which]));
                    }
                };

        mDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.choose_locale_from_list)
                .setNegativeButton(R.string.cancel, null)
                .setAdapter(arrayAdapter, clickListener).create();

        mDialog.getListView().setFastScrollEnabled(true);
        mDialog.getListView().setFastScrollAlwaysVisible(true);
    }

    public void show() {
        mDialog.show();
    }

    private class LocaleArrayAdapter extends ArrayAdapter<Locale> {

        final int mResource;

        LocaleArrayAdapter(final Context context, final int resource, final Locale[] locales) {
            super(context, resource, locales);
            mResource = resource;
        }

        @NonNull
        @Override
        public View getView(final int position, View view, @NonNull final ViewGroup parent) {
            final Locale locale = this.getItem(position);

            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
                final ViewHolder holder = new ViewHolder();
                holder.displayLang = view.findViewById(R.id.language_name);
                holder.displayCountry = view.findViewById(R.id.language_country);
                holder.langCode = view.findViewById(R.id.language_code);
                view.setTag(holder);
            }

            if (locale != null) {
                final ViewHolder holder = (ViewHolder) view.getTag();

                holder.displayLang.setText(locale.getDisplayLanguage());
                holder.langCode.setText(LocaleString.getFullCode(locale));

                final String displayCountry = locale.getDisplayCountry();
                if (displayCountry.isEmpty()) {
                    holder.displayCountry.setText(R.string.default_parenthesis);
                    holder.displayCountry.setTypeface(null, Typeface.ITALIC);
                } else {
                    holder.displayCountry.setText(displayCountry);
                    holder.displayCountry.setTypeface(null, Typeface.NORMAL);
                }
            }

            return view;
        }
    }

    interface LocalePickCallback {
        void onChoice(String localeCode);
    }
}

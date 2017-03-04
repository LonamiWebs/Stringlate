package io.github.lonamiwebs.stringlate.activities.translate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Comparator;
import java.util.Locale;

import io.github.lonamiwebs.stringlate.R;

/**
 * Created by stani on 2017-03-04.
 *
 * This class creates a simple AlertDialog with a list of locales.
 * Use the callback method to retrieve the selected locale.
 */
public class LocaleListBuilder {

    private final AlertDialog mDialog;

    /**
     *
     * @param activity
     * @param cb - use this to retrieve the selected locale
     */
    public LocaleListBuilder(final Activity activity, final LocalePickCallback cb) {

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
                        String localeCode = locales[which].getLanguage();
                        cb.onChoice(localeCode);
                    }
                };

        mDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.choose_locale_from_list)
                .setNegativeButton(R.string.cancel, null)
                .setAdapter(arrayAdapter,clickListener).create();
    }

    public void show() {
        mDialog.show();
    }

    private class LocaleArrayAdapter extends ArrayAdapter<Locale> {

        final int mResource;
        public LocaleArrayAdapter(final Context context, final int resource, final Locale[] locales) {
            super(context, resource, locales);
            mResource = resource;
        }

        @Override
        public View getView(final int position, final View view, final ViewGroup parent) {
            final Locale locale = this.getItem(position);
            final View convertView;
            if (view == null) {
                convertView = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
            } else {
                convertView = view;
            }

            TextView tvLangName = (TextView) convertView.findViewById(R.id.language_name);
            TextView tvLangCode = (TextView) convertView.findViewById(R.id.language_code);

            tvLangCode.setText(locale.getLanguage());
            tvLangName.setText(locale.getDisplayName());

            return convertView;
        }
    }

    public interface LocalePickCallback {
        void onChoice(String localeCode);
    }
}

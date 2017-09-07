package io.github.lonamiwebs.stringlate.classes.locales;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import io.github.lonamiwebs.stringlate.R;

public class LocaleEntryAdapter extends RecyclerView.Adapter<LocaleEntryAdapter.ViewHolder> {

    private ArrayList<Locale> mLocales;
    private ArrayList<Locale> mFilteredLocales;

    public interface OnItemClick {
        void onClick(Locale which);
    }

    public LocaleEntryAdapter.OnItemClick onItemClick;

    class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout root;
        final TextView displayLang, displayCountry, langCode;

        ViewHolder(final LinearLayout root) {
            super(root);
            this.root = root;
            displayLang = root.findViewById(R.id.language_name);
            displayCountry = root.findViewById(R.id.language_country);
            langCode = root.findViewById(R.id.language_code);
        }

        void update(final Locale locale) {
            displayLang.setText(locale.getDisplayLanguage());
            langCode.setText(LocaleString.getFullCode(locale));

            final String displayCountryText = locale.getDisplayCountry();
            if (displayCountryText.isEmpty())
                displayCountry.setText(LocaleString.getEmojiFlag(locale));
            else
                displayCountry.setText(displayCountryText);
        }
    }

    // Optionally show also the country-specific locales, or not at all
    LocaleEntryAdapter(boolean showCountrySpecific) {
        // Create a map {locale code: Locale} to behave like a set and avoid duplicates
        final HashMap<String, Locale> locales = new HashMap<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            if (showCountrySpecific || locale.getCountry().isEmpty())
                locales.put(LocaleString.getFullCode(locale), locale);
        }

        for (String isoLang : Locale.getISOLanguages()) {
            if (!locales.containsKey(isoLang))
                locales.put(isoLang, new Locale(isoLang));
        }

        // Once everything is filtered, fill in the array list
        initLocales(locales.values());
    }

    // Used to show only all the specialized countries for a given locale code
    LocaleEntryAdapter(final String localeCode) {
        if (localeCode.isEmpty()) {
            initLocales(new ArrayList<Locale>(0));
        } else {
            final ArrayList<Locale> locales = new ArrayList<>();
            for (Locale locale : Locale.getAvailableLocales())
                if (locale.getLanguage().equals(localeCode))
                    locales.add(locale);

            initLocales(locales);
        }
    }

    public void initLocales(final Collection<Locale> locales) {
        mLocales = new ArrayList<>(locales.size());
        mFilteredLocales = new ArrayList<>(locales.size());
        for (Locale locale : locales)
            mLocales.add(locale);

        Collections.sort(mLocales, new Comparator<Locale>() {
            @Override
            public int compare(Locale o1, Locale o2) {
                return o1.getDisplayLanguage().compareTo(o2.getDisplayLanguage());
            }
        });

        // Initial collection is now sorted, so add everything to our filtered list
        for (Locale locale : mLocales)
            mFilteredLocales.add(locale);

        notifyDataSetChanged();
    }

    public void setFilter(@NonNull String filter) {
        mFilteredLocales.clear();
        filter = filter.toLowerCase();
        for (Locale locale : mLocales)
            if (locale.getDisplayLanguage().toLowerCase().contains(filter))
                mFilteredLocales.add(locale);

        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int i) {
        return new ViewHolder((LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_locale_list, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder view, final int i) {
        view.update(mFilteredLocales.get(i));
        view.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClick != null) {
                    onItemClick.onClick(mFilteredLocales.get(view.getAdapterPosition()));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFilteredLocales.size();
    }
}

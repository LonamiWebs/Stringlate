package io.github.lonamiwebs.stringlate.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.locales.LocaleString;
import io.github.lonamiwebs.stringlate.settings.AppSettings;

public class LocaleEntryAdapter extends RecyclerView.Adapter<LocaleEntryAdapter.ViewHolder> {

    private ArrayList<Locale> mLocales;
    private ArrayList<Locale> mFilteredLocales;

    private final String mPreferredLocale;
    private final boolean mShowMoreLocales;

    public interface OnItemClick {
        void onLocaleSelected(Locale which);

        void onLocaleExpanderSelected(Locale which);
    }

    public LocaleEntryAdapter.OnItemClick onItemClick;

    class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout root;
        final TextView displayLang, displayCountry, langCode;
        final ImageView expand;

        ViewHolder(final LinearLayout root) {
            super(root);
            this.root = root;
            displayLang = root.findViewById(R.id.language_name);
            displayCountry = root.findViewById(R.id.language_country);
            langCode = root.findViewById(R.id.language_code);
            expand = root.findViewById(R.id.expand);
        }

        void update(final Locale locale) {
            displayLang.setText(locale.getDisplayLanguage());
            langCode.setText(LocaleString.getFullCode(locale));

            final String displayCountryText = locale.getDisplayCountry();
            if (displayCountryText.isEmpty()) {
                displayCountry.setText(LocaleString.getEmojiFlag(locale));
            } else {
                displayCountry.setText(displayCountryText);
            }

            ArrayList<Locale> locales = LocaleString.getCountriesForLocale(LocaleString.getFullCode(locale));
            boolean displayExpander = locales.size() > 1 && displayCountryText.isEmpty();
            expand.setVisibility(displayExpander ? View.VISIBLE : View.INVISIBLE);
        }
    }

    // Optionally show also the country-specific locales, or not at all
    public LocaleEntryAdapter(final Context context, boolean showCountrySpecific, boolean showMoreLocales) {
        mPreferredLocale = new AppSettings(context).getLanguage();
        mShowMoreLocales = showMoreLocales;
        // Create a map {locale code: Locale} to behave like a set and avoid duplicates
        final HashMap<String, Locale> locales = new HashMap<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            if (showCountrySpecific || locale.getCountry().isEmpty())
                locales.put(LocaleString.getFullCode(locale), locale);
        }

        if (mShowMoreLocales) {
            for (String isoLang : Locale.getISOLanguages()) {
                if (!locales.containsKey(isoLang))
                    locales.put(isoLang, new Locale(isoLang));
            }
        }

        // Once everything is filtered, fill in the array list
        initLocales(locales.values());
    }

    // Used to show only all the specialized countries for a given locale code
    public LocaleEntryAdapter(final Context context, final String localeCode) {
        mPreferredLocale = new AppSettings(context).getLanguage();
        mShowMoreLocales = true;
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
                final boolean left = o1.getLanguage().equals(mPreferredLocale);
                final boolean right = o2.getLanguage().equals(mPreferredLocale);
                if (left != right) {
                    if (left)
                        return -1;
                    else
                        return +1;
                } else {
                    return LocaleString.getFullCode(o1).compareTo(LocaleString.getFullCode(o2));
                }
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
        for (Locale locale : mLocales) {
            if (locale.getDisplayLanguage().toLowerCase().contains(filter)) {
                mFilteredLocales.add(locale);
            } else if (LocaleString.getFullCode(locale).toLowerCase().equals(filter)) {
                // Insert at the beginning since these exact matches have priority
                mFilteredLocales.add(0, locale);
            }
        }

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
        ((LinearLayout) view.expand.getParent()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClick != null) {
                    onItemClick.onLocaleExpanderSelected(mFilteredLocales.get(view.getAdapterPosition()));
                }
            }
        });
        ((LinearLayout) view.displayLang.getParent()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClick != null) {
                    onItemClick.onLocaleSelected(mFilteredLocales.get(view.getAdapterPosition()));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFilteredLocales.size();
    }
}

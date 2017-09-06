package io.github.lonamiwebs.stringlate.classes.locales;


import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import io.github.lonamiwebs.stringlate.R;

public class LocaleSelectionDialog extends DialogFragment implements TabLayout.OnTabSelectedListener {

    //region Static methods and members

    public static final String TAG = "LocaleSelectionDialog";

    public static LocaleSelectionDialog newInstance() {
        LocaleSelectionDialog result = new LocaleSelectionDialog();
        result.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        return result;
    }

    //endregion

    //region Members

    private LocaleEntryAdapter mLocaleEntryAdapterLocales;
    private LocaleEntryAdapter mLocaleEntryAdapterCountries;
    private TabLayout.Tab mTabLocales;
    private TabLayout.Tab mTabCountries;

    private RecyclerView mLocaleRecyclerView;
    private TabLayout mTabLayout;
    private EditText mSearchEditText;

    //endregion

    //region Creation

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.dialog_locale_selection, container, false);

        mLocaleRecyclerView = root.findViewById(R.id.locale_recycler_view);
        mTabLayout = root.findViewById(R.id.tab_layout);
        mSearchEditText = root.findViewById(R.id.search_edit_text);

        final Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            if (window != null) {
                // Needed so that the dialog resizes when the keyboard is opened
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }

        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onSearchChanged();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        root.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // More UI
        mTabLayout.addOnTabSelectedListener(this);
        mTabLocales = mTabLayout.getTabAt(0);
        mTabCountries = mTabLayout.getTabAt(1);

        // TODO Setup adapters
    }

    //endregion

    //region Events

    void onLocaleSelected() {
        dismiss();
    }

    void onSearchChanged() {
        mTabLocales.select();
        //mLocaleEntryAdapterCountries.filter(mSearchEditText.getText().toString());
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        /*
        if (tab == mTabLocales)
            mLocaleRecyclerView.setAdapter(mLocaleEntryAdapterLocales);
        else
            mLocaleRecyclerView.setAdapter(mLocaleEntryAdapterCountries);
        */
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        onTabReselected(tab);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) { }

    //endregion
}

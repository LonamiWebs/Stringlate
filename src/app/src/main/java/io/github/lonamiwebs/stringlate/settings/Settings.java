package io.github.lonamiwebs.stringlate.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public abstract class Settings {
    private final SharedPreferences mPrefs;

    protected Settings(Context context, String name) {
        if (name == null) {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        } else {
            mPrefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        }
    }

    protected SharedPreferences getSettings() {
        return mPrefs;
    }

    protected SharedPreferences.Editor editSettings() {
        return mPrefs.edit();
    }
}

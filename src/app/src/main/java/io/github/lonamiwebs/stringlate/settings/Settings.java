package io.github.lonamiwebs.stringlate.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public abstract class Settings {
    private final SharedPreferences mPrefs;

    Settings(Context context, String name) {
        if (name == null) {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        } else {
            mPrefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        }
    }

    SharedPreferences getSettings() {
        return mPrefs;
    }

    SharedPreferences.Editor editSettings() {
        return mPrefs.edit();
    }
}

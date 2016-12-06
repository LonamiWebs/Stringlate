package io.github.lonamiwebs.stringlate.Applications;

import java.util.HashMap;

import io.github.lonamiwebs.stringlate.R;

public class Application {

    //region Members

    public static final String ICON = "icon";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "desc";

    int mIcon;
    String mName;
    String mDescription;

    //endregion

    //region Initialization

    public Application(String name, String description) {
        mIcon = R.drawable.app_not_found;
        mName = name;
        mDescription = description;
    }

    //endregion

    //region Getters

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put(ICON, mIcon);
        result.put(NAME, mName);
        result.put(DESCRIPTION, mDescription);
        return result;
    }

    //endregion
}

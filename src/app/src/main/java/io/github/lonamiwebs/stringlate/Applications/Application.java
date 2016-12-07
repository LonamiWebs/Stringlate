package io.github.lonamiwebs.stringlate.Applications;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import io.github.lonamiwebs.stringlate.R;

public class Application {

    //region Members

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static final String ICON = "icon";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "desc";

    int mIcon;

    Date mLastUpdated;
    String mPackageName;
    String mName;
    String mDescription;
    String mIconName;
    String mSourceCodeUrl;

    //endregion

    //region Initialization

    public Application(String packageName, String lastUpdated,
                       String name, String description,
                       String iconName, String sourceCodeUrl) {
        mIcon = R.drawable.app_not_found;

        try {
            mLastUpdated = DATE_FORMAT.parse(lastUpdated);
        } catch (ParseException e) {
            // Won't happen
            e.printStackTrace();
        }

        mPackageName = packageName;
        mName = name;
        mDescription = description;
        mIconName = iconName;
        mSourceCodeUrl = sourceCodeUrl;
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

    public String getLastUpdatedDateString() {
        return DATE_FORMAT.format(mLastUpdated);
    }

    @Override
    public int hashCode() {
        return mPackageName.hashCode();
    }

    //endregion
}

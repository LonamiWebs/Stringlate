package io.github.lonamiwebs.stringlate.Applications;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.github.lonamiwebs.stringlate.R;

public class Application {

    //region Members

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private int mIcon;

    private Date mLastUpdated;
    private String mPackageName;
    private String mName;
    private String mDescription;
    private String mIconName;
    private String mSourceCodeUrl;

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

    public String getPackageName() {
        return mPackageName;
    }

    public String getLastUpdatedDateString() {
        return DATE_FORMAT.format(mLastUpdated);
    }

    public String getName() {
        return mName;
    }

    public String getDescription() {
        return mDescription;
    }

    public int getIcon() {
        return mIcon;
    }

    public String getIconName() {
        return mIconName;
    }

    public String getSourceCodeUrl() {
        return mSourceCodeUrl;
    }

    @Override
    public int hashCode() {
        return mPackageName.hashCode();
    }

    //endregion
}

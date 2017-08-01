package io.github.lonamiwebs.stringlate.classes.applications;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Application {

    //region Members
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private Date mLastUpdated;
    private final String mPackageName;
    private final String mName;
    private final String mDescription;
    private final String mIconUrl;
    private final String mSourceCodeUrl;
    private final String mWebUrl;

    private boolean mIsInstalled;

    //endregion

    //region Initialization

    public Application(String packageName, String lastUpdated,
                       String name, String description,
                       String iconUrl, String sourceCodeUrl,
                       String webUrl) {
        try {
            mLastUpdated = DATE_FORMAT.parse(lastUpdated);
        } catch (ParseException e) {
            // Won't happen
            e.printStackTrace();
        }

        mPackageName = packageName;
        mName = name;
        mDescription = description;
        mSourceCodeUrl = sourceCodeUrl;
        mIconUrl = iconUrl;
        mWebUrl = webUrl;
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

    public String getIconUrl() {
        return mIconUrl;
    }

    public String getSourceCodeUrl() {
        return mSourceCodeUrl;
    }

    public boolean isInstalled() {
        return mIsInstalled;
    }

    public String getWebUrl() {
        return mWebUrl;
    }

    @Override
    public int hashCode() {
        return mPackageName.hashCode();
    }

    //endregion

    //region Setters

    void setInstalled() {
        mIsInstalled = true;
    }

    //endregion
}

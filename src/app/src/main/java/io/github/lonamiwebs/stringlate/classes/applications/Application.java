package io.github.lonamiwebs.stringlate.classes.applications;

import android.content.Context;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static io.github.lonamiwebs.stringlate.classes.applications.ApplicationList.FDROID_REPO_URL;

public class Application {

    //region Members

    private static final String FALLBACK_ICONS_DIR = "/icons/";
    private static String mBaseIconUrl;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private Date mLastUpdated;
    private final String mPackageName;
    private final String mName;
    private final String mDescription;
    private final String mIconName;
    private final String mSourceCodeUrl;

    private boolean mIsInstalled;

    //endregion

    //region Initialization

    public Application(String packageName, String lastUpdated,
                       String name, String description,
                       String iconName, String sourceCodeUrl) {
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

    public String getIconName() {
        return mIconName;
    }

    public String getIconUrl(Context context) {
        return getFDroidIconUrl(context)+mIconName;
    }

    public String getSourceCodeUrl() {
        return mSourceCodeUrl;
    }

    public boolean isInstalled() {
        return mIsInstalled;
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

    private static String getFDroidIconUrl(Context context) {
        if (mBaseIconUrl == null)
            mBaseIconUrl = FDROID_REPO_URL+getIconDirectory(context);

        return mBaseIconUrl;
    }

    private static String getIconDirectory(Context context) {
        final double dpi = context.getResources().getDisplayMetrics().densityDpi;
        if (dpi >= 640) return "/icons-640/";
        if (dpi >= 480) return "/icons-480/";
        if (dpi >= 320) return "/icons-320/";
        if (dpi >= 240) return "/icons-240/";
        if (dpi >= 160) return "/icons-160/";
        if (dpi >= 120) return "/icons-120/";

        return FALLBACK_ICONS_DIR;
    }
}

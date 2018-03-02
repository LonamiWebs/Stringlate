package io.github.lonamiwebs.stringlate.classes.applications;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ApplicationDetails {

    //region Members
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private Date mLastUpdated;
    private String mPackageName;
    private String mName;
    private String mDescription;
    private String mIconUrl;
    private String mSourceCodeUrl;
    private String mWebUrl;
    private String mMail;

    private boolean mIsInstalled;

    //endregion

    //region Initialization

    public ApplicationDetails() {

    }

    public ApplicationDetails(String packageName, String lastUpdated,
                              String name, String description,
                              String iconUrl, String sourceCodeUrl,
                              String webUrl, String mail) {
        try {
            mLastUpdated = DATE_FORMAT.parse(lastUpdated);
        } catch (ParseException e) {
            // Won't happen
            e.printStackTrace();
        }

        setPackageName(packageName);
        setProjectName(name);
        setDescription(description);
        setSourceCodeUrl(sourceCodeUrl);
        setIconUrl(iconUrl);
        setProjectWebUrl(webUrl);
        setProjectMail(mail);
    }


    //endregion

    //region Getters

    public String getPackageName() {
        return mPackageName;
    }

    public String getLastUpdatedDateString() {
        return DATE_FORMAT.format(mLastUpdated);
    }

    public String getProjectName() {
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

    public String getProjectWebUrl() {
        return mWebUrl;
    }

    public String getProjectMail() {
        return mMail;
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

    public void setLastUpdated(Date lastUpdated) {
        mLastUpdated = lastUpdated;
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    public void setProjectName(String name) {
        mName = name;
    }

    public void setProjectMail(String mail) {
        if (mail != null && !mail.isEmpty() && mail.contains("@@")) {
            mail = new StringBuilder(mail).reverse().toString().replace("@@", "@");
        }
        mMail = mail;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public void setIconUrl(String iconUrl) {
        mIconUrl = iconUrl;
    }

    public void setSourceCodeUrl(String sourceCodeUrl) {
        mSourceCodeUrl = sourceCodeUrl;
    }

    public void setProjectWebUrl(String webUrl) {
        mWebUrl = webUrl;
    }
    //endregion
}

package io.github.lonamiwebs.stringlate.classes.resources;

public class ResourcesString implements Comparable<ResourcesString> {

    //region Members

    private final String mId;
    private String mContent;

    // "metadata" used to keep track whether a string is the original or not. This
    // will be later used when downloading remote changes, to keep local if modified.
    private boolean mModified;

    //endregion

    //region Constructors

    ResourcesString(String id) {
        this(id, "", false);
    }

    ResourcesString(String id, String content, boolean modified) {
        mId = id.trim();
        mContent = content.trim();
        mModified = modified;
    }

    //endregion

    //region Getters

    public String getId() {
        return mId;
    }

    public String getContent() {
        return mContent;
    }

    public int getContentLength() {
        return mContent.length();
    }

    public boolean hasContent() { return mContent != null && !mContent.isEmpty(); }

    public boolean wasModified() { return mModified; }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    @Override
    public int compareTo(ResourcesString resourcesString) {
        if (resourcesString == null)
            return -1;

        return mId.compareTo(resourcesString.mId);
    }

    //endregion

    //region Setters

    // Returns true if the content was successfully set
    public boolean setContent(String content) {
        content = content.trim();
        if (!mContent.equals(content)) {
            mContent = content;
            mModified = true;
            return true;
        } else {
            return false;
        }
    }

    //endregion
}

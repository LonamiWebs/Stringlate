package io.github.lonamiwebs.stringlate.classes.resources;

public class ResourcesString implements Comparable<ResourcesString> {

    //region Members

    private final String mId;
    private String mContent;
    private final boolean mTranslatable;

    //endregion

    //region Constructors

    ResourcesString(String id, String content) {
        // Strings are by default translatable unless otherwise stated
        this(id, content, true);
    }

    ResourcesString(String id, String content, boolean translatable) {
        mId = id.trim();
        mContent = content.trim();
        mTranslatable = translatable;
    }

    //endregion

    //region Getters

    public String getId() {
        return mId;
    }

    public String getContent() {
        return mContent;
    }
    public boolean hasContent() { return mContent != null && !mContent.isEmpty(); }

    public boolean isTranslatable() {
        return mTranslatable;
    }

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
            return true;
        } else {
            return false;
        }
    }

    //endregion
}

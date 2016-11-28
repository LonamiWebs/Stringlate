package io.github.lonamiwebs.stringlate.ResourcesStrings;

public class ResourcesString {

    //region Members

    private final String mId;
    private String mContent;
    private final boolean mTranslatable;

    private boolean mSavedChanges;

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
        mSavedChanges = true;
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

    //endregion

    //region Setters

    // Returns true if the content was successfully set
    public boolean setContent(String content) {
        content = content.trim();
        if (!mContent.equals(content)) {
            mContent = content;
            mSavedChanges = false;
            return true;
        } else {
            return false;
        }
    }

    //endregion
}

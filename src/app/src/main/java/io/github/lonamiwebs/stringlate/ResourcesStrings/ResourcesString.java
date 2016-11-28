package io.github.lonamiwebs.stringlate.ResourcesStrings;

public class ResourcesString {
    private final String mId;
    private String mContent;
    private final boolean mTranslatable;

    private boolean mSavedChanges;

    ResourcesString(String id, String content) {
        this(id, content, true);
    }
    ResourcesString(String id, String content, boolean translatable) {
        mId = id.trim();
        mContent = content.trim();
        mTranslatable = translatable;
        mSavedChanges = true;
    }

    public String getId() {
        return mId;
    }

    public String getContent() {
        return mContent;
    }

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

    public boolean hasContent() { return mContent != null && !mContent.isEmpty(); }

    public boolean isTranslatable() {
        return mTranslatable;
    }
}

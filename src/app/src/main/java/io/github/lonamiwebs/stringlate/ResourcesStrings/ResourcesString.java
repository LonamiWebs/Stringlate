package io.github.lonamiwebs.stringlate.ResourcesStrings;

public class ResourcesString {
    private String mId;
    private String mContent;
    private boolean mTranslatable;

    ResourcesString(String id, String content, boolean translatable) {
        mId = id;
        mContent = content;
        mTranslatable = translatable;
    }

    public String getId() {
        return mId;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String mContent) {
        this.mContent = mContent;
    }

    public boolean isTranslatable() {
        return mTranslatable;
    }
}

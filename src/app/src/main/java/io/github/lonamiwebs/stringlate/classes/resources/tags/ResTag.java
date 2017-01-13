package io.github.lonamiwebs.stringlate.classes.resources.tags;

import android.support.annotation.NonNull;

public abstract class ResTag implements Comparable<ResTag> {

    //region Members

    @NonNull String mContent;

    // "metadata" used to keep track whether a string is the original or not. This
    // will be later used when downloading remote changes, to keep local if modified.
    boolean mModified;

    //endregion

    //region Getters

    @NonNull abstract public String getId();

    @Override
    public int hashCode() { return getId().hashCode(); }

    @NonNull
    public String getContent() { return mContent; }
    public int getContentLength() { return mContent.length(); }
    public boolean hasContent() { return !mContent.isEmpty(); }

    public boolean wasModified() { return mModified; }

    @NonNull
    public abstract ResTag clone(String newContent);

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

    //region Interfaces implementation

    @Override
    public int compareTo(@NonNull ResTag o) {
        return getId().compareTo(o.getId());
    }
}

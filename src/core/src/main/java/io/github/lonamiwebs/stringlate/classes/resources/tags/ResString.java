package io.github.lonamiwebs.stringlate.classes.resources.tags;

import android.support.annotation.NonNull;

public class ResString extends ResTag {

    //region Members

    @NonNull
    private final String mId;

    //endregion

    //region Constructors

    public ResString(@NonNull String id, String content, boolean modified) {
        mId = id.trim();
        mContent = content.trim();
        mModified = modified;
    }

    //endregion

    //region Getters

    @Override
    @NonNull
    public String getId() {
        return mId;
    }

    @Override
    @NonNull
    public ResTag clone(String newContent) {
        ResString result = new ResString(mId, mContent, mModified);
        result.setContent(newContent);
        return result;
    }

    //endregion
}

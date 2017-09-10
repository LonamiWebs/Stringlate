package io.github.lonamiwebs.stringlate.classes.resources.tags;

public class ResString extends ResTag {

    //region Members

    private final String mId;

    //endregion

    //region Constructors

    public ResString(final String id, final String content, final boolean modified) {
        if (id == null)
            throw new IllegalArgumentException("ID is null");
        mId = id.trim();
        mContent = content.trim();
        mModified = modified;
    }

    //endregion

    //region Getters

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public ResTag clone(String newContent) {
        ResString result = new ResString(mId, mContent, mModified);
        result.setContent(newContent);
        return result;
    }

    //endregion
}

package io.github.lonamiwebs.stringlate.classes.resources.tags;

import java.util.HashSet;
import java.util.Locale;

public class ResStringArray {

    //region Members

    private final HashSet<Item> mItems;
    private final String mId;

    //endregion

    //region Constructor

    public ResStringArray(final String id) {
        if (id == null)
            throw new IllegalArgumentException();
        mItems = new HashSet<>();
        mId = id;
    }

    //endregion

    //region Getters

    public String getId() {
        return mId;
    }

    public Item getItem(final int i) {
        for (Item item : mItems)
            if (item.getIndex() == i)
                return item;

        return null;
    }

    public Iterable<Item> expand() {
        return mItems;
    }

    private ResStringArray fakeClone() {
        // We're losing the original items… But this is the desired behaviour
        // because when setting the content for a new translation for the first
        // time, we need it to be a new parent
        return new ResStringArray(mId);
    }

    //endregion

    //region Setters

    public Item addItem(final String content, final boolean modified, final int index) {
        if (content == null)
            throw new IllegalArgumentException();
        int i = index < 0 ? mItems.size() : index; // Auto-detect index if -1 (negative)
        Item result = new Item(this, i, content, modified);
        mItems.add(result);
        return result;
    }

    //endregion

    //region Sub classes

    public class Item extends ResTag {
        final ResStringArray mParent;
        final int mIndex;

        Item(final ResStringArray parent,
             final int index, String content, final boolean modified) {
            if (parent == null || content == null)
                throw new IllegalArgumentException();
            mParent = parent;
            mIndex = index;
            mContent = content.trim();
            mModified = modified;
        }

        @Override
        public String getId() {
            // ':' is not a valid separator for the <string>'s, so use it to avoid conflicts
            return String.format(Locale.ENGLISH, "%s:%d", mParent.mId, mIndex);
        }

        public int getIndex() {
            return mIndex;
        }

        @Override
        public ResTag clone(String newContent) {
            ResStringArray parent = mParent.fakeClone();
            Item result = parent.addItem(mContent, mModified, mIndex);
            result.setContent(newContent);
            return result;
        }

        public ResStringArray getParent() {
            return mParent;
        }
    }

    //endregion
}

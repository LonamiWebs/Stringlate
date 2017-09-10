package io.github.lonamiwebs.stringlate.classes.resources.tags;

import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Locale;

public class ResStringArray {

    //region Members

    @NonNull
    private final HashSet<Item> mItems;
    @NonNull
    private final String mId;

    //endregion

    //region Constructor

    public ResStringArray(@NonNull final String id) {
        mItems = new HashSet<>();
        mId = id;
    }

    //endregion

    //region Getters

    @NonNull
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

    public Item addItem(@NonNull final String content, final boolean modified, final int index) {
        int i = index < 0 ? mItems.size() : index; // Auto-detect index if -1 (negative)
        Item result = new Item(this, i, content, modified);
        mItems.add(result);
        return result;
    }

    //endregion

    //region Sub classes

    public class Item extends ResTag {
        @NonNull
        final ResStringArray mParent;
        final int mIndex;

        Item(@NonNull final ResStringArray parent,
             final int index, @NonNull String content, final boolean modified) {
            mParent = parent;
            mIndex = index;
            mContent = content.trim();
            mModified = modified;
        }

        @Override
        @NonNull
        public String getId() {
            // ':' is not a valid separator for the <string>'s, so use it to avoid conflicts
            return String.format(Locale.ENGLISH, "%s:%d", mParent.mId, mIndex);
        }

        public int getIndex() {
            return mIndex;
        }

        @Override
        @NonNull
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

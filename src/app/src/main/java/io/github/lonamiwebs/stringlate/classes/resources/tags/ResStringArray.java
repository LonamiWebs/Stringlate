package io.github.lonamiwebs.stringlate.classes.resources.tags;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Locale;

public class ResStringArray {

    //region Members

    // These strings are ORDERED
    @NonNull private final ArrayList<Item> mItems;
    @NonNull private final String mId;

    //endregion

    //region Constructor

    public ResStringArray(@NonNull final String id) {
        mItems = new ArrayList<>();
        mId = id;
    }

    //endregion

    //region Getters

    public Item getItem(final int i) {
        return mItems.get(i);
    }

    //endregion

    //region Setters

    public void addItem(@NonNull final String content) {
        mItems.add(new Item(this, mItems.size(), content));
    }

    //endregion

    //region Sub classes

    private class Item extends ResTag {
        @NonNull final ResStringArray mParent;
        final int mIndex;

        Item(@NonNull final ResStringArray parent,
                    final int index, @NonNull String content) {
            mParent = parent;
            mIndex = index;
            mContent = content.trim();
        }

        @Override
        @NonNull
        public String getId() {
            // ':' is not a valid separator for the <string>'s, so use it to avoid conflicts
            return String.format(Locale.ENGLISH, "%s:%d", mParent.mId, mIndex);
        }
    }

    //endregion
}

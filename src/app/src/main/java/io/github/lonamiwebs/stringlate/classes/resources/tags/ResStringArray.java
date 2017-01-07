package io.github.lonamiwebs.stringlate.classes.resources.tags;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    @NonNull
    public String getId() {
        return mId;
    }

    public Item getItem(final int i) {
        return mItems.get(i);
    }

    public Iterable<Item> expand() {
        return mItems;
    }

    //endregion

    //region Setters

    public void addItem(@NonNull final String content, final boolean modified) {
        mItems.add(new Item(this, mItems.size(), content, modified));
    }

    public void sort() {
        Collections.sort(mItems, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                int x = o1.mIndex;
                int y = o2.mIndex;
                return (x < y) ? -1 : ((x == y) ? 0 : 1); // Integer.compare implementation
            }
        });
    }

    //endregion

    //region Sub classes

    public class Item extends ResTag {
        @NonNull final ResStringArray mParent;
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

        public ResStringArray getParent() {
            return mParent;
        }
    }

    //endregion
}

package io.github.lonamiwebs.stringlate.classes.resources.tags;

import android.support.annotation.NonNull;

import java.util.HashSet;

public class ResPlurals {

    //region Members

    @NonNull private final HashSet<Item> mItems;
    @NonNull private final String mId;

    //endregion

    //region Constructor

    public ResPlurals(@NonNull String id) {
        mItems = new HashSet<>();
        mId = id;
    }

    //endregion

    //region Getters

    public String getId() {
        return mId;
    }

    public Item getItem(@NonNull final String quantity) {
        for (Item i : mItems)
            if (i.mQuantity.equals(quantity))
                return i;
        return null;
    }

    public Iterable<Item> expand() {
        return mItems;
    }

    //endregion

    //region Setters

    public void addItem(@NonNull final String quantity,
                        @NonNull final String content, final boolean modified) {
        mItems.add(new Item(this, quantity, content, modified));
    }

    //endregion

    //region Sub classes

    public class Item extends ResTag {
        @NonNull final ResPlurals mParent;
        @NonNull final String mQuantity;

        Item(@NonNull final ResPlurals parent, @NonNull final String quantity,
             @NonNull final String content, final boolean modified) {
            mParent = parent;
            mQuantity = quantity;
            mContent = content.trim();
            mModified = modified;
        }

        @Override
        @NonNull
        public String getId() {
            // ':' is not a valid separator for the <string>'s, so use it to avoid conflicts
            return String.format("%s:%s", mParent.mId, mQuantity);
        }

        public String getQuantity() {
            return mQuantity;
        }

        public ResPlurals getParent() {
            return mParent;
        }
    }

    //endregion
}

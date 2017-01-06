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

    public Item getItem(@NonNull final String quantity) {
        for (Item i : mItems)
            if (i.mQuantity.equals(quantity))
                return i;
        return null;
    }

    //endregion

    //region Setters

    public void addItem(@NonNull final String quantity, @NonNull final String content) {
        mItems.add(new Item(this, quantity, content));
    }

    //endregion

    //region Sub classes

    private class Item extends ResTag {
        @NonNull final ResPlurals mParent;
        @NonNull final String mQuantity;

        Item(@NonNull final ResPlurals parent,
                    @NonNull final String quantity, @NonNull final String content) {
            mParent = parent;
            mQuantity = quantity;
            mContent = content.trim();
        }

        @Override
        @NonNull
        public String getId() {
            // ':' is not a valid separator for the <string>'s, so use it to avoid conflicts
            return String.format("%s:%s", mParent.mId, mQuantity);
        }
    }

    //endregion
}

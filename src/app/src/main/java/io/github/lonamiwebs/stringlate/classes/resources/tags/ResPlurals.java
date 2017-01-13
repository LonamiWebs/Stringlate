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

    private ResPlurals fakeClone() {
        // We're losing the original items… But this is the desired behaviour
        // because when setting the content for a new translation for the first
        // time, we need it to be a new parent
        return new ResPlurals(mId);
    }

    //endregion

    //region Setters

    public Item addItem(@NonNull final String quantity,
                        @NonNull final String content, final boolean modified) {
        Item result = new Item(this, quantity, content, modified);
        mItems.add(result);
        return result;
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

        @Override
        @NonNull
        public ResTag clone(String newContent) {
            ResPlurals parent = mParent.fakeClone();
            Item result = parent.addItem(mQuantity, mContent, mModified);
            result.setContent(newContent);
            return result;
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

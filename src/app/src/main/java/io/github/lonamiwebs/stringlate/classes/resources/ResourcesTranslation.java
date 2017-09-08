package io.github.lonamiwebs.stringlate.classes.resources;

import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;

// Represents a ResString-like object, but rather with
// the resource ID, its original value and its translated value for a given locale
public class ResourcesTranslation {

    //region Members

    private final String mResourceId;

    private final String mOriginalValue;
    private final String mTranslatedValue;

    //endregion

    //region Constructor

    public static ArrayList<ResourcesTranslation> fromPairs(
            Resources original, Resources translation, String filter) {
        String id;
        ArrayList<ResourcesTranslation> result = new ArrayList<>();
        if (filter == null) {
            for (ResTag rs : original) {
                id = rs.getId();
                result.add(new ResourcesTranslation(id, rs.getContent(), translation.getContent(id)));
            }
        } else {
            String ori, trn;
            filter = filter.toLowerCase();
            for (ResTag rs : original) {
                id = rs.getId();
                ori = rs.getContent();
                trn = translation.getContent(id);
                if (id.toLowerCase().contains(filter) ||
                        ori.toLowerCase().contains(filter) ||
                        trn.toLowerCase().contains(filter)) {
                    result.add(new ResourcesTranslation(id, ori, trn));
                }
            }
        }
        return result;
    }

    private ResourcesTranslation(String resourceId, String original, String translated) {
        mResourceId = resourceId;
        mOriginalValue = original;
        mTranslatedValue = translated;
    }

    //endregion

    //region Getters

    public String getId() {
        return mResourceId;
    }

    public String getOriginal() {
        return mOriginalValue;
    }

    public String getTranslation() {
        return mTranslatedValue;
    }

    public boolean hasTranslation() {
        return mTranslatedValue != null && !mTranslatedValue.isEmpty();
    }

    //endregion
}

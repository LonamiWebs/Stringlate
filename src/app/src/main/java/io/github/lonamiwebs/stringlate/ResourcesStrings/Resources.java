package io.github.lonamiwebs.stringlate.ResourcesStrings;

import java.util.ArrayList;
import java.util.Iterator;

// Class to manage multiple ResourcesString,
// usually parsed from strings.xml files
public class Resources implements Iterable<ResourcesString> {

    //region Members

    private final ArrayList<ResourcesString> mStrings;

    // Internally keep track if the changes are saved not to look
    // over all the resources strings individually
    boolean mSavedChanges;

    //endregion

    //region Constructors

    public Resources() { this(new ArrayList<ResourcesString>()); }

    public Resources(ArrayList<ResourcesString> strings) {
        mStrings = strings;
        mSavedChanges = true;
    }

    //endregion

    //region Getting content

    public boolean contains(String resourceId) {
        for (ResourcesString rs : mStrings)
            if (rs.getId().equals(resourceId))
                return true;

        return false;
    }

    public String getContent(String resourceId) {
        for (ResourcesString rs : mStrings)
            if (rs.getId().equals(resourceId))
                return rs.getContent();

        return "";
    }

    //endregion

    //region Updating (setting) content

    public void setContent(String resourceId, String content) {
        boolean found = false;
        for (ResourcesString rs : mStrings)
            if (rs.getId().equals(resourceId)) {
                if (rs.setContent(content))
                    mSavedChanges = false;

                found = true;
                break;
            }

        if (!found)
            mStrings.add(new ResourcesString(resourceId, content));
    }

    //endregion

    //region Deleting content

    public void deleteId(String resourceId) {
        for (ResourcesString rs : mStrings)
            if (rs.getId().equals(resourceId)) {
                mStrings.remove(rs);
                break;
            }
    }

    //endregion

    //region Iterator wrapper

    @Override
    public Iterator<ResourcesString> iterator() {
        return mStrings.iterator();
    }

    //endregion
}

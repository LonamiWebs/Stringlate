package io.github.lonamiwebs.stringlate.ResourcesStrings;

import java.util.ArrayList;
import java.util.Iterator;

public class Resources implements Iterable<ResourcesString> {

    private final ArrayList<ResourcesString> mStrings;

    boolean mSavedChanges;

    public Resources() { this(new ArrayList<ResourcesString>()); }

    public Resources(ArrayList<ResourcesString> strings) {
        mStrings = strings;
        mSavedChanges = true;
    }

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

    @Override
    public Iterator<ResourcesString> iterator() {
        return mStrings.iterator();
    }
}

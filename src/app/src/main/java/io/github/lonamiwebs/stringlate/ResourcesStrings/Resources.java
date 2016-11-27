package io.github.lonamiwebs.stringlate.ResourcesStrings;

import java.util.ArrayList;
import java.util.Iterator;

public class Resources implements Iterable<ResourcesString> {

    ArrayList<ResourcesString> mStrings;

    public Resources(ArrayList<ResourcesString> strings) {
        mStrings = strings;
    }

    @Override
    public Iterator<ResourcesString> iterator() {
        return mStrings.iterator();
    }
}

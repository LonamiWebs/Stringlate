package io.github.lonamiwebs.stringlate.classes.resources;

import java.util.Comparator;

import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;

public class ResourceStringComparator {
    public static final int SORT_ALPHABETICALLY = 0;
    public static final int SORT_STRING_LENGTH = 1;

    public static Comparator<ResTag> getStringsComparator(int sortMethod) {
        switch (sortMethod) {
            default:
            case SORT_ALPHABETICALLY:
                return new Comparator<ResTag>() {
                    @Override
                    public int compare(ResTag o1, ResTag o2) {
                        return o1.compareTo(o2);
                    }
                };
            case SORT_STRING_LENGTH:
                return new Comparator<ResTag>() {
                    @Override
                    public int compare(ResTag o1, ResTag o2) {
                        int x = o1.getContentLength();
                        int y = o2.getContentLength();
                        return (x < y) ? -1 : ((x == y) ? 0 : 1);
                    }
                };
        }
    }
}

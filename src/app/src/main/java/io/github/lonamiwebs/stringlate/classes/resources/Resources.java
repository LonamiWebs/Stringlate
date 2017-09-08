package io.github.lonamiwebs.stringlate.classes.resources;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.github.lonamiwebs.stringlate.classes.resources.tags.ResPlurals;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResStringArray;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;

// Class to manage multiple ResTag,
// usually parsed from strings.xml files
public class Resources implements Iterable<ResTag> {

    //region Members

    private final File mFile; // Keep track of the original file to be able to save()
    private final HashMap<String, ResTag> mStrings;
    private final HashMap<String, ResTag> mReferenceStrings; // Those starting with "@string/"

    private ResTag mLastTag; // The last tag returned by getTag()

    private boolean mSavedChanges;
    private boolean mModified;

    @NonNull
    private String mFilter;

    //endregion

    //region Constructors

    @NonNull
    public static Resources fromFile(File file) {
        Resources result = new Resources(file);

        if (file.isFile()) {
            InputStream is = null;
            try {
                is = new FileInputStream(file);
                // Load the resources from the XML into our resulting Resources
                ResourcesParser.loadFromXml(is, result);
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException ignored) {
                }
            }
        }

        // If loading went okay, the resources will have been loaded into the result
        return result;
    }

    // Empty resources cannot be saved
    @NonNull
    public static Resources empty() {
        return new Resources(null);
    }

    private Resources(File file) {
        mFile = file;
        mStrings = new HashMap<>();
        mReferenceStrings = new HashMap<>();
        mSavedChanges = mFile != null && mFile.isFile();
        mFilter = "";
    }

    //endregion

    //region Getting content

    public int count() {
        return mStrings.size();
    }

    public boolean isEmpty() {
        return mStrings.isEmpty();
    }

    public boolean contains(String resourceId) {
        return getTag(resourceId) != null;
    }

    @NonNull
    public String getContent(String resourceId) {
        ResTag tag = getTag(resourceId);
        return tag == null ? "" : tag.getContent();
    }

    public ResTag getTag(String resourceId) {
        if (mLastTag != null && mLastTag.getId().equals(resourceId)) {
            return mLastTag;
        }

        mLastTag = mStrings.get(resourceId);
        if (mLastTag == null && !resourceId.contains(":")) {
            // We might be looking for a parent string, not the ResTag itself
            // This is a fallback to the slightly slower method
            for (Map.Entry<String, ResTag> srt : mStrings.entrySet()) {
                ResTag rt = srt.getValue();

                if (rt instanceof ResStringArray.Item) {
                    if (((ResStringArray.Item) rt).getParent().getId().equals(resourceId))
                        return mLastTag = rt;
                } else if (rt instanceof ResPlurals.Item) {
                    if (((ResPlurals.Item) rt).getParent().getId().equals(resourceId))
                        return mLastTag = rt;
                }
            }
        }

        return mLastTag;
    }

    // Determines whether the resource ID was modified or not
    // If this resource ID doesn't exist, then it obviously wasn't modified
    public boolean wasModified(String resourceId) {
        ResTag rs = getTag(resourceId);
        return rs != null && rs.wasModified();
    }

    //endregion

    //region Updating (setting) content

    public void setContent(ResTag original, @NonNull String content) {
        String resourceId = original == null ? "" : original.getId();
        if (resourceId.isEmpty())
            return;

        // If the content is empty (or null), treat it as deleting this ID
        if (content.isEmpty()) {
            deleteId(resourceId);
            return;
        }

        ResTag rs = getTag(resourceId);
        if (rs != null) {
            if (rs.setContent(content)) {
                mSavedChanges = false;
            }
        } else {
            // We need to treat string arrays and plurals specially
            // For these, we need to find the parent, and if it exists
            // then we need to add the child to the existing parent
            boolean handled = false;
            if (original instanceof ResStringArray.Item) {
                ResStringArray.Item ori = (ResStringArray.Item) original;
                ResStringArray.Item existingChild =
                        (ResStringArray.Item) getTag(ori.getParent().getId());

                if (existingChild != null) {
                    // The parent existed, so add the new string to it, and the
                    // resulting new string to our local array of children
                    ResStringArray parent = existingChild.getParent();
                    ResTag newItem = parent.addItem(content, true, ori.getIndex());
                    mStrings.put(newItem.getId(), newItem);
                    handled = true;
                } // else the parent didn't exist, so behave as the general case

            } else if (original instanceof ResPlurals.Item) {
                ResPlurals.Item ori = (ResPlurals.Item) original;
                ResPlurals.Item existingChild =
                        (ResPlurals.Item) getTag(ori.getParent().getId());

                if (existingChild != null) {
                    // The parent existed, so add the new string to it, and the
                    // resulting new string to our local array of children
                    ResPlurals parent = existingChild.getParent();
                    ResTag newItem = parent.addItem(ori.getQuantity(), content, true);
                    mStrings.put(newItem.getId(), newItem);
                    handled = true;
                } // else the parent didn't exist, so behave as the general case
            }
            if (!handled) {
                ResTag clone = original.clone(content);
                mStrings.put(clone.getId(), clone);
            }
            mSavedChanges = false;
        }
    }

    public void addTag(ResTag rt) {
        // If it's null, there was no old value, so changes won't not saved
        if (mStrings.put(rt.getId(), rt) == null)
            mSavedChanges = false;
    }

    // To be used by the ResourcesParser
    void loadTag(ResTag rt) {
        if (rt.getContent().startsWith("@"))
            mReferenceStrings.put(rt.getId(), rt);
        else
            mStrings.put(rt.getId(), rt);

        mModified |= rt.wasModified();
    }

    //endregion

    //region Deleting content

    public void deleteId(String resourceId) {
        mStrings.remove(resourceId);
        if (mLastTag != null && mLastTag.getId().equals(resourceId))
            mLastTag = null;
    }

    //endregion

    //region File saving and deleting

    public String getFilename() {
        return mFile == null ? "strings.xml" : mFile.getName();
    }

    // Determines whether the file was ever modified or not (any of its strings were modified)
    public boolean wasModified() {
        return mModified;
    }

    // If there are unsaved changes, saves the file
    // If the file was saved successfully or there were no changes to save, returns true
    public boolean save() {
        if (mSavedChanges)
            return true;

        if (mFile == null)
            return false;

        try {
            if (!mFile.getParentFile().isDirectory())
                mFile.getParentFile().mkdirs();

            FileOutputStream out = new FileOutputStream(mFile);
            mSavedChanges = ResourcesParser.parseToXml(this, out);
            mModified = true;
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // We do not want empty files, if it exists and it's empty delete it
        if (mFile.isFile() && mFile.length() == 0)
            mFile.delete();

        return mFile.isFile();
    }

    public boolean delete() {
        boolean ok = mFile != null && mFile.delete();
        if (ok) {
            // If the directory is empty, delete it too
            File parent = mFile.getParentFile();
            String[] children = parent.list();
            if (children == null || children.length == 0) {
                ok = parent.delete();
            }
        }
        return ok;
    }

    //endregion

    //region Iterator filtering

    public void setFilter(@NonNull String filter) {
        mFilter = filter.toLowerCase();
    }

    //endregion

    //region Iterator wrapper

    @Override
    public Iterator<ResTag> iterator() {
        return sortIterator(null);
    }

    public Iterator<ResTag> sortIterator(final Comparator<ResTag> comparator) {
        final ArrayList<ResTag> strings = new ArrayList<>(mStrings.size());
        if (mFilter.isEmpty()) {
            for (Map.Entry<String, ResTag> srt : mStrings.entrySet()) {
                strings.add(srt.getValue());
            }
        } else {
            for (Map.Entry<String, ResTag> srt : mStrings.entrySet()) {
                if (srt.getKey().toLowerCase().contains(mFilter) ||
                        srt.getValue().getContent().toLowerCase().contains(mFilter)) {
                    strings.add(srt.getValue());
                }
            }
        }
        if (comparator != null)
            Collections.sort(strings, comparator);

        return strings.iterator();
    }

    //endregion
}

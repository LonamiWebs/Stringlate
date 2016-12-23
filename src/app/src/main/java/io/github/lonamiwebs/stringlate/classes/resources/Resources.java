package io.github.lonamiwebs.stringlate.classes.resources;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import static io.github.lonamiwebs.stringlate.classes.resources.ResourcesParser.parseToXml;

// Class to manage multiple ResourcesString,
// usually parsed from strings.xml files
public class Resources implements Iterable<ResourcesString> {

    //region Members

    private File mFile; // Keep track of the original file to be able to save()
    private HashSet<ResourcesString> mStrings;
    private HashSet<String> mUnsavedIDs;

    private boolean mSavedChanges;
    private boolean mModified;

    //endregion

    //region Constructors

    public static Resources fromFile(File file) {
        if (!file.isFile())
            return new Resources(file, new HashSet<ResourcesString>());

        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return new Resources(file, ResourcesParser.parseFromXml(is));
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) { }
        }
        return null;
    }

    private Resources(File file, HashSet<ResourcesString> strings) {
        mFile = file;
        mStrings = strings;
        mSavedChanges = file.isFile();

        // Keep track of the unsaved strings not to iterate over the list to count them
        mUnsavedIDs = new HashSet<>();

        // Backwards compatibility with version 0.9
        // If the .modified file exists, assume all the local strings were modified
        // This will allow old users to pull new changes without overwriting local
        // TODO Remove this on version 1.0 (or similar)

        // -- Begin of backwards-compatibility code
        String path = mFile.getAbsolutePath();
        int extensionIndex = path.lastIndexOf('.');
        if (extensionIndex > -1)
            path = path.substring(0, extensionIndex);
        path += ".modified";
        File mModifiedFile = new File(path);

        if (mModifiedFile.isFile()) {
            for (ResourcesString rs : mStrings) {
                String content = rs.getContent();
                // Clear the content and then set the original one
                // so 'modified' equals true
                rs.setContent("");
                rs.setContent(content);
            }
            // Set saved changes = false to force saving
            mSavedChanges = false;
            save();

            // Delete the file, it's now useless
            mModifiedFile.delete();
        }
        // -- End of backwards-compatibility code

        mModified = false;
        for (ResourcesString rs : mStrings) {
            if (rs.wasModified()) {
                mModified = true;
                break;
            }
        }
    }

    //endregion

    //region Getting content

    public int count() {
        return mStrings.size();
    }

    public int unsavedCount() {
        return mUnsavedIDs.size();
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

    //endregion

    //region Updating (setting) content

    public void setContent(String resourceId, String content) {
        if (resourceId == null || resourceId.isEmpty())
            return;

        boolean found = false;
        for (ResourcesString rs : mStrings)
            if (rs.getId().equals(resourceId)) {
                if (rs.setContent(content)) {
                    mSavedChanges = false;
                    mUnsavedIDs.add(resourceId);
                }

                found = true;
                break;
            }

        // We don't want to set an empty string unless we're
        // clearing an existing one since it's unnecessary
        if (!found && !content.isEmpty()) {
            ResourcesString string = new ResourcesString(resourceId);
            string.setContent(content); // Will also update its modified = true state
            mStrings.add(string);

            mSavedChanges = false;
        }
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

    //region File saving and deleting

    public String getFilename() {
        return mFile.getName();
    }

    // Determines whether the files was saved or not
    public boolean areSaved() {
        return mSavedChanges;
    }

    // Determines whether the file was ever modified or not (any of its strings were modified)
    public boolean wasModified() { return mModified; }

    // If there are unsaved changes, saves the file
    // If the file was saved successfully or there were no changes to save, returns true
    public boolean save() {
        if (mSavedChanges)
            return true;

        try {
            FileOutputStream out = new FileOutputStream(mFile);
            // Always add metadata when saving locally
            mSavedChanges = save(out, true);
            mModified = true;
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // We do not want empty files, if it exists and it's empty delete it
        if (mFile.isFile() && mFile.length() == 0)
            mFile.delete();

        // Clear the unsaved IDs if we succeeded
        if (mSavedChanges)
            mUnsavedIDs.clear();

        return mFile.isFile();
    }

    public boolean save(OutputStream out) {
        // Never save metadata if saving to a given stream.
        // Only save the metadata when saving locally.
        return save(out, false);
    }

    private boolean save(OutputStream out, boolean addMetadata) {
        return ResourcesParser.parseToXml(this, out, false, addMetadata);
    }

    public void delete() {
        mFile.delete();
    }

    //endregion

    //region Iterator wrapper

    @Override
    public Iterator<ResourcesString> iterator() {
        ArrayList<ResourcesString> strings = new ArrayList<>(mStrings);
        Collections.sort(strings);
        return strings.iterator();
    }

    //endregion

    //region To string

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean indent) {
        OutputStream out = new ByteArrayOutputStream();
        // Never add the metadata when converting to a string
        parseToXml(this, out, indent, false);
        return out.toString();
    }

    //endregion
}

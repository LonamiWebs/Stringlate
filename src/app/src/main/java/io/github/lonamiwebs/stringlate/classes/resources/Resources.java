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
    private File mModifiedFile; // ".modified" file, used to tell if the user saved it before
    private HashSet<ResourcesString> mStrings;

    private boolean mSavedChanges;

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

        String path = mFile.getAbsolutePath();
        int extensionIndex = path.lastIndexOf('.');
        if (extensionIndex > -1)
            path = path.substring(0, extensionIndex);
        path += ".modified";
        mModifiedFile = new File(path);
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
        if (resourceId == null || resourceId.isEmpty())
            return;

        boolean found = false;
        for (ResourcesString rs : mStrings)
            if (rs.getId().equals(resourceId)) {
                if (rs.setContent(content))
                    mSavedChanges = false;

                found = true;
                break;
            }

        // We don't want to set an empty string unless we're
        // clearing an existing one since it's unnecessary
        if (!found && !content.isEmpty()) {
            mStrings.add(new ResourcesString(resourceId, content));
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

    // If there are unsaved changes, saves the file
    // If the file was saved successfully or there were no changes to save, returns true
    public boolean save() {
        if (mSavedChanges)
            return true;

        try {
            FileOutputStream out = new FileOutputStream(mFile);
            mSavedChanges = save(out);
            out.close();

            // Also create a ".modified" file so we know this locale was modified
            // We need to somehow keep track of which files we modified before syncing
            mModifiedFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // We do not want empty files, if it exists and it's empty delete it
        if (mFile.isFile() && mFile.length() == 0)
            mFile.delete();

        return mFile.isFile();
    }

    public boolean save(OutputStream out) {
        return ResourcesParser.parseToXml(this, out, false);
    }

    // Determines whether the file was modified or not (.save() has ever been called)
    public boolean wasModified() { return mModifiedFile.isFile(); }

    public void delete() {
        if (mModifiedFile.isFile())
            mModifiedFile.delete();

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
        parseToXml(this, out, indent);
        return out.toString();
    }

    //endregion
}

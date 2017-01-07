package io.github.lonamiwebs.stringlate.classes.resources;

import android.support.annotation.NonNull;
import android.util.Pair;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import io.github.lonamiwebs.stringlate.classes.resources.tags.ResString;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;

// Class to manage multiple ResTag,
// usually parsed from strings.xml files
public class Resources implements Iterable<ResTag> {

    //region Members

    private final File mFile; // Keep track of the original file to be able to save()
    private final HashSet<ResTag> mStrings;
    private final HashSet<String> mUnsavedIDs;
    private final String mRemoteUrl;

    private boolean mSavedChanges;
    private boolean mModified;

    //endregion

    //region Constructors

    public static Resources fromFile(File file) {
        return fromFile(file, null);
    }

    public static Resources fromFile(File file, File rootDir) {
        String remoteUrl = "";
        if (rootDir != null) {
            remoteUrl = file.getAbsolutePath().substring(rootDir.getAbsolutePath().length());
            if (remoteUrl.startsWith("/"))
                remoteUrl = remoteUrl.substring(1);
        }

        if (!file.isFile())
            return new Resources(file, new HashSet<ResTag>(), remoteUrl);

        InputStream is = null;
        try {
            is = new FileInputStream(file);
            Pair<HashSet<ResTag>, String> result = ResourcesParser.parseFromXml(is);
            if (result.second == null) {
                // Keep the remote url that we have
                return new Resources(file, result.first, remoteUrl);
            } else {
                // Keep the saved remote url
                return new Resources(file, result.first, result.second);
            }
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ignored) { }
        }
        return null;
    }

    private Resources(File file, HashSet<ResTag> strings, @NonNull String remoteUrl) {
        mFile = file;
        mStrings = strings;
        mSavedChanges = file.isFile();
        mRemoteUrl = remoteUrl;

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
            for (ResTag rs : mStrings) {
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
        for (ResTag rs : mStrings) {
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
        for (ResTag rs : mStrings)
            if (rs.getId().equals(resourceId))
                return true;

        return false;
    }

    public String getContent(String resourceId) {
        for (ResTag rs : mStrings)
            if (rs.getId().equals(resourceId))
                return rs.getContent();

        return "";
    }

    @NonNull public String getRemoteUrl() {
        return mRemoteUrl;
    }

    //endregion

    //region Updating (setting) content

    public void setContent(String resourceId, @NonNull String content) {
        if (resourceId == null || resourceId.isEmpty())
            return;

        // If the content is empty (or null), treat it as deleting this ID
        if (content.isEmpty()) {
            deleteId(resourceId);
            return;
        }

        boolean found = false;
        for (ResTag rs : mStrings)
            if (rs.getId().equals(resourceId)) {
                if (rs.setContent(content)) {
                    mSavedChanges = false;
                    mUnsavedIDs.add(resourceId);
                }

                found = true;
                break;
            }

        if (!found) {
            // TODO Uhm… Maybe pass the original resource…? But how do I handle parents…?
            // An option perhaps would be to pass the original string -> .clone() -> .setContent
            // But actually, this works, because the ID is the same (despite being an
            // item from a strings array or not), and when exporting I simply look for
            // the required ID. But although this works I should make this better.
            ResTag string = new ResString(resourceId);
            string.setContent(content); // Will also update its modified = true state
            mStrings.add(string);

            mSavedChanges = false;
            mUnsavedIDs.add(resourceId);
        }
    }

    //endregion

    //region Deleting content

    public void deleteId(String resourceId) {
        for (ResTag rs : mStrings)
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

    public boolean forceSave() {
        mSavedChanges = false;
        return save();
    }

    // If there are unsaved changes, saves the file
    // If the file was saved successfully or there were no changes to save, returns true
    public boolean save() {
        if (mSavedChanges)
            return true;

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

        // Clear the unsaved IDs if we succeeded
        if (mSavedChanges)
            mUnsavedIDs.clear();

        return mFile.isFile();
    }

    // TODO Should this delete the parent directory? Not if there were more .xml files though…
    public void delete() {
        mFile.delete();
    }

    //endregion

    //region Iterator wrapper

    @Override
    public Iterator<ResTag> iterator() {
        ArrayList<ResTag> strings = new ArrayList<>(mStrings);
        Collections.sort(strings);
        return strings.iterator();
    }

    //endregion
}

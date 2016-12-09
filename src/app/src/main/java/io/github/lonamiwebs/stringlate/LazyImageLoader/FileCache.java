package io.github.lonamiwebs.stringlate.LazyImageLoader;

import android.content.Context;

import java.io.File;

// Original code at https://github.com/thest1/LazyList (http://stackoverflow.com/a/3068012/4759433)
public class FileCache {

    //region Members

    private File mCacheDir;

    //endregion

    //region Constructor

    public FileCache(Context context, String name) {
        mCacheDir = new File(context.getCacheDir(), name);
        if (!mCacheDir.exists())
            mCacheDir.mkdirs();
    }

    //endregion

    //region Public methods

    // Retrieves the local file for the given URL
    public File getFile(String url) {
        return new File(mCacheDir, url.substring(url.lastIndexOf('/')));
    }

    // Clears the file cache
    public void clear() {
        File[] files = mCacheDir.listFiles();
        if (files != null)
            for (File f : files)
                f.delete();
    }

    //endregion
}

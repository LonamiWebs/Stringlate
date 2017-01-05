package io.github.lonamiwebs.stringlate.classes.lazyloader;

import android.content.Context;

import java.io.File;

import io.github.lonamiwebs.stringlate.R;

// Original code at https://github.com/thest1/LazyList (http://stackoverflow.com/a/3068012/4759433)
public class FileCache {

    //region Members

    private final File mCacheDir;

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
    public long clear() {
        long size;
        long cleared = 0;

        File[] files = mCacheDir.listFiles();
        if (files != null) {
            for (File f : files) {
                size = f.length();
                if (f.delete())
                    cleared += size;
            }
        }
        return cleared;
    }

    public static String getHumanReadableSize(Context ctx, long sizeInBytes) {
        String[] suffixes = new String[] {
                // Some day phones will have peta bytes, and then I'll laugh
                "bytes", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB"
        };
        int i = 0;
        double size = sizeInBytes;
        while (size > 1024.0) {
            size /= 1024.0;
            i++;
        }

        return ctx.getString(R.string.bytes_size_format, size, suffixes[i]);
    }

    //endregion
}

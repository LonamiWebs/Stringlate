package io.github.lonamiwebs.stringlate.LazyImageLoader;

import android.graphics.Bitmap;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

// Original code at https://github.com/thest1/LazyList (http://stackoverflow.com/a/3068012/4759433)
public class MemoryCache {

    //region Constant fields

    private static final int INITIAL_CAPACITY = 10;
    private static final float LOAD_FACTOR = 1.5f; // I honestly have no idea what this is for
    private static final float MAXIMUM_MEMORY_PERCENTAGE = 0.2f;

    //endregion

    //region Members

    private Map<String, Bitmap> mCache;

    // Memory usage and limit
    private long mUsed;
    private long mLimit;

    //endregion

    //region Constructor

    public MemoryCache() {
        setLimit((long)(Runtime.getRuntime().maxMemory() * MAXIMUM_MEMORY_PERCENTAGE));

        // Last argument true for Least Recently Used ordering
        mCache = Collections.synchronizedMap(new LinkedHashMap<String, Bitmap>(
                INITIAL_CAPACITY, LOAD_FACTOR, true));
    }

    //endregion

    //region Public methods

    // Sets the memory usage limit
    public void setLimit(long limit) {
        boolean needCheck = limit < mLimit;
        mLimit = limit;
        if (needCheck)
            checkUsedSize();
    }

    // Gets the stored bitmap by its identifier, may be null if it was never put
    public Bitmap get(String id) {
        if (!mCache.containsKey(id))
            return null;

        return mCache.get(id);
    }

    // Puts the given bitmap into memory with a given identifier
    public void put(String id, Bitmap bitmap) {
        try{
            if (mCache.containsKey(id))
                mUsed -= getSizeInBytes(mCache.get(id));

            mCache.put(id, bitmap);
            mUsed += getSizeInBytes(bitmap);
            checkUsedSize();
        }catch(Throwable th) {
            th.printStackTrace();
        }
    }

    // Clears the cache
    public void clear() {
        mCache.clear();
        mUsed = 0;
    }

    //endregion

    //region Private methods

    // Checks the used memory size to ensure we're inside the limits
    private void checkUsedSize() {
        if (mUsed > mLimit) {
            // Least recently accessed item will be the first one iterated
            Iterator<Entry<String, Bitmap>> iter = mCache.entrySet().iterator();
            while (iter.hasNext()) {
                mUsed -= getSizeInBytes(iter.next().getValue());
                iter.remove();
                if (mUsed < mLimit)
                    break;
            }
        }
    }

    // Gets the bitmap size in bytes
    private long getSizeInBytes(Bitmap bitmap) {
        return bitmap == null ? 0 : bitmap.getRowBytes() * bitmap.getHeight();
    }

    //endregion
}

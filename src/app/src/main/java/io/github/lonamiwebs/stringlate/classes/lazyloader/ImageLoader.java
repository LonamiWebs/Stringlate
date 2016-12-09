package io.github.lonamiwebs.stringlate.classes.lazyloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Pair;
import android.widget.ImageView;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.utilities.FileDownloader;

// Original code at https://github.com/thest1/LazyList (http://stackoverflow.com/a/3068012/4759433)
public class ImageLoader {

    //region Constant fields

    private final static int MAX_THREADS = 4;
    private final static int STUB_ID = R.drawable.app_not_found;

    //endregion

    //region Members

    private MemoryCache mMemoryCache;
    private FileCache mFileCache;
    private Map<ImageView, String> mImageViews;

    private ExecutorService mExecutorService;
    private Handler mHandler;

    //endregion

    //region Constructor

    public ImageLoader(Context context) {
        mMemoryCache = new MemoryCache();
        mFileCache = new FileCache(context, "icons");
        mImageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());

        mExecutorService = Executors.newFixedThreadPool(MAX_THREADS);
        mHandler = new Handler();
    }

    //endregion

    //region Public methods

    // Loads the given URL image into the image view async,
    // downloading it if the image has not been cached yet
    public void loadImageAsync(ImageView imageView, String url) {
        mImageViews.put(imageView, url);
        Bitmap bitmap = mMemoryCache.get(url);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(STUB_ID);
            queuePhoto(imageView, url);
        }
    }

    // Clears both the memory and file cache
    public long clearCache() {
        mMemoryCache.clear();
        return mFileCache.clear();
    }

    //endregion

    //region Private methods

    // Enqueues a new URL to be downloaded and set to the image view
    private void queuePhoto(ImageView imageView, String url) {
        mExecutorService.submit(new ImageLoaderRunnable(imageView, url));
    }

    // Downloads the image from the given URL if it has not been cached yet,
    // and then it returns the resulting bitmap loaded from the saved file
    private Bitmap getBitmap(String url) {
        File f = mFileCache.getFile(url);

        // If we don't have the file cached, download it first
        if (!f.isFile()) {
            if (!FileDownloader.downloadFile(url, f))
                mMemoryCache.clear();
        }

        return f.isFile() ? BitmapFactory.decodeFile(f.getAbsolutePath()) : null;
    }

    // I don't quite understand this method, but it works and it's not my code!
    private boolean isImageViewReused(Pair<ImageView, String> photoToLoad) {
        String urlTag = mImageViews.get(photoToLoad.first);
        return urlTag == null || !urlTag.equals(photoToLoad.second);
    }

    //endregion

    //region Sub classes

    // We need to run this in a different thread because
    // it may download images from the network
    private class ImageLoaderRunnable implements Runnable {
        Pair<ImageView, String> mImageToLoad;

        ImageLoaderRunnable(ImageView imageView, String url) {
            mImageToLoad = new Pair<>(imageView, url);
        }

        @Override
        public void run() {
            try {
                if (isImageViewReused(mImageToLoad))
                    return;

                final Bitmap bmp = getBitmap(mImageToLoad.second);
                mMemoryCache.put(mImageToLoad.second, bmp);
                if (isImageViewReused(mImageToLoad))
                    return;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isImageViewReused(mImageToLoad)) {
                            if (bmp != null)
                                mImageToLoad.first.setImageBitmap(bmp);
                            else
                                mImageToLoad.first.setImageResource(STUB_ID);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //endregion
}

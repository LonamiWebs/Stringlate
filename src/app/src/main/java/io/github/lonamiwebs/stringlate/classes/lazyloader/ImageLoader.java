package io.github.lonamiwebs.stringlate.classes.lazyloader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Pair;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private final Context mContext;

    private final MemoryCache mMemoryCache;
    private final FileCache mFileCache;
    private final Map<ImageView, String> mImageViews;

    private final ExecutorService mExecutorService;
    private final Handler mHandler;

    public boolean mAllowInternetDownload;

    //endregion

    //region Constructor

    public ImageLoader(Context context, boolean allowInternetDownload) {
        mContext = context;

        mMemoryCache = new MemoryCache();
        mFileCache = new FileCache(context, "icons");
        mImageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());

        mExecutorService = Executors.newFixedThreadPool(MAX_THREADS);
        mHandler = new Handler();

        mAllowInternetDownload = allowInternetDownload;
    }

    //endregion

    //region Public methods

    // Loads the given URL image into the image view async,
    // downloading it if the image has not been cached yet
    // If the application is not installed on the device, installedPackage should be null
    public void loadImageAsync(ImageView imageView, String url, String installedPackage) {
        mImageViews.put(imageView, url);
        Bitmap bitmap = mMemoryCache.get(url);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(STUB_ID);
            queuePhoto(imageView, url, installedPackage);
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
    private void queuePhoto(ImageView imageView, String url, String packageName) {
        mExecutorService.submit(new ImageLoaderRunnable(imageView, url, packageName));
    }

    // Downloads the image from the given URL if it has not been cached yet,
    // and then it returns the resulting bitmap loaded from the saved file
    private Bitmap getBitmap(String url, String packageName) {
        File f = mFileCache.getFile(url);

        // If we don't have the file cached, check if we can retrieve it
        if (!f.isFile() && packageName != null) {
            try {
                PackageManager pm = mContext.getPackageManager();
                Drawable icon = pm.getApplicationIcon(packageName);
                FileOutputStream out = null;
                try {
                    // Cache the icon not to retrieve it from the package manager
                    out = new FileOutputStream(f);
                    drawableToBitmap(icon).compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (!f.isFile() && mAllowInternetDownload) {
            if (!FileDownloader.downloadFile(url, f))
                mMemoryCache.clear();
        }

        return f.isFile() ? BitmapFactory.decodeFile(f.getAbsolutePath()) : null;
    }

    // http://stackoverflow.com/a/10600736/4759433
    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null)
                return bitmapDrawable.getBitmap();
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap
        } else {
            bitmap = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
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
        final Pair<ImageView, String> mImageToLoad;
        final String mPackageName;

        ImageLoaderRunnable(ImageView imageView, String url, String packageName) {
            mImageToLoad = new Pair<>(imageView, url);
            mPackageName = packageName;
        }

        @Override
        public void run() {
            try {
                if (isImageViewReused(mImageToLoad))
                    return;

                final Bitmap bmp = getBitmap(mImageToLoad.second, mPackageName);
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

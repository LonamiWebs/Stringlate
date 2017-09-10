package io.github.lonamiwebs.stringlate.classes.repos;

import android.content.Context;
import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.classes.Messenger;
import io.github.lonamiwebs.stringlate.interfaces.StringsSource;

public class RepoHandlerHelper {

    private static final String BASE_DIR = "repos";

    private static File getWorkDir(final Context context) {
        return new File(context.getFilesDir(), BASE_DIR);
    }

    public static RepoHandler fromContext(final Context context, final String source) {
        return new RepoHandler(source, getWorkDir(context), context.getCacheDir());
    }

    public static RepoHandler fromBundle(final Bundle bundle) {
        final String root = bundle.getString("root");
        final String cache = bundle.getString("cache");
        if (root == null || cache == null)
            throw new IllegalArgumentException("Cannot construct RepoHandler from invalid bundle");

        return new RepoHandler(new File(root), new File(cache));
    }

    public static Bundle toBundle(final RepoHandler repo) {
        final Bundle result = new Bundle();
        result.putString("root", repo.mRoot.getAbsolutePath());
        result.putString("cache", repo.mCacheDir.getAbsolutePath());
        return result;
    }

    public static ArrayList<RepoHandler> listRepositories(final Context context) {
        return RepoHandler.listRepositories(getWorkDir(context), context.getCacheDir());
    }

    public static boolean syncResources(final Context context, final RepoHandler repo,
                                        final StringsSource source, final Messenger.OnSyncProgress callback) {
        return repo.syncResources(source, context.getResources().getDisplayMetrics().densityDpi, callback);
    }
}

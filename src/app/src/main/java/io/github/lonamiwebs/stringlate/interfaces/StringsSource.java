package io.github.lonamiwebs.stringlate.interfaces;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.git.GitCloneProgressCallback;

public interface StringsSource {

    // Sources may need to prepare some files first, e.g. strings from git repositories
    // need to pull the repository itself in order to get access to the files.
    // TODO It shouldn't be a "git" callback
    boolean setup(final Context context, final GitCloneProgressCallback callback);

    // Should retrieve a list of all locales available for this source
    @NonNull
    List<String> getLocales();

    // Returns all the available, non-default resources for the specified locale.
    @NonNull
    Resources getResources(final @NonNull String locale);

    // Returns pairs of (default resource name, resources)
    @NonNull
    Map<String, Resources> getDefaultResources();

    // May return a File pointing to an existing icon for this source, or null
    File getIcon();

    // Any temporary resource used by the source should be cleaned up here.
    void dispose();
}

package io.github.lonamiwebs.stringlate.interfaces;

import java.io.File;
import java.util.List;

import io.github.lonamiwebs.stringlate.classes.Messenger;
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.sources.SourceSettings;

public interface StringsSource {

    // Sources may need to prepare some files first, e.g. strings from git repositories
    // need to pull the repository itself in order to get access to the files.
    boolean setup(final SourceSettings settings,
                  final File workDir,
                  final int desiredIconDpi,
                  final Messenger.OnSyncProgress callback);

    // Indicates that the setup operation should be cancelled
    void cancel();

    // The name for a certain StringsSource, e.g. "git". Must be non-null
    String getName();

    // Should retrieve a list of all locales available for this source. Must be non-null
    List<String> getLocales();

    // Returns all the available, non-default resources for the specified locale. Must be non-null
    Resources getResources(final String locale);

    // Returns all the names for available default resources (since these are named). Must be non-null
    List<String> getDefaultResources();

    // Given the name of a default resource, loads and returns its contents. Must be non-null
    Resources getDefaultResource(final String name);

    // Some developers have a well-defined XML structure already made,
    // for instance for GitSources where the XML itself is known, for
    // their strings.xml files. If this is the case this method should
    // return said XML, otherwise, it can return null.
    String getDefaultResourceXml(final String name);

    // May return a File pointing to an existing icon for this source, or null
    File getIcon();

    // Any temporary resource used by the source should be cleaned up here.
    void dispose();
}

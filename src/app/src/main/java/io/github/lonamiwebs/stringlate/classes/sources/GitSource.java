package io.github.lonamiwebs.stringlate.classes.sources;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;
import io.github.lonamiwebs.stringlate.git.GitCloneProgressCallback;
import io.github.lonamiwebs.stringlate.git.GitWrapper;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.interfaces.StringsSource;
import io.github.lonamiwebs.stringlate.settings.SourceSettings;
import io.github.lonamiwebs.stringlate.utilities.Utils;

public class GitSource implements StringsSource {

    private File mTmpCloneDir;
    private final String mGitUrl, mBranch;
    private final HashMap<String, ArrayList<File>> mLocaleFiles;

    private File iconFile;

    // Match locale from "values-(…)/strings.xml"
    private final static Pattern VALUES_LOCALE_PATTERN =
            Pattern.compile("values(?:-([\\w-]+))?/.+?\\.xml");

    // Match "dontranslate.xml", "do-not-translate.xml", "donottranslate.xml" and such
    private static final Pattern DO_NOT_TRANSLATE = Pattern.compile(
            "(?:do?[ _-]*no?t?|[u|i]n)[ _-]*trans(?:lat(?:e|able))?");


    public GitSource(final String gitUrl, final String branch) {
        mGitUrl = gitUrl;
        mBranch = branch;
        mLocaleFiles = new HashMap<>();
    }

    @Override
    public boolean setup(final Context context, final SourceSettings settings, final ProgressUpdateCallback callback) {
        // 1. Prepare to clone the repository
        callback.onProgressUpdate(
                context.getString(R.string.cloning_repo),
                context.getString(R.string.cloning_repo_progress, 0.0f)
        );

        settings.set("git_url", mGitUrl);
        mTmpCloneDir = new File(context.getCacheDir(), "tmp_clone");
        Utils.deleteRecursive(mTmpCloneDir); // Don't care, it's temp and it can't exist on cloning

        // 2. Clone the repository itself
        if (!GitWrapper.cloneRepo(
                mGitUrl, mTmpCloneDir, mBranch,
                new GitCloneProgressCallback(context, callback))) {
            callback.showMessage(context.getString(R.string.invalid_repo));
            return false;
        }

        // 3. Scan resources
        callback.onProgressUpdate(
                context.getString(R.string.scanning_repository),
                context.getString(R.string.scanning_repository_long)
        );

        final ArrayList<File> resourceFiles = GitWrapper.searchAndroidResources(mTmpCloneDir);

        if (resourceFiles.isEmpty()) {
            callback.showMessage(context.getString(R.string.no_strings_found));
            return false;
        }

        // Save the branches of this repository
        settings.setArray("remote_branches", GitWrapper.getBranches(mTmpCloneDir));

        iconFile = GitWrapper.findProperIcon(mTmpCloneDir, context);

        // Iterate over all the found resources to sort them by locale
        for (File resourceFile : resourceFiles) {
            Matcher m = VALUES_LOCALE_PATTERN.matcher(resourceFile.getAbsolutePath());

            // Ensure that we can tell the locale from the path (otherwise it's invalid)
            if (m.find()) {
                if (m.group(1) == null) { // Default locale
                    // If this is the default locale, TODO save its remote path
                    // If the file name is something like "do not translate", skip it
                    if (DO_NOT_TRANSLATE.matcher(resourceFile.getName()).find())
                        continue;
                }

                // TODO Can I use .getOrDefault()? It shows an error so let's not risk
                if (!mLocaleFiles.containsKey(m.group(1)))
                    mLocaleFiles.put(m.group(1), new ArrayList<File>());

                mLocaleFiles.get(m.group(1)).add(resourceFile);

            }
        }

        settings.set("translation_service", GitWrapper.mayUseTranslationServices(mTmpCloneDir));

        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "git";
    }

    @NonNull
    @Override
    public List<String> getLocales() {
        final ArrayList<String> result = new ArrayList<>(mLocaleFiles.size());
        for (String locale : mLocaleFiles.keySet())
            if (locale != null)
                result.add(locale);
        return result;
    }

    @NonNull
    @Override
    public Resources getResources(@NonNull final String locale) {
        final Resources result = Resources.empty();
        for (File file : mLocaleFiles.get(locale)) {
            for (ResTag rt : Resources.fromFile(file))
                result.addTag(rt);
        }
        return result;
    }

    @NonNull
    @Override
    public Map<String, Resources> getDefaultResources() {
        HashMap<String, Resources> result = new HashMap<>();
        for (File file : mLocaleFiles.get(null)) {
            result.put(getDefaultResourceName(file), Resources.fromFile(file));
        }
        return result;
    }

    @Override
    public String getDefaultResourceXml(String name) {
        for (File file : mLocaleFiles.get(null)) {
            if (getDefaultResourceName(file).equals(name)) {
                return Utils.readFile(file);
            }
        }
        return null;
    }

    @Override
    public File getIcon() {
        return iconFile;
    }

    @NonNull
    private String getDefaultResourceName(final File file) {
        return file.getAbsolutePath().substring(mTmpCloneDir.getAbsolutePath().length() + 1);
    }

    @Override
    public void dispose() {
        Utils.deleteRecursive(mTmpCloneDir);
        mLocaleFiles.clear();
        iconFile = null;
    }
}

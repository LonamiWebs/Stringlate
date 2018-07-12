package io.github.lonamiwebs.stringlate.classes.sources;

import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.classes.Messenger;
import io.github.lonamiwebs.stringlate.classes.git.GitCloneProgressCallback;
import io.github.lonamiwebs.stringlate.classes.git.GitWrapper;
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;
import io.github.lonamiwebs.stringlate.interfaces.StringsSource;

public class GitSource implements StringsSource {

    private File mWorkDir;
    private final String mGitUrl, mBranch;
    private final HashMap<String, ArrayList<File>> mLocaleFiles;
    private GitCloneProgressCallback mCloneCallback;
    private boolean mCancelled;

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
    public boolean setup(final SourceSettings settings, final File workDir,
                         final int desiredIconDpi,
                         final Messenger.OnSyncProgress callback) {
        callback.onUpdate(1, 0f);

        settings.set("git_url", mGitUrl);
        mWorkDir = workDir;

        // 2. Clone the repository itself
        mCloneCallback = new GitCloneProgressCallback(callback);
        if (!GitWrapper.cloneRepo(
                mGitUrl, mWorkDir, mBranch, mCloneCallback) || mCancelled) {
            // TODO These messages are still useful, show them somehow?
            //callback.showMessage(context.getString(R.string.invalid_repo));
            return false;
        }

        // Cache all the repository resources here for faster look-up on upcoming methods
        final GitWrapper.RepositoryResources repoResources =
                GitWrapper.findUsefulResources(mWorkDir);

        final ArrayList<File> resourceFiles = GitWrapper.searchAndroidResources(repoResources);
        if (resourceFiles.isEmpty() || mCancelled) {
            //callback.showMessage(context.getString(R.string.no_strings_found));
            return false;
        }

        // Save the branches of this repository
        settings.setArray("remote_branches", GitWrapper.getBranches(mWorkDir));

        iconFile = GitWrapper.findProperIcon(repoResources, desiredIconDpi);

        // Iterate over all the found resources to sort them by locale
        for (File resourceFile : resourceFiles) {
            Matcher m = VALUES_LOCALE_PATTERN.matcher(resourceFile.getAbsolutePath());

            // Ensure that we can tell the locale from the path (otherwise it's invalid)
            if (m.find()) {
                if (m.group(1) == null) { // Default locale
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

        settings.set("translation_service", GitWrapper.mayUseTranslationServices(repoResources));
        return !mCancelled;
    }

    @Override
    public void cancel() {
        if (mCloneCallback != null) {
            mCloneCallback.cancel();
        }
        mCancelled = true;
    }

    @Override
    public String getName() {
        return "git";
    }

    @Override
    public List<String> getLocales() {
        final ArrayList<String> result = new ArrayList<>(mLocaleFiles.size());
        for (String locale : mLocaleFiles.keySet())
            if (locale != null)
                result.add(locale);
        return result;
    }

    @Override
    public Resources getResources(final String locale) {
        final Resources result = Resources.empty();
        for (File file : mLocaleFiles.get(locale)) {
            for (ResTag rt : Resources.fromFile(file))
                result.addTag(rt);
        }
        return result;
    }

    @Override
    public List<String> getDefaultResources() {
        final ArrayList<String> result = new ArrayList<>(mLocaleFiles.get(null).size());
        for (File file : mLocaleFiles.get(null))
            result.add(getDefaultResourceName(file));

        return result;
    }

    @Override
    public Resources getDefaultResource(String name) {
        for (File file : mLocaleFiles.get(null))
            if (getDefaultResourceName(file).equals(name))
                return Resources.fromFile(file);

        throw new IllegalArgumentException("No default resources were found with that name");
    }

    @Override
    public String getDefaultResourceXml(String name) {
        for (File file : mLocaleFiles.get(null))
            if (getDefaultResourceName(file).equals(name))
                return FileUtils.readTextFile(file);

        throw new IllegalArgumentException("No XML was found with that name");
    }

    @Override
    public File getIcon() {
        return iconFile;
    }

    private String getDefaultResourceName(final File file) {
        return file.getAbsolutePath().substring(mWorkDir.getAbsolutePath().length() + 1);
    }

    @Override
    public void dispose() {
        FileUtils.deleteRecursive(mWorkDir);
        mLocaleFiles.clear();
        iconFile = null;
    }
}

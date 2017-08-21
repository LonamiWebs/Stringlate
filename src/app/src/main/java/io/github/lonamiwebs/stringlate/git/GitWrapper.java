package io.github.lonamiwebs.stringlate.git;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Pair;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.utilities.Utils;

public class GitWrapper {

    private static final String MASTER_BRANCH = "master";
    private static final String REMOTE_NAME = "origin";

    private static final String BASE_GITHUB_URL = "https://github.com/%s/%s";

    // Only the '.xml' extension is case insensitive
    // No regex is required for '<string', which includes '<string-array'
    private static final Pattern PATTERN_XML = Pattern.compile("\\.xml$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_README =
            Pattern.compile("(?:read|le[ea])[-_.]?me(?:\\.(?:md|rst|txt))?", Pattern.CASE_INSENSITIVE);

    private static final String STR_STRING = "<string";
    private static final String STR_PLURALS = "<plurals";
    private static final String[] STR_TRANSLATION_SERVICES = {
            "transifex", "crowdin", "weblate", "zanata", "pootle"
    };

    // GitHub URLs (and GitLab) are well-known
    private static final Pattern OWNER_REPO = Pattern.compile(
            "(?:https?://|git@)(git(?:hub|lab).com)[/:]([\\w-]+)/([\\w-]+)(?:/.*|\\.git)?");

    public static String buildGitHubUrl(String owner, String repository) {
        return getGitUri(String.format(BASE_GITHUB_URL, owner, repository));
    }

    // Attempts to convert a normal url to a git valid one
    public static String getGitUri(String url) {
        url = url.trim();

        if (url.startsWith("git://")) {
            return url;
        } else if (url.contains("github") || url.contains("gitlab")) {
            // Since we know how these URLs work, match the owner and the repository name.
            // This will allow us to handle any URL on the site (even "/issues", "/wiki"…)
            Matcher m = OWNER_REPO.matcher(url);
            if (m.matches()) {
                String host = m.group(1);
                String owner = m.group(2);
                String repo = m.group(3);
                return String.format("https://%s/%s/%s.git", host, owner, repo);
            }
        }

        // Hope that the url is valid
        return url;
    }

    public static Pair<String, String> getGitHubOwnerRepo(final String url)
            throws InvalidObjectException {
        Matcher m = OWNER_REPO.matcher(url);
        if (m.matches() && m.group(1).equalsIgnoreCase("github.com")) {
            return new Pair<>(m.group(2), m.group(3));
        }
        throw new InvalidObjectException("Not a GitHub repository.");
    }

    public static boolean cloneRepo(final String uri, final File cloneTo,
                                    @NonNull final String branch,
                                    final GitCloneProgressCallback callback) {
        Git result = null;

        try {
            final CloneCommand clone = Git.cloneRepository()
                    .setURI(uri).setDirectory(cloneTo)
                    .setBare(false).setRemote(REMOTE_NAME).setNoCheckout(false)
                    .setCloneAllBranches(false).setCloneSubmodules(false)
                    .setProgressMonitor(callback);

            if (!branch.isEmpty()) {
                if (branch.contains("/")) {
                    clone.setBranch(branch.substring(branch.lastIndexOf('/') + 1));
                } else {
                    clone.setBranch(branch);
                }
            }

            result = clone.call();
            return true;
        } catch (GitAPIException e) {
            e.printStackTrace();
        } finally {
            if (result != null) {
                result.close();
            }
        }
        return false;
    }

    @NonNull
    public static ArrayList<String> getBranches(final File repo) {
        try {
            final List<Ref> refs = Git.open(repo)
                    .branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();

            final ArrayList<String> result = new ArrayList<String>();
            for (Ref ref : refs)
                result.add(ref.getName());

            return result;
        } catch (GitAPIException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    //region Searching Android resources

    public static ArrayList<File> searchAndroidResources(File dir) {
        ArrayList<File> result = new ArrayList<>();
        searchAndroidResources(dir, result);
        return result;
    }

    private static void searchAndroidResources(File dir, ArrayList<File> result) {
        if (!dir.getName().startsWith(".")) {
            if (dir.isDirectory()) {
                for (File child : dir.listFiles())
                    searchAndroidResources(child, result);
            } else {
                // dir is a file really
                if (PATTERN_XML.matcher(dir.getName()).find()) {
                    if (Utils.fileContains(dir, STR_STRING, STR_PLURALS) != -1) {
                        result.add(dir);
                    }
                }
            }
        }
    }

    //endregion

    //region Searching Android icon

    private static final Pattern ICON_PATTERN = Pattern.compile(
            "<application[\\s\\S]+?android:icon=\"@(mipmap|drawable)/(\\w+)\"[\\s\\S]+?>");

    private static final String MANIFEST = "AndroidManifest.xml";

    public static File findProperIcon(final File dir, final Context ctx) {
        ArrayList<File> foundIcons = new ArrayList<>();
        findIconFromManifest(dir, dir, foundIcons);
        if (foundIcons.isEmpty())
            return null;

        String density;
        int wantedDensity = getDensityIndex(ctx.getResources().getDisplayMetrics().densityDpi);
        // Check whether we're looking on drawables or mipmaps (usual is mipmap)
        // If we're using mipmaps, we want the corresponding drawable size (2 less) because:
        //   mipmap-mdpi has the same size as drawable-xdpi (2 more)
        //   mipmap-xhdpi has the same size as drawable-xxxhdpi (2 more)
        // All files should either be mipmaps or drawables so just check the first
        if (!foundIcons.get(0).getAbsolutePath().contains("drawable-")) {
            // Using mipmaps, decrease by 2
            wantedDensity -= 2;
            if (wantedDensity < 0)
                wantedDensity = 0;
        }

        int i = wantedDensity;
        while (i <= MAX_DENSITY_INDEX) {
            // Try to find this density on the files, if it's not found, increase
            density = getDensitySuffix(i);
            for (File f : foundIcons) {
                if (f.getAbsolutePath().contains(density)) {
                    return f;
                }
            }
            i++;
        }
        // Not found? Try going backwards (-1, we already checked wantedDensity)
        i = wantedDensity - 1;
        while (i >= 0) {
            density = getDensitySuffix(i);
            for (File f : foundIcons) {
                if (f.getAbsolutePath().contains(density)) {
                    return f;
                }
            }
            i--;
        }
        // Just return a random one, this shouldn't happen
        if (i >= 0 && i < foundIcons.size()) {
            return foundIcons.get(i);
        } else {
            return null;
        }
    }

    private final static int MAX_DENSITY_INDEX = 6;

    private static int getDensityIndex(final int density) {
        switch (density) {
            case DisplayMetrics.DENSITY_LOW:
                return 0;
            case DisplayMetrics.DENSITY_MEDIUM:
                return 1;
            case DisplayMetrics.DENSITY_TV:
                return 2;
            case DisplayMetrics.DENSITY_HIGH:
                return 3;
            case DisplayMetrics.DENSITY_XHIGH:
                return 4;
            case DisplayMetrics.DENSITY_XXHIGH:
                return 5;
            default:
            case DisplayMetrics.DENSITY_XXXHIGH:
                return 6;
        }
    }

    private static String getDensitySuffix(final int index) {
        switch (index) {
            case 0:
                return "-ldpi";
            case 1:
                return "-mdpi";
            case 2:
                return "-tvdpi";
            case 3:
                return "-hdpi";
            case 4:
                return "-xhdpi";
            case 5:
                return "-xxhdpi";
            default:
            case 6:
                return "-xxxhdpi";
        }
    }

    // Finds the android:icon="…" on a AndroidManifest.xml
    // and then returns the search on that path (pointing to the icon files)
    public static void findIconFromManifest(final File parent,
                                            final File dir, final ArrayList<File> foundIcons) {
        if (!dir.getName().startsWith(".")) {
            if (dir.isDirectory()) {
                for (File child : dir.listFiles()) {
                    findIconFromManifest(parent, child, foundIcons);
                    if (!foundIcons.isEmpty()) {
                        // Exit as soon as we find the icons
                        return;
                    }
                }
            } else {
                // dir is a file really
                if (dir.getName().equals(MANIFEST)) {
                    // Look for the pattern
                    try {
                        Scanner s = new Scanner(dir);
                        String found = s.findWithinHorizon(ICON_PATTERN, 0);
                        s.close();

                        if (found != null) {
                            Matcher m = ICON_PATTERN.matcher(found);
                            if (m.find()) {
                                String type = m.group(1);
                                String name = m.group(2);
                                findIcons(parent, type, name, false, foundIcons);
                            }
                        }
                    } catch (FileNotFoundException ignored) {
                    }
                }
            }
        }
    }

    // resourceDir might be 'mipmap(-density)' or 'drawable(-density)'
    private static void findIcons(final File dir, final String resourceDir,
                                  final String name, boolean inResDir,
                                  final ArrayList<File> foundIcons) {
        if (!dir.getName().startsWith(".")) {
            if (dir.isDirectory()) {
                inResDir = dir.getName().startsWith(resourceDir);
                for (File child : dir.listFiles()) {
                    findIcons(child, resourceDir, name, inResDir, foundIcons);
                }
            } else if (inResDir) {
                // We don't care about the files unless we're already on the resources directory
                if (dir.getName().startsWith(name)) {
                    foundIcons.add(dir);
                }
            }
        }
    }

    //endregion

    //region Searching on the application's README

    @NonNull
    public static String mayUseTranslationServices(final File dir) {
        if (!dir.getName().startsWith(".")) {
            if (dir.isDirectory()) {
                String found;
                for (File child : dir.listFiles()) {
                    found = mayUseTranslationServices(child);
                    if (!found.isEmpty())
                        return found;
                }
            } else {
                // dir is a file really
                if (PATTERN_README.matcher(dir.getName()).find()) {
                    int i = Utils.fileContains(dir, STR_TRANSLATION_SERVICES);
                    if (i != -1)
                        return STR_TRANSLATION_SERVICES[i];
                }
            }
        }
        return "";
    }

    //endregion
}

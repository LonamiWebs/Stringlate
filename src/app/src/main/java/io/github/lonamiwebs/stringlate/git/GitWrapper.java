package io.github.lonamiwebs.stringlate.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class GitWrapper {

    private static final String MASTER_BRANCH = "master";
    private static final String REMOTE_NAME = "origin";

    private static final String BASE_GITHUB_URL = "https://github.com/%s/%s";

    // Only the '.xml' extension is case insensitive
    // No regex is required for '<string', which includes '<string-array'
    private static final Pattern PATTERN_XML = Pattern.compile("\\.xml$", Pattern.CASE_INSENSITIVE);
    private static final String STR_STRING = "<string";

    public static String buildGitHubUrl(String owner, String repository) {
        return String.format(BASE_GITHUB_URL, owner, repository);
    }

    // Attempts to convert a normal url to a git valid one
    public static String getGitUri(String url) {
        url = url.trim();

        // TODO Convert SSH to HTTP?
        if (url.startsWith("git://")) {
            return url;
        }
        else if (url.contains("github") || url.contains("gitlab")) {
            // Make sure we use the https version or things will go wrong (301 - moved permanently)
            if (url.startsWith("http:"))
                url = url.replace("http:", "https:");

            if (!url.endsWith(".git")) {
                // GitHub actually works with 'git://' prefix or without the '.git' suffix.
                // GitLab also works with or without '.git' suffix.
                // However since the UI interfaces append '.git', we do so too.
                return url+".git";
            }
        }

        // Hope that the url is valid
        return url;
    }

    public static boolean cloneRepo(String uri, File cloneTo) {
        Git result = null;

        try {
            result = Git.cloneRepository().setURI(uri).setDirectory(cloneTo)
                    .setBranch(MASTER_BRANCH).setBare(false).setRemote(REMOTE_NAME)
                    .setNoCheckout(false).setCloneAllBranches(false).setCloneSubmodules(false).call();
            return true;
        } catch (InvalidRemoteException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (result != null) {
                result.close();
            }
        }
        return false;
    }

    public static boolean deleteRepo(File dir) {
        boolean ok = true;
        if (dir.isDirectory()) {
            for (File child : dir.listFiles())
                ok &= deleteRepo(child);
        }
        ok &= dir.delete();
        return ok;
    }

    public static ArrayList<File> searchAndroidResources(File dir) {
        ArrayList<File> result = new ArrayList<>();
        searchAndroidResources(dir, result);
        return result;
    }

    public static void searchAndroidResources(File dir, ArrayList<File> result) {
        if (!dir.getName().startsWith(".")) {
            if (dir.isDirectory()) {
                for (File child : dir.listFiles())
                    searchAndroidResources(child, result);
            } else {
                // dir is a file really
                if (PATTERN_XML.matcher(dir.getName()).find()) {
                    if (fileContains(dir, STR_STRING))
                        result.add(dir);
                }
            }
        }
    }

    private static boolean fileContains(File file, String needle) {
        try {
            FileInputStream in = new FileInputStream(file);

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                if (line.contains(needle))
                    return true;
            }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
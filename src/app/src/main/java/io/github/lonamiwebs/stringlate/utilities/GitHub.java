package io.github.lonamiwebs.stringlate.utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

// Static GitHub API interface that uses AsyncTasks
public class GitHub {
    //region Members

    private static final String API_URL = "https://api.github.com/";

    //endregion

    //region Private methods

    private static String gGetUrl(final String call, final Object... args) {
        if (args.length > 0)
            return API_URL+String.format(call, args);
        else
            return API_URL+call;
    }

    //endregion

    //region Public methods

    // Determines whether a call to GitHub can be made or not
    // This is equivalent to checking if the user is connected to the internet

    public static boolean gCanCall() {
        // See http://stackoverflow.com/a/27312494/4759433
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            return ipProcess.waitFor() == 0;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Determines whether the given combination of owner/repo is OK or not
    public static boolean gCheckOwnerRepoOK(String owner, String repo) {
        String result = WebUtils.performCall(
                gGetUrl("repos/%s/%s", owner, repo), WebUtils.GET);

        return !result.isEmpty();
    }

    // Looks for 'query' in 'owner/repo' repository's files and
    // returns a JSON with the files for which 'filename' also matches
    public static JSONObject gFindFiles(String owner, String repo, String query, String filename) {
        try {
            return new JSONObject(WebUtils.performCall(
                    gGetUrl("search/code?q=%s+repo:%s/%s+filename:%s",
                            query, owner, repo, filename), WebUtils.GET));

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject gCreateGist(String description, boolean isPublic,
                                   String filename, String content, String token) {
        try {
            JSONObject params = new JSONObject();

            if (description != null && !description.isEmpty())
                params.put("description", description);

            params.put("public", isPublic);

            JSONObject fileObject = new JSONObject();
            fileObject.put("content", content);

            JSONObject filesObject = new JSONObject();
            filesObject.put(filename, fileObject);

            params.put("files", filesObject);

            if (token == null || token.isEmpty())
                return new JSONObject(WebUtils.performCall(gGetUrl("gists"), params));
            else
                return new JSONObject(WebUtils.performCall(gGetUrl("gists?access_token="+token), params));
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject gCreateIssue(RepoHandler repo,
                                    String title, String description, String token) {
        try {
            JSONObject params = new JSONObject();

            params.put("title", title);
            params.put("body", description);

            return new JSONObject(WebUtils.performCall(gGetUrl("repos/%s/issues?access_token=%s",
                    repo.toString(), token), params));
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    //endregion
}

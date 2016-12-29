package io.github.lonamiwebs.stringlate.utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.github.lonamiwebs.stringlate.interfaces.Callback;
import io.github.lonamiwebs.stringlate.tasks.DownloadJSONTask;

// Static GitHub API interface that uses AsyncTasks
public class GitHub {

    //region Members

    private static final String API_URL = "https://api.github.com/";

    //endregion

    //region Private methods

    // Calls the given function with GET method and invokes callback() as result
    private static void gCall(final String call,
                              final Callback<Object> callback) {
        new DownloadJSONTask() {
            @Override
            protected void onPostExecute(Object jsonObject) {
                callback.onCallback(jsonObject);
                super.onPostExecute(jsonObject);
            }
        }.execute(API_URL+call);
    }

    // Calls the given function with POST method and the given
    // parameters and invokes callback() as result
    private static void gCall(final String call, String data,
                              final Callback<Object> callback) {
        new DownloadJSONTask("POST", data) {
            @Override
            protected void onPostExecute(Object jsonObject) {
                callback.onCallback(jsonObject);
                super.onPostExecute(jsonObject);
            }
        }.execute(API_URL+call);
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
    public static void gCheckOwnerRepoOK(String owner, String repo,
                                  final Callback<Boolean> callback) {
        gCall(String.format("repos/%s/%s", owner, repo), new Callback<Object>() {
            @Override
            public void onCallback(Object object) {
                callback.onCallback(object != null);
            }
        });
    }

    // Looks for 'query' in 'owner/repo' repository's files and
    // returns a JSON with the files for which 'filename' also matches
    public static void gFindFiles(String owner, String repo,
                                  String query, String filename,
                                  final Callback<Object> callback) {
        gCall(String.format("search/code?q=%s+repo:%s/%s+filename:%s",
                query, owner, repo, filename), callback);
    }

    public static void gCreateGist(String description, boolean isPublic,
                                   String filename, String content, String token,
                                   final Callback<Object> callback) {
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

            if (token == null || token.isEmpty()) {
                gCall("gists", params.toString(), callback);
            } else {
                gCall("gists?access_token="+token, params.toString(), callback);
            }
        }
        catch (JSONException e) {
            // Won't happen
            e.printStackTrace();
        }
    }

    public static void gCreateIssue(RepoHandler repo,
                                    String title, String description, String token,
                                    final Callback<Object> callback) {
        try {
            JSONObject params = new JSONObject();

            params.put("title", title);
            params.put("body", description);

            String calling = String.format("repos/%s/issues?access_token=%s",
                    repo.toString(), token);
            gCall(String.format("repos/%s/issues?access_token=%s",
                    repo.toString(), token), params.toString(), callback);
        }
        catch (JSONException e) {
            // Won't happen
            e.printStackTrace();
        }
    }

    //endregion
}

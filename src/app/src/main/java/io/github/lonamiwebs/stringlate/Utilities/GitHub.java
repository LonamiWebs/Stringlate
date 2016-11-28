package io.github.lonamiwebs.stringlate.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import io.github.lonamiwebs.stringlate.Interfaces.Callback;
import io.github.lonamiwebs.stringlate.Tasks.DownloadJSONTask;

// Static GitHub API interface that uses AsyncTasks
public class GitHub {

    //region Members

    static final String API_URL = "https://api.github.com/";

    //endregion

    //region Private methods

    // Calls the given function and invokes callback() as result
    private static void gCall(final String call, final Callback<Object> callback) {
        new DownloadJSONTask() {
            @Override
            protected void onPostExecute(Object jsonObject) {
                callback.onCallback(jsonObject);
                super.onPostExecute(jsonObject);
            }
        }.execute(API_URL+call);
    }

    //endregion

    //region Public methods

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

    // Retrieves the (possibly truncated) tree of owner/repo
    public static void gGetTree(String owner, String repo, String sha, boolean recursive,
                         Callback<Object> callback) {
        gCall(String.format(
                "repos/%s/%s/git/trees/%s?recursive=%s",
                owner, repo, sha, recursive ? "1" : "0"), callback);
    }

    // Retrieves the (possibly truncated) top tree of owner/repo
    public static void gGetTopTree(final String owner, final String repo, final boolean recursive,
                            final Callback<Object> callback) {
        // TODO Handle truncated trees on large repositories
        gCall(String.format("repos/%s/%s/branches/master", owner, repo),
                new Callback<Object>() {
                    @Override
                    public void onCallback(Object jsonObject) {
                        try {
                            JSONObject json;
                            json = (JSONObject) jsonObject;
                            json = json.getJSONObject("commit");
                            json = json.getJSONObject("commit");
                            json = json.getJSONObject("tree");
                            String sha = json.getString("sha");

                            gGetTree(owner, repo, sha, recursive, callback);
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                });
    }

    //endregion
}

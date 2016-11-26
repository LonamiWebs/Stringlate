package io.github.lonamiwebs.stringlate.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;

import io.github.lonamiwebs.stringlate.Interfaces.Callback;
import io.github.lonamiwebs.stringlate.Tasks.DownloadJSONTask;

public class GitHub {
    static final String API_URL = "https://api.github.com/";

    private static void gCall(final String call, final Callback<Object> callback) {
        new DownloadJSONTask() {
            @Override
            protected void onPostExecute(Object jsonObject) {
                callback.onCallback(jsonObject);
                super.onPostExecute(jsonObject);
            }
        }.execute(API_URL+call);
    }

    public static void gGetContents(String owner, String repo,
                             Callback<Object> callback) {
        gGetContents(owner, repo, "", callback);
    }
    public static void gGetContents(String owner, String repo, String path,
                             Callback<Object> callback) {
        gCall(String.format("repos/%s/%s/contents/%s", owner, repo, path), callback);
    }

    public static void gCheckOwnerRepoOK(String owner, String repo,
                                  final Callback<Boolean> callback) {
        gCall(String.format("repos/%s/%s", owner, repo), new Callback<Object>() {
            @Override
            public void onCallback(Object object) {
                callback.onCallback(object != null);
            }
        });
    }

    public static void gGetRawFileStream(String owner, String repo, String path,
                                  Callback<InputStreamReader> callback) {
// todo idk lol
    }

    public static void gGetFileString(String owner, String repo, String path,
                               Callback<Object> callback) {
        gGetContents(owner, repo, path, callback);
        // if file encoding == base64...
    }

    public static void gGetTree(String owner, String repo, String sha, boolean recursive,
                         Callback<Object> callback) {
        gCall(String.format(
                "repos/%s/%s/git/trees/%s?recursive=%s",
                owner, repo, sha, recursive ? "1" : "0"), callback);
    }

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
}

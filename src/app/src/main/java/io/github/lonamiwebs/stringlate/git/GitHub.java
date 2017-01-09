package io.github.lonamiwebs.stringlate.git;

import android.support.annotation.NonNull;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;

import io.github.lonamiwebs.stringlate.utilities.RepoHandler;
import io.github.lonamiwebs.stringlate.utilities.WebUtils;

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

    public static boolean gCannotCall() {
        // See http://stackoverflow.com/a/27312494/4759433
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            return ipProcess.waitFor() != 0;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static JSONObject gCreateGist(String description, boolean isPublic,
                                         HashMap<String, String> fileContents,
                                         @NonNull String token) {
        try {
            JSONObject params = new JSONObject();

            if (description != null && !description.isEmpty())
                params.put("description", description);

            params.put("public", isPublic);

            JSONObject filesObject = new JSONObject();
            for (Map.Entry<String, String> fileContent : fileContents.entrySet()) {
                JSONObject fileObject = new JSONObject();
                fileObject.put("content", fileContent.getValue());

                // Add this file object (with content) using the filename as key
                filesObject.put(fileContent.getKey(), fileObject);
            }
            params.put("files", filesObject);

            if (token.isEmpty())
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
                                          String title, String description, String token)
            throws InvalidObjectException {
        try {
            JSONObject params = new JSONObject();

            params.put("title", title);
            params.put("body", description);

            return new JSONObject(WebUtils.performCall(gGetUrl("repos/%s/issues?access_token=%s",
                    repo.toOwnerRepo(), token), params));
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject gGetUserInfo(String token) {
        try {
            return new JSONObject(WebUtils.performCall(gGetUrl(
                    "user?access_token=%s", token), WebUtils.GET));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONArray gGetCollaborators(String token, RepoHandler repo)
            throws InvalidObjectException {
        try {
            return new JSONArray(WebUtils.performCall(gGetUrl(
                    "repos/%s/collaborators?access_token=%s", repo.toOwnerRepo(), token), WebUtils.GET));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Returns both the authenticated user and whether they have right to push
    public static Pair<String, Boolean> gCanPush(String token, RepoHandler repo) {
        JSONObject user = gGetUserInfo(token);
        if (user == null)
            // TODO Actually, maybe throw an NoPermissionException or something
            return new Pair<>(null, false);

        String username = "";
        try {
            username = user.getString("login");

            JSONArray collaborators = gGetCollaborators(token, repo);
            if (collaborators != null) {
                for (int i = 0; i < collaborators.length(); i++) {
                    JSONObject collaborator = collaborators.getJSONObject(i);
                    if (collaborator.getString("login").equals(username)) {
                        JSONObject permissions = collaborator.getJSONObject("permissions");
                        // TODO Can someone possibly not have 'pull' permissions? Then what?
                        return new Pair<>(username, permissions.getBoolean("push"));
                    }
                }
            }
            // If we're not a collaborator, then we obviously don't have permission
            return new Pair<>(username, false);
        } catch (JSONException | InvalidObjectException e) {
            e.printStackTrace();
            return new Pair<>(username, false);
        }
    }

    public static JSONArray gGetBranches(final RepoHandler repo) {
        try {
            return new JSONArray(WebUtils.performCall(gGetUrl(
                    "repos/%s/branches", repo.toOwnerRepo()), WebUtils.GET));
        } catch (JSONException | InvalidObjectException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONArray gGetCommits(final String token, final RepoHandler repo)
            throws InvalidObjectException {
        try {
            return new JSONArray(WebUtils.performCall(gGetUrl(
                    "repos/%s/commits?access_token=%s", repo.toOwnerRepo(), token), WebUtils.GET));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject gForkRepository(final String token, final RepoHandler repo)
            throws InvalidObjectException {
        try {
            JSONObject result = new JSONObject(WebUtils.performCall(gGetUrl(
                    "repos/%s/forks?access_token=%s", repo.toOwnerRepo(), token), WebUtils.POST));

            // "Forking a Repository happens asynchronously."
            // One way to know when forking is done is to fetch the list of commits for the fork.
            // (http://stackoverflow.com/a/33667417/4759433)
            int sleep = 1000;
            do {
                try {
                    Thread.sleep(sleep);
                    sleep *= 2; // Sleep longer if the repo isn't yet forked not to call so often
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            } while (gGetCommits(token, repo) == null);

            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // head = Fork's branch name. If this PR is cross-repository, prefix username:branch.
    // base = Branch's name where the changes will be pulled into. This should exist already.
    public static JSONObject gCreatePullRequest(final String token, final RepoHandler originalRepo,
                                                final String title, final String head,
                                                final String base, final String body)
            throws InvalidObjectException {
        try {
            JSONObject params = new JSONObject();
            params.put("title", title);
            params.put("head", head);
            params.put("base", base);
            if (body != null && !body.isEmpty())
                params.put("body", body);

            return new JSONObject(WebUtils.performCall(gGetUrl(
                    "repos/%s/pulls?access_token=%s", originalRepo.toOwnerRepo(), token), params));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Really big thanks to http://www.levibotelho.com/development/commit-a-file-with-the-github-api
    public static JSONObject gCreateCommitFile(final String token, final RepoHandler repo,
                                               final String branch,
                                               final HashMap<String, String> pathContents,
                                               final String commitMessage)
            throws JSONException, InvalidObjectException {
        final String tokenQuery = "?access_token="+token;
        final String ownerRepo = repo.toOwnerRepo();

        // Step 1. Get a reference to HEAD (GET /repos/:owner/:repo/git/refs/:ref)
        // https://developer.github.com/v3/git/refs/#get-a-reference
        JSONObject head = new JSONObject(WebUtils.performCall(
                gGetUrl("repos/%s/git/refs/heads/%s%s", ownerRepo, branch, tokenQuery), WebUtils.GET));

        // Step 2. Grab the commit that HEAD points to (GET /repos/:owner/:repo/git/commits/:sha)
        // https://developer.github.com/v3/git/commits/#get-a-commit
        String headCommitUrl = head.getJSONObject("object").getString("url");
        // Equivalent to getting object.sha and then formatting it

        JSONObject commit = new JSONObject(WebUtils.performCall(headCommitUrl+tokenQuery, WebUtils.GET));

        // Step 3. Post your new file to the server (POST /repos/:owner/:repo/git/blobs)
        // https://developer.github.com/v3/git/blobs/#create-a-blob
        final HashMap<String, JSONObject> pathBlobs = new HashMap<>();

        for (Map.Entry<String, String> pathContent : pathContents.entrySet()) {
            JSONObject newBlob = new JSONObject();
            newBlob.put("content", pathContent.getValue());
            newBlob.put("encoding", "utf-8");

            JSONObject blob = new JSONObject(WebUtils.performCall(
                    gGetUrl("repos/%s/git/blobs%s", ownerRepo, tokenQuery), newBlob));

            pathBlobs.put(pathContent.getKey(), blob);
        }

        // Step 4. Get a hold of the tree that the commit points to (GET /repos/:owner/:repo/git/trees/:sha)
        // https://developer.github.com/v3/git/trees/#get-a-tree
        String treeUrl = commit.getJSONObject("tree").getString("url");
        // Equivalent to getting tree.sha and then formatting it

        JSONObject baseTree = new JSONObject(WebUtils.performCall(treeUrl+tokenQuery, WebUtils.GET));

        // Step 5. Create a tree containing your new file
        //      5a. The easy way (POST /repos/:owner/:repo/git/trees)
        // https://developer.github.com/v3/git/trees/#create-a-tree
        JSONObject newTree = new JSONObject();
        newTree.put("base_tree", baseTree.get("sha"));
        {
            JSONArray blobFileArray = new JSONArray();
            for (Map.Entry<String, JSONObject> pathBlob : pathBlobs.entrySet()) {
                JSONObject blobFileTree = new JSONObject();
                blobFileTree.put("path", pathBlob.getKey());
                blobFileTree.put("mode", "100644"); // 100644 (blob), 100755 (executable), 040000 (subdirectory/tree), 160000 (submodule/commit), or 120000 (blob specifying path of symlink)
                blobFileTree.put("type", "blob"); // "blob", "tree", or "commit"
                blobFileTree.put("sha", pathBlob.getValue().getString("sha"));

                blobFileArray.put(blobFileTree);
            }

            // Finally put the array with our files
            newTree.put("tree", blobFileArray);
        }

        JSONObject createdTree = new JSONObject(WebUtils.performCall(
                gGetUrl("repos/%s/git/trees%s", ownerRepo, tokenQuery), newTree));

        // Step 6. Create a new commit (POST /repos/:owner/:repo/git/commits)
        // https://developer.github.com/v3/git/commits/#create-a-commit
        JSONObject newCommit = new JSONObject();
        newCommit.put("message", commitMessage);
        // [...] put the SHA of the commit that you retrieved in step #2 in the parents array
        {
            JSONArray parents = new JSONArray();
            parents.put(0, commit.getString("sha"));
            newCommit.put("parents", parents);
        }
        // and the SHA of your newly-created tree from step #5 in the tree field.
        newCommit.put("tree", createdTree.getString("sha"));

        JSONObject repliedNewCommit = new JSONObject(WebUtils.performCall(
                gGetUrl("repos/%s/git/commits%s", ownerRepo, tokenQuery), newCommit));

        // Step 7. Update HEAD (PATCH /repos/:owner/:repo/git/refs/:ref)
        // https://developer.github.com/v3/git/refs/#update-a-reference
        JSONObject patch = new JSONObject();
        patch.put("sha", repliedNewCommit.getString("sha"));

        return new JSONObject(WebUtils.performCall(
                gGetUrl("repos/%s/git/refs/heads/%s%s", ownerRepo, branch, tokenQuery),
                WebUtils.PATCH, patch));
    }

    //endregion
}

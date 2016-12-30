package io.github.lonamiwebs.stringlate.utilities;

import android.util.Pair;

import org.json.JSONArray;
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

    public static JSONObject gGetUserInfo(String token) {
        try {
            return new JSONObject(WebUtils.performCall(gGetUrl(
                    "user?access_token=%s", token), WebUtils.GET));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONArray gGetCollaborators(String token, RepoHandler repo) {
        try {
            return new JSONArray(WebUtils.performCall(gGetUrl(
                    "repos/%s/collaborators?access_token=%s", repo.toString(), token), WebUtils.GET));
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
        } catch (JSONException e) {
            e.printStackTrace();
            return new Pair<>(username, false);
        }
    }

    public static JSONArray gGetBranches(final RepoHandler repo) {
        try {
            return new JSONArray(WebUtils.performCall(gGetUrl(
                    "repos/%s/branches", repo.toString()), WebUtils.GET));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject gForkRepository(final String token, final RepoHandler repo) {
        try {
            return new JSONObject(WebUtils.performCall(gGetUrl(
                    "repos/%s/forks?access_token=%s", repo.toString(), token), WebUtils.POST));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // head = Fork's branch name. If this PR is cross-repository, prefix username:branch.
    // base = Branch's name where the changes will be pulled into. This should exist already.
    public static JSONObject gCreatePullRequest(final String token, final RepoHandler originalRepo,
                                                final String title, final String head,
                                                final String base, final String body) {
        try {
            JSONObject params = new JSONObject();
            params.put("title", title);
            params.put("head", head);
            params.put("base", base);
            if (body != null && !body.isEmpty())
                params.put("body", body);

            return new JSONObject(WebUtils.performCall(gGetUrl(
                    "repos/%s/pulls?access_token=%s", originalRepo.toString(), token), params));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Really big thanks to http://www.levibotelho.com/development/commit-a-file-with-the-github-api
    public static JSONObject gCreateCommitFile(final String token, final RepoHandler repo,
                                               final String branch, final String content,
                                               final String filename, final String commitMessage)
            throws JSONException {
        final String tokenQuery = "?access_token="+token;

        // Step 1. Get a reference to HEAD (GET /repos/:owner/:repo/git/refs/:ref)
        // https://developer.github.com/v3/git/refs/#get-a-reference
        JSONObject head = new JSONObject(WebUtils.performCall(
                gGetUrl("repos/%s/git/refs/heads/%s%s", repo.toString(), branch, tokenQuery), WebUtils.GET));

        // Step 2. Grab the commit that HEAD points to (GET /repos/:owner/:repo/git/commits/:sha)
        // https://developer.github.com/v3/git/commits/#get-a-commit
        String headCommitUrl = head.getJSONObject("object").getString("url");
        // Equivalent to getting object.sha and then formatting it

        JSONObject commit = new JSONObject(WebUtils.performCall(headCommitUrl+tokenQuery, WebUtils.GET));

        // Step 3. Post your new file to the server (POST /repos/:owner/:repo/git/blobs)
        // https://developer.github.com/v3/git/blobs/#create-a-blob
        JSONObject newBlob = new JSONObject();
        newBlob.put("content", content);
        newBlob.put("encoding", "utf-8");

        JSONObject blob = new JSONObject(WebUtils.performCall(
                gGetUrl("repos/%s/git/blobs%s", repo.toString(), tokenQuery), newBlob));

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
            JSONObject blobFileTree = new JSONObject();
            blobFileTree.put("path", filename);
            blobFileTree.put("mode", "100644"); // 100644 (blob), 100755 (executable), 040000 (subdirectory/tree), 160000 (submodule/commit), or 120000 (blob specifying path of symlink)
            blobFileTree.put("type", "blob"); // "blob", "tree", or "commit"
            blobFileTree.put("sha", blob.getString("sha"));
            // More files could be added to the array
            JSONArray blobFileArray = new JSONArray();
            blobFileArray.put(0, blobFileTree);

            // Finally put the array with our files
            newTree.put("tree", blobFileArray);
        }

        JSONObject createdTree = new JSONObject(WebUtils.performCall(
                gGetUrl("repos/%s/git/trees%s", repo.toString(), tokenQuery), newTree));

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
                gGetUrl("repos/%s/git/commits%s", repo.toString(), tokenQuery), newCommit));

        // Step 7. Update HEAD (PATCH /repos/:owner/:repo/git/refs/:ref)
        // https://developer.github.com/v3/git/refs/#update-a-reference
        JSONObject patch = new JSONObject();
        patch.put("sha", repliedNewCommit.getString("sha"));

        return new JSONObject(WebUtils.performCall(
                gGetUrl("repos/%s/git/refs/heads/%s%s", repo.toString(), branch, tokenQuery),
                WebUtils.PATCH, patch));
    }

    //endregion
}

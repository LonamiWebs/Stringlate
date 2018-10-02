package io.github.lonamiwebs.stringlate.classes.git;

import net.gsantner.opoc.util.NetworkUtils;

import org.eclipse.jgit.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InvalidObjectException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.interfaces.SlAppSettings;

// Static GitHub API interface that uses AsyncTasks
@SuppressWarnings({"WeakerAccess", "unused"})
public class GitHub {
    // GitHub OAuth (https://developer.github.com/v3/oauth/#scopes)
    // Scopes joined by '%20', although result scopes are joined by ','
    public final static String GITHUB_API_URL = "https://api.github.com/";
    public final static String GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize?scope=%s&client_id=%s";
    public final static String GITHUB_COMPLETE_AUTH_URL = "https://github.com/login/oauth/access_token";
    public final static String[] GITHUB_WANTED_SCOPES = {"public_repo", "gist"};
    private static final String GITHUB_REPO_URL_TEMPLATE = "https://github.com/%s/%s";

    //region Private methods

    private static String getUrl(final String call, final Object... args) {
        if (args.length > 0)
            return GITHUB_API_URL + String.format(call, args);
        else
            return GITHUB_API_URL + call;
    }

    //endregion

    //region Public methods

    public static JSONObject createGist(String description, boolean isPublic,
                                        HashMap<String, String> fileContents,
                                        String token) {
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
                return new JSONObject(NetworkUtils.performCall(getUrl("gists"), params));
            else
                return new JSONObject(NetworkUtils.performCall(getUrl("gists?access_token=" + token), params));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject createIssue(RepoHandler repo,
                                         String title, String description, String token)
            throws InvalidObjectException {
        try {
            JSONObject params = new JSONObject();

            params.put("title", title);
            params.put("body", description);

            return new JSONObject(NetworkUtils.performCall(getUrl("repos/%s/issues?access_token=%s",
                    repo.toOwnerRepo(), token), params));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject commentIssue(RepoHandler repo,
                                          int issueNumber, String body, String token)
            throws InvalidObjectException {
        try {
            JSONObject params = new JSONObject();
            params.put("body", body);
            return new JSONObject(NetworkUtils.performCall(getUrl(
                    "repos/%s/issues/%d/comments?access_token=%s",
                    repo.toOwnerRepo(), issueNumber, token), params));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject getUserInfo(String token) {
        try {
            return new JSONObject(NetworkUtils.performCall(getUrl(
                    "user?access_token=%s", token), NetworkUtils.GET));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONArray getCollaborators(String token, RepoHandler repo)
            throws InvalidObjectException {
        try {
            return new JSONArray(NetworkUtils.performCall(getUrl(
                    "repos/%s/collaborators?access_token=%s", repo.toOwnerRepo(), token), NetworkUtils.GET));
        } catch (JSONException e) {
            // We might not have permission so the response isn't an array, rather an object:
            // {
            //   "message": "Must have push access to view repository collaborators.",
            //   "documentation_url": "https://developer.github.com/v3/repos/collaborators/#list-collaborators"
            // }
            e.printStackTrace();
            return null;
        }
    }

    // Returns both the authenticated user and whether they have right to push
    public static AbstractMap.SimpleImmutableEntry<String, Boolean> canPush(String token, RepoHandler repo) {
        JSONObject user = getUserInfo(token);
        if (user == null)
            // TODO Actually, maybe throw an NoPermissionException or something
            return new AbstractMap.SimpleImmutableEntry<>(null, false);

        String username = "";
        try {
            username = user.getString("login");

            JSONArray collaborators = getCollaborators(token, repo);
            if (collaborators != null) {
                for (int i = 0; i < collaborators.length(); i++) {
                    JSONObject collaborator = collaborators.getJSONObject(i);
                    if (collaborator.getString("login").equals(username)) {
                        JSONObject permissions = collaborator.getJSONObject("permissions");
                        // TODO Can someone possibly not have 'pull' permissions? Then what?
                        return new AbstractMap.SimpleImmutableEntry<>(username, permissions.getBoolean("push"));
                    }
                }
            }
            // If we're not a collaborator, then we obviously don't have permission
            return new AbstractMap.SimpleImmutableEntry<>(username, false);
        } catch (JSONException | InvalidObjectException e) {
            e.printStackTrace();
            return new AbstractMap.SimpleImmutableEntry<>(username, false);
        }
    }

    public static JSONArray getBranches(final RepoHandler repo) {
        try {
            return new JSONArray(NetworkUtils.performCall(getUrl(
                    "repos/%s/branches", repo.toOwnerRepo()), NetworkUtils.GET));
        } catch (JSONException | InvalidObjectException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getDefaultBranch(final RepoHandler repo) {
        try {
            JSONObject result = new JSONObject(NetworkUtils.performCall(getUrl(
                    "repos/%s", repo.toOwnerRepo()), NetworkUtils.GET));
            return result.getString("default_branch");
        } catch (JSONException | InvalidObjectException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONArray getCommits(final String token, final RepoHandler repo)
            throws InvalidObjectException {
        try {
            return new JSONArray(NetworkUtils.performCall(getUrl(
                    "repos/%s/commits?access_token=%s", repo.toOwnerRepo(), token), NetworkUtils.GET));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject createBranch(final String token, final RepoHandler repo, final String branchName)
            throws InvalidObjectException {
        try {
            JSONArray head = new JSONArray(NetworkUtils.performCall(getUrl(
                    "repos/%s/git/refs/heads?access_token=%s", repo.toOwnerRepo(), token), NetworkUtils.GET));

            final String sha = head.getJSONObject(0).getJSONObject("object").getString("sha");
            JSONObject params = new JSONObject();
            params.put("ref", "refs/heads/" + branchName);
            params.put("sha", sha);
            return new JSONObject(NetworkUtils.performCall(getUrl(
                    "repos/%s/git/refs?access_token=%s", repo.toOwnerRepo(), token), params));

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject forkRepository(final String token, final RepoHandler repo)
            throws InvalidObjectException {
        try {
            JSONObject result = new JSONObject(NetworkUtils.performCall(getUrl(
                    "repos/%s/forks?access_token=%s", repo.toOwnerRepo(), token), NetworkUtils.POST));

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
            } while (getCommits(token, repo) == null);

            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // head = Fork's branch name. If this PR is cross-repository, prefix username:branch.
    // base = Branch's name where the changes will be pulled into. This should exist already.
    public static JSONObject createPullRequest(final String token, final RepoHandler originalRepo,
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

            return new JSONObject(NetworkUtils.performCall(getUrl(
                    "repos/%s/pulls?access_token=%s", originalRepo.toOwnerRepo(), token), params));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Really big thanks to http://www.levibotelho.com/development/commit-a-file-with-the-github-api
    public static JSONObject createCommitFile(final String token, final RepoHandler repo,
                                              final String branch,
                                              final HashMap<String, String> pathContents,
                                              final String commitMessage)
            throws JSONException, InvalidObjectException {
        final String tokenQuery = "?access_token=" + token;
        final String ownerRepo = repo.toOwnerRepo();

        // Step 1. Get a reference to HEAD (GET /repos/:owner/:repo/git/refs/:ref)
        // https://developer.github.com/v3/git/refs/#get-a-reference
        JSONObject head = new JSONObject(NetworkUtils.performCall(
                getUrl("repos/%s/git/refs/heads/%s%s", ownerRepo, branch, tokenQuery), NetworkUtils.GET));

        // Step 2. Grab the commit that HEAD points to (GET /repos/:owner/:repo/git/commits/:sha)
        // https://developer.github.com/v3/git/commits/#get-a-commit
        String headCommitUrl = head.getJSONObject("object").getString("url");
        // Equivalent to getting object.sha and then formatting it

        JSONObject commit = new JSONObject(NetworkUtils.performCall(headCommitUrl + tokenQuery, NetworkUtils.GET));

        // Step 3. Post your new file to the server (POST /repos/:owner/:repo/git/blobs)
        // https://developer.github.com/v3/git/blobs/#create-a-blob
        final HashMap<String, JSONObject> pathBlobs = new HashMap<>();

        for (Map.Entry<String, String> pathContent : pathContents.entrySet()) {
            JSONObject newBlob = new JSONObject();
            newBlob.put("content", pathContent.getValue());
            newBlob.put("encoding", "utf-8");

            JSONObject blob = new JSONObject(NetworkUtils.performCall(
                    getUrl("repos/%s/git/blobs%s", ownerRepo, tokenQuery), newBlob));

            pathBlobs.put(pathContent.getKey(), blob);
        }

        // Step 4. Get a hold of the tree that the commit points to (GET /repos/:owner/:repo/git/trees/:sha)
        // https://developer.github.com/v3/git/trees/#get-a-tree
        String treeUrl = commit.getJSONObject("tree").getString("url");
        // Equivalent to getting tree.sha and then formatting it

        JSONObject baseTree = new JSONObject(NetworkUtils.performCall(treeUrl + tokenQuery, NetworkUtils.GET));

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

        JSONObject createdTree = new JSONObject(NetworkUtils.performCall(
                getUrl("repos/%s/git/trees%s", ownerRepo, tokenQuery), newTree));

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

        JSONObject repliedNewCommit = new JSONObject(NetworkUtils.performCall(
                getUrl("repos/%s/git/commits%s", ownerRepo, tokenQuery), newCommit));

        // Step 7. Update HEAD (PATCH /repos/:owner/:repo/git/refs/:ref)
        // https://developer.github.com/v3/git/refs/#update-a-reference
        JSONObject patch = new JSONObject();
        patch.put("sha", repliedNewCommit.getString("sha"));

        return new JSONObject(NetworkUtils.performCall(
                getUrl("repos/%s/git/refs/heads/%s%s", ownerRepo, branch, tokenQuery),
                NetworkUtils.PATCH, patch));
    }

    public static String buildGitHubUrl(String owner, String repository) {
        return String.format(GITHUB_REPO_URL_TEMPLATE, owner, repository);
    }

    public static String getGitHubOwnerRepo(final String url)
            throws InvalidObjectException {
        Matcher m = GitWrapper.OWNER_REPO.matcher(url);
        if (m.matches() && m.group(1).equalsIgnoreCase("github.com")) {
            return String.format("%s/%s", m.group(2), m.group(3));
        }
        throw new InvalidObjectException("Not a GitHub repository.");
    }

    //endregion

    //region Authentication
    public static class Authentication {
        public static class CompleteAuthenticationResult {
            public boolean ok;
            public String message, token, grantedScopes;
        }

        public static String getAuthRequestUrl() {
            return String.format(GITHUB_AUTH_URL,
                    StringUtils.join(Arrays.asList(GITHUB_WANTED_SCOPES), "%20"), SlAppSettings.GITHUB_CLIENT_ID);
        }

        public static CompleteAuthenticationResult completeGitHubAuth(SlAppSettings appSettings, String clientId, String clientSecret, String code) {
            final HashMap<String, String> map = new HashMap<>();
            map.put("client_id", clientId);
            map.put("client_secret", clientSecret);
            map.put("code", code);

            CompleteAuthenticationResult ret = new CompleteAuthenticationResult();
            HashMap<String, String> postResult = NetworkUtils.getDataMap(
                    NetworkUtils.performCall(GITHUB_COMPLETE_AUTH_URL, NetworkUtils.POST, map)
            );
            if (postResult.containsKey("error")) {
                ret.message = postResult.get("error_description");
            } else {
                ret.token = postResult.get("access_token");
                ret.grantedScopes = postResult.get("scope");
                appSettings.setGitHubAccess(ret.token, ret.grantedScopes);
                if (appSettings.hasGitHubAuthorization()) {
                    ret.ok = true;
                }
            }
            return ret;
        }
    }
    //endregion
}

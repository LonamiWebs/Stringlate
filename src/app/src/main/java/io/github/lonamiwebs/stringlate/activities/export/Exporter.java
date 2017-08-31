package io.github.lonamiwebs.stringlate.activities.export;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.git.GitHub;
import io.github.lonamiwebs.stringlate.git.GitWrapper;

class Exporter {
    // Callable exporters return the non-null posted URL as String on success, or throw on failure
    static Callable<String> getGistExporter(
            final String description, final boolean isPublic,
            final HashMap<String, String> fileContents, final String token) {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                JSONObject result = GitHub.gCreateGist(description, isPublic, fileContents, token);
                if (result == null)
                    throw new JSONException("Null JSON");

                return result.getString("html_url");
            }
        };
    }

    static Callable<String> getIssueExporter(
            final RepoHandler repo, final int existingIssueNumber, final String forLocale,
            final String issueTitle, final String issueDesc, final String token) {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                final JSONObject result;
                if (existingIssueNumber == -1) {
                    result = GitHub.gCreateIssue(
                            repo, issueTitle, issueDesc, token
                    );
                } else {
                    result = GitHub.gCommentIssue(
                            repo, existingIssueNumber, issueDesc, token
                    );
                }

                if (result == null)
                    throw new InvalidObjectException("Invalid GitHub repository.");

                String postedUrl = result.getString("html_url");

                if (existingIssueNumber == -1)
                    repo.settings.addCreatedIssue(forLocale, result.optInt("number"));

                return postedUrl;
            }
        };
    }

    static Callable<String> getPullRequestExporter(
            final RepoHandler originalRepo, final Context context, final boolean needFork,
            final String locale, final String branch, final String commitMessage,
            final String username, final String token) {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {

                JSONObject commitResult;
                RepoHandler repo;
                if (needFork) {
                    // Fork the repository
                    //try {
                    //updateProgress(getString(R.string.forking_repo), getString(R.string.forking_repo_long));

                    JSONObject fork = GitHub.gForkRepository(token, originalRepo);
                    if (fork == null) throw new JSONException("Resulting fork is null.");

                    String owner = fork.getJSONObject("owner").getString("login");
                    String repoName = fork.getString("name");
                    repo = new RepoHandler(context, GitWrapper.buildGitHubUrl(owner, repoName));
                    /*
                    } catch (JSONException | InvalidObjectException e) {
                        setFailure(getString(R.string.fork_failed));
                        throw e;
                    }
                    */
                } else {
                    repo = originalRepo;
                }

                // Commit the file
                //try {
                //updateProgress(getString(R.string.creating_commit), getString(R.string.creating_commit_long));

                final HashMap<String, String> remoteContents = new HashMap<>();
                for (Map.Entry<File, String> templateRemote :
                        originalRepo.getTemplateRemotePaths(locale).entrySet()) {
                    // Iterate over the local template files and the remote paths for this locale
                    String content = originalRepo.applyTemplate(templateRemote.getKey(), locale);
                    if (!content.isEmpty()) {
                        remoteContents.put(templateRemote.getValue(), content);
                    }
                }
                commitResult = GitHub.gCreateCommitFile(
                        token, repo, branch, remoteContents, commitMessage
                );
                /*
                } catch (JSONException | InvalidObjectException e) {
                    setFailure(getString(R.string.commit_failed));
                    throw e;
                }
                */

                if (needFork) {
                    // Create pull request
                    //updateProgress(getString(R.string.creating_pr), getString(R.string.creating_pr_long));

                    String title, body;
                    int newLineIndex = commitMessage.indexOf('\n');
                    if (newLineIndex > -1) {
                        title = commitMessage.substring(0, newLineIndex);
                        body = commitMessage.substring(newLineIndex).trim().replace(
                                "Stringlate", "[Stringlate](https://lonamiwebs.github.io/stringlate/)"
                        );
                    } else {
                        title = commitMessage;
                        body = "";
                    }

                    // This may throw another InvalidObjectException
                    commitResult = GitHub.gCreatePullRequest(
                            token, originalRepo, title, username + ":" + branch, branch, body
                    );
                }

                // Now we finally have our commitResult ready
                if (commitResult == null)
                    throw new InvalidObjectException("commitResult cannot be null");

                //try {
                return needFork ? commitResult.getString("html_url") // postedUrl
                        : String.format("https://github.com%s/commit/%s",
                        originalRepo.getPath(), commitResult.getJSONObject("object").getString("sha"));
                /*
                return true;
                } catch (JSONException e) {
                    return false;
                }
                */
            }
        };
    }
}

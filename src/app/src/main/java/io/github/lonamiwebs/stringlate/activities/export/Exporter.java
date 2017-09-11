package io.github.lonamiwebs.stringlate.activities.export;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;
import io.github.lonamiwebs.stringlate.classes.repos.RepoHandlerHelper;
import io.github.lonamiwebs.stringlate.git.GitHub;
import io.github.lonamiwebs.stringlate.classes.git.GitWrapper;
import io.github.lonamiwebs.stringlate.interfaces.Callback;

class Exporter {

    private static SparseArray<CallableExporter> exporters = new SparseArray<>();
    private static Random random = new Random();

    public static abstract class CallableExporter {
        @NonNull
        String mFailureReason = ""; // Meant to be set before a possible exception happens

        abstract String getDescription(Context context);

        abstract String getSuccessDescription(Context context);

        abstract String call(Context context, Callback<String> progress) throws Exception;
    }

    private static int addExporter(final CallableExporter exporter) {
        int id = random.nextInt();
        while (id == 0 || exporters.indexOfKey(id) != -1)
            id = random.nextInt();

        exporters.put(id, exporter);
        return id;
    }

    // Callable exporters return the non-null posted URL as String on success
    // Note that the exporter may be null if no such ID is found.
    // The ID 0 will never exist.
    static CallableExporter getExporter(final int id) {
        final CallableExporter result = exporters.get(id);
        if (result != null)
            exporters.delete(id);
        return result;
    }

    static int createGistExporter(
            final String description, final boolean isPublic,
            final HashMap<String, String> fileContents, final String token) {
        return addExporter(new CallableExporter() {
            @Override
            String getDescription(final Context context) {
                return context.getString(R.string.posting_gist_ellipsis_long);
            }

            @Override
            public String call(final Context context, final Callback<String> progress) throws Exception {
                mFailureReason = context.getString(R.string.post_gist_error);
                JSONObject result = GitHub.gCreateGist(description, isPublic, fileContents, token);
                if (result == null)
                    throw new JSONException("Null JSON");

                return result.getString("html_url");
            }

            @Override
            String getSuccessDescription(final Context context) {
                return context.getString(R.string.post_gist_success);
            }
        });
    }

    static int createIssueExporter(
            final RepoHandler repo, final int existingIssueNumber, final String forLocale,
            final String issueTitle, final String issueDesc, final String token) {
        return addExporter(new CallableExporter() {
            @Override
            String getDescription(final Context context) {
                return context.getString(R.string.creating_issue_long_ellipsis);
            }

            @Override
            public String call(final Context context, final Callback<String> progress) throws Exception {
                final JSONObject result;
                mFailureReason = context.getString(R.string.create_issue_error);
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

            @Override
            String getSuccessDescription(final Context context) {
                return context.getString(R.string.create_issue_success);
            }
        });
    }

    static int createPullRequestExporter(
            final RepoHandler originalRepo, final boolean needFork,
            final String locale, final String commitMessage,
            final String username, final String token) {
        return addExporter(new CallableExporter() {

            @Override
            String getDescription(final Context context) {
                return context.getString(R.string.creating_pr_long);
            }

            @Override
            public String call(final Context context, final Callback<String> progress) throws Exception {
                JSONObject commitResult;
                RepoHandler repo;
                if (needFork) {
                    // Fork the repository
                    mFailureReason = context.getString(R.string.fork_failed);
                    progress.onCallback(context.getString(R.string.forking_repo_long));

                    JSONObject fork = GitHub.gForkRepository(token, originalRepo);
                    if (fork == null) throw new JSONException("Resulting fork is null.");

                    String owner = fork.getJSONObject("owner").getString("login");
                    String repoName = fork.getString("name");
                    repo = RepoHandlerHelper.fromContext(context, GitWrapper.buildGitHubUrl(owner, repoName));
                } else {
                    repo = originalRepo;
                }

                // Create a temporary branch
                // TODO If we have write access, should we create a new branch at all?
                @SuppressLint("DefaultLocale")
                final String branch = String.format(
                        "stringlate-%s-%d", locale, 1000 + new Random().nextInt(8999)
                );
                JSONObject result = GitHub.gCreateBranch(token, repo, branch);
                if (result == null) throw new JSONException("Failed to create a new branch.");

                // Commit the file
                progress.onCallback(context.getString(R.string.creating_commit_long));
                mFailureReason = context.getString(R.string.commit_failed);

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

                if (needFork) {
                    // Create pull request
                    progress.onCallback(context.getString(R.string.creating_pr_long));
                    mFailureReason = context.getString(R.string.something_went_wrong);

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

                return needFork ? commitResult.getString("html_url") // postedUrl
                        : String.format("https://github.com%s/commit/%s",
                        originalRepo.getPath(), commitResult.getJSONObject("object").getString("sha"));
            }

            @Override
            String getSuccessDescription(final Context context) {
                return context.getString(R.string.done);
            }
        });
    }
}

package io.github.lonamiwebs.stringlate.classes;

import java.util.ArrayList;

import io.github.lonamiwebs.stringlate.classes.repos.RepoHandler;

// Static messenger class to pass messages back and forth between activities or async tasks.
// Any activity can add a listener for certain events, e.g. when the progress of syncing a
// repository changes, the count of available repositories changes.
public class Messenger {

    // This interface is meant to be used by the RepoHandler and StringSource to report progress
    public interface OnRepoSyncProgress {
        void onUpdate(int stage, float progress);
    }

    // This interface is meant to be used as the progress of the task running RepoHandler.sync
    public interface OnRepoSync {
        void onUpdate(RepoHandler which, float progress);
        void onFinish(RepoHandler which, boolean okay);
    }

    public interface OnRepoChange {
        void onRepoAdded(RepoHandler which);
        void onRepoRemoved(RepoHandler which);
    }

    public final static ArrayList<OnRepoSync> onRepoSync = new ArrayList<>();
    public final static ArrayList<OnRepoChange> onRepoChange = new ArrayList<>();

    public static void notifyRepoSync(final RepoHandler which, final float progress) {
        for (OnRepoSync x : onRepoSync)
            x.onUpdate(which, progress);
    }

    public static void notifyRepoSyncFinished(final RepoHandler which, final boolean okay) {
        for (OnRepoSync x : onRepoSync)
            x.onFinish(which, okay);
    }

    public static void notifyRepoAdded(final RepoHandler which) {
        for (OnRepoChange x : onRepoChange)
            x.onRepoAdded(which);
    }

    public static void notifyRepoRemoved(final RepoHandler which) {
        for (OnRepoChange x : onRepoChange)
            x.onRepoRemoved(which);
    }
}

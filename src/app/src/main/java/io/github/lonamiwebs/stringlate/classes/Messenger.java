package io.github.lonamiwebs.stringlate.classes;

import java.util.ArrayList;

// Static messenger class to pass messages back and forth between activities or async tasks.
// Any activity can add a listener for certain events, e.g. when the progress of syncing a
// repository changes, the count of available repositories changes.
public class Messenger {

    public interface OnRepoChange {
        void onCountChanged();
    }

    public final static ArrayList<OnRepoChange> onRepoChange = new ArrayList<>();

    public static void notifyRepoCountChange() {
        for (OnRepoChange x : onRepoChange)
            x.onCountChanged();
    }
}

package io.github.lonamiwebs.stringlate.Interfaces;

// Interface used to notify a progress change,
// preferably to a progress dialog, and to also dismiss it onProgressFinished
public interface ProgressUpdateCallback {
    void onProgressUpdate(String title, String description);
    void onProgressFinished(String description, boolean status);
}

package io.github.lonamiwebs.stringlate.interfaces;

// Interface used to notify a progress change,
// preferably to a progress dialog, and to also dismiss it onProgressFinished
public interface ProgressUpdateCallback {
    void onProgressUpdate(String title, String description);
    void showMessage(String message);
}

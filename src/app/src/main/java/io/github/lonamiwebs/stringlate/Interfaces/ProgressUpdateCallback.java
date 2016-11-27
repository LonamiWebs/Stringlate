package io.github.lonamiwebs.stringlate.Interfaces;

public interface ProgressUpdateCallback {
    void onProgressUpdate(String title, String description);
    void onProgressFinished(String description, boolean status);
}

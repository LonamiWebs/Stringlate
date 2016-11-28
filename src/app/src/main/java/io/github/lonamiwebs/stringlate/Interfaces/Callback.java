package io.github.lonamiwebs.stringlate.Interfaces;

// Simple callback interface to return a single result from AsyncTasks
public interface Callback<T> {
    void onCallback(T t);
}

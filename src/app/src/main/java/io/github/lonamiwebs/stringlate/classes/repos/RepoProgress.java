package io.github.lonamiwebs.stringlate.classes.repos;

import org.json.JSONException;
import org.json.JSONObject;

public class RepoProgress {
    public int stringsCount;
    public int translatedCount;
    public int currentChars;
    public int totalChars;

    public RepoProgress() { }

    private RepoProgress(int stringsCount, int translatedCount, int currentChars, int totalChars) {
        this.stringsCount = stringsCount;
        this.translatedCount = translatedCount;
        this.currentChars = currentChars;
        this.totalChars = totalChars;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("stringsCount", stringsCount);
        json.put("translatedCount", translatedCount);
        json.put("currentChars", currentChars);
        json.put("totalChars", totalChars);
        return json;
    }

    static RepoProgress fromJson(JSONObject json) {
        return new RepoProgress(
                json.optInt("stringsCount"), json.optInt("translatedCount"),
                json.optInt("currentChars"), json.optInt("totalChars"));
    }

    public float getPercentage() {
        if (totalChars == 0)
            return 0f;

        return (100.0f * (float)currentChars) / (float)totalChars;
    }
}

package io.github.lonamiwebs.stringlate.Tasks;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;


public class DownloadJSONTask extends AsyncTask<String, Void, Object> {
    @Override
    protected Object doInBackground(String... params) {
        BufferedReader in = null;

        try {
            URL url = new URL(params[0]);
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            return new JSONTokener(in.readLine()).nextValue();
        }
        catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }
}

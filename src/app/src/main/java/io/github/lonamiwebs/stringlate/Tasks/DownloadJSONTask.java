package io.github.lonamiwebs.stringlate.Tasks;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;


// Task used to download a JSON string from a given remote URL
public class DownloadJSONTask extends AsyncTask<String, Void, Object> {

    private String mMethod;
    private String mData;

    public DownloadJSONTask() {
        this(null, null);
    }

    public DownloadJSONTask(String method, String data) {
        mMethod = method;
        mData = data;
    }

    @Override
    protected Object doInBackground(String... params) {
        BufferedReader in = null;
        String line = null;

        try {
            URL url = new URL(params[0]);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            if (mMethod != null)
                conn.setRequestMethod(mMethod);

            if (mData != null) {
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(mData);
                writer.flush();
                writer.close();
                os.close();
            }

            conn.connect();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            line = in.readLine(); // For debugging purposes
            return new JSONTokener(line).nextValue();
        }
        catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e("DownloadJsonError", "Received JSON line: "+line);
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

package io.github.lonamiwebs.stringlate.tasks;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

// Source: http://stackoverflow.com/a/31357311/4759433
// Used to authorize Stringlate with OAuth
public class HttpPostTask extends AsyncTask<String, Void, HashMap<String, String>> {

    private HashMap<String, String> mData;

    public HttpPostTask() {
        this(null);
    }

    public HttpPostTask(HashMap<String, String> data) {
        mData = data;
    }

    @Override
    protected HashMap<String, String> doInBackground(String... params) {
        try {
            URL url = new URL(params[0]);
            return getPostDataMap(performPostCall(url, mData));
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String performPostCall(URL url, HashMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        try {
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(params));
            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append('\n');
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) first = false;
            else result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    private static HashMap<String, String> getPostDataMap(String encoded) throws UnsupportedEncodingException {
        HashMap<String, String> result = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        String name = "";

        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            switch (c) {
                case '=':
                    name = URLDecoder.decode(sb.toString(), "UTF-8");
                    sb.setLength(0);
                    break;
                case '&':
                    result.put(name, URLDecoder.decode(sb.toString(), "UTF-8"));
                    sb.setLength(0);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        if (!name.isEmpty())
            result.put(name, URLDecoder.decode(sb.toString(), "UTF-8"));

        return result;
    }
}

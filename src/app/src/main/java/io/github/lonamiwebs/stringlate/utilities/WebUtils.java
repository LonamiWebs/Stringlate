package io.github.lonamiwebs.stringlate.utilities;

import org.json.JSONObject;

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

public class WebUtils {

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PATCH = "PATCH";
    private static final String UTF8 = "UTF-8";

    // No parameters
    public static String performCall(String url, String method) {
        try {
            return performCall(new URL(url), method, "");
        } catch (MalformedURLException e) { e.printStackTrace(); }
        return "";
    }

    // URL encoded parameters
    public static String performCall(String url, String method, HashMap<String, String> params) {
        try {
            return performCall(new URL(url), method, getQuery(params));
        } catch (UnsupportedEncodingException | MalformedURLException e) { e.printStackTrace(); }
        return "";
    }

    // JSON encoded parameters are usually POST
    public static String performCall(String url, JSONObject json) {
        return performCall(url, POST, json);
    }
    public static String performCall(String url, String method, JSONObject json) {
        try {
            return performCall(new URL(url), method, json.toString());
        } catch (MalformedURLException e) { e.printStackTrace(); }
        return "";
    }

    // Source: http://stackoverflow.com/a/31357311/4759433
    private static String performCall(URL url, String method, String data) {
        StringBuilder sb = new StringBuilder();
        try {
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod(method);
            conn.setDoInput(true);

            if (data != null && !data.isEmpty()) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(data);
                writer.flush();
                writer.close();
                os.close();
            }

            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static String getQuery(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) first = false;
            else result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), UTF8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), UTF8));
        }

        return result.toString();
    }

    public static HashMap<String, String> getDataMap(String query) {
        HashMap<String, String> result = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        String name = "";

        try {
            for (int i = 0; i < query.length(); i++) {
                char c = query.charAt(i);
                switch (c) {
                    case '=':
                        name = URLDecoder.decode(sb.toString(), UTF8);
                        sb.setLength(0);
                        break;
                    case '&':
                        result.put(name, URLDecoder.decode(sb.toString(), UTF8));
                        sb.setLength(0);
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            if (!name.isEmpty())
                result.put(name, URLDecoder.decode(sb.toString(), UTF8));
        } catch (UnsupportedEncodingException e) { e.printStackTrace(); }

        return result;
    }
}

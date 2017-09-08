/*
 * ------------------------------------------------------------------------------
 * Lonami Exo <lonamiwebs.github.io> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me
 * a coke in return. Provided as is without any kind of warranty. Do not blame
 * or sue me if something goes wrong. No attribution required.
 *                                                             - Lonami Exo
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package io.github.gsantner.opoc.util;

import org.json.JSONObject;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "SpellCheckingInspection", "deprecation"})
public class HelpersNetwork {

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PATCH = "PATCH";
    private static final String UTF8 = "UTF-8";

    // No parameters, method can be GET, POST, etc.
    public static String performCall(final String url, final String method) {
        try {
            return performCall(new URL(url), method, "");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "";
    }

    // URL encoded parameters
    public static String performCall(final String url, final String method, final HashMap<String, String> params) {
        try {
            return performCall(new URL(url), method, encodeQuery(params));
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            e.printStackTrace();
        }
        return "";
    }

    // Defaults to POST
    public static String performCall(final String url, final JSONObject json) {
        return performCall(url, POST, json);
    }

    public static String performCall(final String url, final String method, final JSONObject json) {
        try {
            return performCall(new URL(url), method, json.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String performCall(final URL url, final String method, final String data) {
        try {
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setDoInput(true);

            if (data != null && !data.isEmpty()) {
                conn.setDoOutput(true);
                final OutputStream output = conn.getOutputStream();
                output.write(data.getBytes(Charset.forName(UTF8)));
                output.flush();
                output.close();
            }

            return HelpersFiles.readCloseTextStream(conn.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String encodeQuery(final HashMap<String, String> params) throws UnsupportedEncodingException {
        final StringBuilder result = new StringBuilder();
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

    public static HashMap<String, String> getDataMap(final String query) {
        final HashMap<String, String> result = new HashMap<>();
        final StringBuilder sb = new StringBuilder();
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
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return result;
    }
}

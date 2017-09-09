package io.github.lonamiwebs.stringlate.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import io.github.lonamiwebs.stringlate.interfaces.Callback;

public class FileDownloader {

    private final static int BUFFER_SIZE = 4096;

    // Downloads a file from the give url to the output file
    // Creates the file's parent directory if it doesn't exist
    public static boolean downloadFile(String url, File out) {
        return downloadFile(url, out, null);
    }

    public static boolean downloadFile(String url, File out, Callback<Float> progressCallback) {
        try {
            return downloadFile(new URL(url), out, progressCallback);
        } catch (MalformedURLException e) {
            // Won't happen
            e.printStackTrace();
            return false;
        }
    }

    public static boolean downloadFile(URL url, File outFile, Callback<Float> progressCallback) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            input = connection.getInputStream();

            if (!outFile.getParentFile().isDirectory())
                outFile.getParentFile().mkdirs();
            output = new FileOutputStream(outFile);

            int count;
            int written = 0;
            float length = connection.getContentLength();

            byte data[] = new byte[BUFFER_SIZE];
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
                if (length != -1f && progressCallback != null) {
                    written += count;
                    progressCallback.onCallback(written / length);
                }
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;

        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }
            if (connection != null)
                connection.disconnect();
        }
    }
}
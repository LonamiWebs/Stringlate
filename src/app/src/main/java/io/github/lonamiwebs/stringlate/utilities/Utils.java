package io.github.lonamiwebs.stringlate.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import io.github.lonamiwebs.stringlate.R;

public class Utils {

    //region Network

    public static boolean isNotConnected(final Context ctx, boolean warn) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        boolean notConnected = activeNetworkInfo == null || !activeNetworkInfo.isConnected();
        if (notConnected && warn) {
            Toast.makeText(ctx, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
        }
        return notConnected;
    }

    //endregion

    //region String utilities

    // https://stackoverflow.com/a/1086134
    public static String toTitleCase(final String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }

    //endregion

    //region Reading and writing files

    @NonNull
    public static String readFile(final File file) {
        try {
            return readCloseStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            Log.w("Utils/readFile", "File "+file+" not found.");
            e.printStackTrace();
        }

        return "";
    }

    @NonNull
    static String readCloseStream(final InputStream stream) {
        String line;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null)
                sb.append(line).append('\n');

            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "";
    }

    public static boolean writeFile(final File file, final String content) {
        BufferedWriter writer = null;
        try {
            if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs())
                return false;

            writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //endregion

    //region Searching in files

    // Returns -1 if the file did not contain any of the needles, otherwise,
    // the index of which needle was found in the contents of the file.
    //
    // Needless MUST be in lower-case.
    public static int fileContains(File file, String... needles) {
        try {
            FileInputStream in = new FileInputStream(file);

            int i;
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                for (i = 0; i != needles.length; ++i)
                    if (line.toLowerCase().contains(needles[i]))
                        return i;
            }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    //endregion

    //region Directories

    public static boolean deleteRecursive(File dir) {
        boolean ok = true;
        if (dir.exists()) {
            if (dir.isDirectory()) {
                for (File child : dir.listFiles())
                    ok &= deleteRecursive(child);
            }
            ok &= dir.delete();
        }
        return ok;
    }

    //endregion
}

package io.github.lonamiwebs.stringlate.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Helpers extends io.github.gsantner.opoc.util.Helpers {

    public Helpers(Context context) {
        super(context);
    }

    //region Network

    public boolean isDisconnectedFromInternet(@Nullable @StringRes Integer warnMessageStringRes) {
        ConnectivityManager connectivityManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean notConnected = activeNetworkInfo == null || !activeNetworkInfo.isConnected();
        if (notConnected && warnMessageStringRes != null) {
            Toast.makeText(_context, _context.getString(warnMessageStringRes), Toast.LENGTH_SHORT).show();
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
    public static String readTextFile(final File file) {
        try {
            return readCloseTextStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            Log.w("readTextFile", "File " + file + " not found.");
        }

        return "";
    }


    public static String readCloseTextStream(final InputStream stream) {
        return readCloseTextStream(stream, true).get(0);
    }

    @NonNull
    static List<String> readCloseTextStream(final InputStream stream, boolean concatToOneString) {
        final ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        String line = "";
        try {
            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(stream));

            while ((line = reader.readLine()) != null) {
                if (concatToOneString) {
                    sb.append(line).append('\n');
                } else {
                    lines.add(line);
                }
            }
            line = sb.toString();
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
        if (concatToOneString) {
            lines.clear();
            lines.add(line);
        }
        return lines;
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

    public static boolean copy(final File src, final File dst) {
        try {
            final FileInputStream in = new FileInputStream(src);
            try {
                final FileOutputStream out = new FileOutputStream(dst);
                try {
                    int length;
                    byte[] buf = new byte[4096];
                    while ((length = in.read(buf)) > 0)
                        out.write(buf, 0, length);

                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (IOException ignored) { }
        return false;
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

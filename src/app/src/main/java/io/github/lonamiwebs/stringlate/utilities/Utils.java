package io.github.lonamiwebs.stringlate.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.github.lonamiwebs.stringlate.R;

public class Utils {

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

    @NonNull
    public static String readFile(final File file) {
        try {
            return readCloseStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    @NonNull
    static String readCloseStream(final InputStream stream) {
        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null)
                sb.append(line).append('\n');

            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }
}

package io.github.lonamiwebs.stringlate.Utilities;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RepoHandler {
    Context mContext;
    String mOwner, mRepo;

    static final String BASE_DIR = "repos";
    static final String RAW_FILE_URL = "https://raw.githubusercontent.com/%s/%s/master/%s";

    public RepoHandler(Context context, String owner, String repo) {
        mContext = context;

        mOwner = owner;
        mRepo = repo;

        checkExists();
    }

    private File getLocaleFile(String locale) {
        String name = locale == null ? "strings.xml" : "strings-"+locale+".xml";
        return new File(mContext.getFilesDir(),
                String.format("%s/%s/%s/%s", BASE_DIR, mOwner, mRepo, name));
    }

    public void downloadLocale(String path, String locale) {
        final String urlString = String.format(RAW_FILE_URL, mOwner, mRepo, path);
        final File outputFile = getLocaleFile(locale);

        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection)url.openConnection();
            connection.connect();

            input = connection.getInputStream();
            output = new FileOutputStream(outputFile);

            int count;
            byte data[] = new byte[4096];
            while ((count = input.read(data)) != -1)
                output.write(data, 0, count);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException e) { }
            if (connection != null)
                connection.disconnect();
        }
    }

    private void checkExists() {
        File file = new File(mContext.getFilesDir(), BASE_DIR+"/"+mOwner+"/"+mRepo);
        if (!file.exists())
            file.mkdirs();
    }
}

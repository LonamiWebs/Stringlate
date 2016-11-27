package io.github.lonamiwebs.stringlate.Utilities;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.Interfaces.Callback;
import io.github.lonamiwebs.stringlate.Interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.ResourcesStrings.Resources;
import io.github.lonamiwebs.stringlate.ResourcesStrings.ResourcesParser;

public class RepoHandler {
    Context mContext;
    String mOwner, mRepo;

    File mRoot;

    Pattern mValuesLocalePattern; // Match locale from "res/values-(...)/strings.xml"
    Pattern mLocalesPattern; // Match locale from "strings-(...).xml"
    ArrayList<String> mLocales;

    public static final String DEFAULT_LOCALE = "default";

    static final String BASE_DIR = "repos";
    static final String RAW_FILE_URL = "https://raw.githubusercontent.com/%s/%s/master/%s";

    public RepoHandler(Context context, String owner, String repo) {
        mContext = context;
        mOwner = owner;
        mRepo = repo;

        mValuesLocalePattern = Pattern.compile(
                "res/values(?:-([\\w-]+))?/strings\\.xml");

        mLocalesPattern = Pattern.compile("strings(?:-([\\w-]+))?\\.xml");
        mLocales = new ArrayList<>();

        mRoot = new File(mContext.getFilesDir(), BASE_DIR+"/"+mOwner+"/"+mRepo);
        // We do not want to create any directory by default, so don't call init()
    }

    public RepoHandler init() {
        if (mRoot.isDirectory()) {
            loadLocales();
        } else {
            mRoot.mkdirs();
        }
        return this;
    }

    //region Loading single resource files

    private File getResourcesFile(String locale) {
        if (locale == null || locale.equals(DEFAULT_LOCALE))
            return new File(mRoot, "strings.xml");
        else
            return new File(mRoot, "strings-"+locale+".xml");
    }

    public Resources loadResources(String locale) throws FileNotFoundException {
        InputStream is = new FileInputStream(getResourcesFile(locale));
        try {
            ResourcesParser parser = new ResourcesParser();
            return parser.parse(is);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) { }
        }
        return null;
    }

    //endregion

    //region Loading available locale files

    private void loadLocales() {
        mLocales.clear();
        for (File f : mRoot.listFiles()) {
            String path = f.getAbsolutePath();
            Matcher m = mLocalesPattern.matcher(path);
            if (m.find())
                mLocales.add(m.group(1) == null ? DEFAULT_LOCALE : m.group(1));
        }
    }

    public ArrayList<String> getLocales() {
        return mLocales;
    }

    public boolean isEmpty() { return !mRoot.isDirectory() || mLocales.size() == 0; }

    //endregion

    //region Downloading locales

    // TODO Add some check to avoid overwriting files, i.e. was locale modified?
    public void updateStrings(ProgressUpdateCallback callback) {
        scanStringsXml(callback);
    }

    // Step 1
    private void scanStringsXml(final ProgressUpdateCallback callback) {
        callback.onProgressUpdate(
                "Scanning repository...",
                "Looking for strings.xml files in the repository...");

        GitHub.gGetTopTree(mOwner, mRepo, true, new Callback<Object>() {
            @Override
            public void onCallback(Object o) {
                ArrayList<String> remotePaths = new ArrayList<>();
                ArrayList<String> locales = new ArrayList<>();
                try {
                    JSONObject json = (JSONObject) o;
                    JSONArray tree = json.getJSONArray("tree");
                    for (int i = 0; i < tree.length(); i++) {
                        JSONObject item = tree.getJSONObject(i);
                        Matcher m = mValuesLocalePattern.matcher(item.getString("path"));
                        if (m.find()) {
                            remotePaths.add(item.getString("path"));
                            locales.add(m.group(1));
                        }
                    }
                    if (remotePaths.size() == 0) {
                        callback.onProgressFinished(
                                "No strings.xml files were found in this repository.", false);
                    } else {
                        downloadLocales(remotePaths, locales, callback);
                    }
                } catch (JSONException e) {
                    callback.onProgressFinished(
                            "Error parsing the JSON from the API request.", false);
                }
            }
        });
    }

    // Step 2
    private void downloadLocales(final ArrayList<String> remotePaths,
                                 final ArrayList<String> locales,
                                 final ProgressUpdateCallback callback) {
        callback.onProgressUpdate(
                String.format("Downloading locales 0/%d...", remotePaths.size()),
                "Downloading strings.xml files for you to translate...");

        new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                for (int i = 0; i < remotePaths.size(); i++) {
                    publishProgress(i);
                    downloadLocale(remotePaths.get(i), locales.get(i));
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                int i = values[0];
                callback.onProgressUpdate(
                        String.format("Downloading locales %d/%d...", i+1, remotePaths.size()),
                        String.format("Downloading values-%s/strings.xml", locales.get(i)));

                super.onProgressUpdate(values);
            }

            @Override
            protected void onPostExecute(Void v) {
                callback.onProgressFinished(null, true);
                super.onPostExecute(v);
            }
        }.execute();
    }

    public void downloadLocale(String remotePath, String locale) {
        final String urlString = String.format(RAW_FILE_URL, mOwner, mRepo, remotePath);
        final File outputFile = getResourcesFile(locale);

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

    //endregion
}

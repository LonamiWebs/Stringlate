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
import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.ResourcesStrings.Resources;
import io.github.lonamiwebs.stringlate.ResourcesStrings.ResourcesParser;

public class RepoHandler {
    private final Context mContext;
    private final String mOwner, mRepo;

    private final File mRoot;

    private final Pattern mValuesLocalePattern; // Match locale from "res/values-(...)/strings.xml"
    private final Pattern mLocalesPattern; // Match locale from "strings-(...).xml"
    private final ArrayList<String> mLocales;

    private static final String BASE_DIR = "repos";
    private static final String RAW_FILE_URL = "https://raw.githubusercontent.com/%s/%s/master/%s";

    public static final String DEFAULT_LOCALE = "default";

    public RepoHandler(Context context, String owner, String repo) {
        mContext = context;
        mOwner = owner;
        mRepo = repo;

        mRoot = new File(mContext.getFilesDir(), BASE_DIR+"/"+mOwner+"/"+mRepo);

        mValuesLocalePattern = Pattern.compile(
                "res/values(?:-([\\w-]+))?/strings\\.xml");

        mLocalesPattern = Pattern.compile("strings(?:-([\\w-]+))?\\.xml");
        mLocales = new ArrayList<>();
        loadLocales();
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
        if (mRoot.isDirectory()) {
            for (File f : mRoot.listFiles()) {
                String path = f.getAbsolutePath();
                Matcher m = mLocalesPattern.matcher(path);
                if (m.find())
                    mLocales.add(m.group(1) == null ? DEFAULT_LOCALE : m.group(1));
            }
        }
    }

    public ArrayList<String> getLocales() {
        return mLocales;
    }

    public boolean isEmpty() { return mLocales.isEmpty(); }

    //endregion

    //region Downloading locales

    // TODO Add some check to avoid overwriting files, i.e. was locale modified?
    public void updateStrings(ProgressUpdateCallback callback) {
        scanStringsXml(callback);
    }

    // Step 1
    private void scanStringsXml(final ProgressUpdateCallback callback) {
        callback.onProgressUpdate(
                mContext.getString(R.string.scanning_repository),
                mContext.getString(R.string.scanning_repository_long));

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
                            locales.add(m.group(1) == null ? DEFAULT_LOCALE : m.group(1));
                        }
                    }
                    if (remotePaths.size() == 0) {
                        callback.onProgressFinished(
                                mContext.getString(R.string.no_strings_found), false);
                    } else {
                        downloadLocales(remotePaths, locales, callback);
                    }
                } catch (JSONException e) {
                    callback.onProgressFinished(
                            mContext.getString(R.string.error_parsing_json), false);
                }
            }
        });
    }

    // Step 2
    private void downloadLocales(final ArrayList<String> remotePaths,
                                 final ArrayList<String> locales,
                                 final ProgressUpdateCallback callback) {
        callback.onProgressUpdate(
                mContext.getString(R.string.downloading_strings_locale, 0, remotePaths.size()),
                mContext.getString(R.string.downloading_to_translate));

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
                        mContext.getString(R.string.downloading_strings_locale, i+1, remotePaths.size()),
                        mContext.getString(R.string.downloading_strings_locale_description, locales.get(i)));

                super.onProgressUpdate(values);
            }

            @Override
            protected void onPostExecute(Void v) {
                loadLocales();
                callback.onProgressFinished(null, true);
                super.onPostExecute(v);
            }
        }.execute();
    }

    public void downloadLocale(String remotePath, String locale) {
        if (!mRoot.isDirectory())
            mRoot.mkdirs();

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

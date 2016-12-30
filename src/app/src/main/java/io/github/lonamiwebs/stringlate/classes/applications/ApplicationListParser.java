package io.github.lonamiwebs.stringlate.classes.applications;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

public class ApplicationListParser {
    // We don't use namespaces
    private static final String ns = null;

    private static final String ID = "id";
    private static final String LAST_UPDATED = "lastupdated";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "summary";
    private static final String ICON = "icon";
    private static final String SOURCE_URL = "source";

    // We will only parse https GitHub urls
    private static final Pattern GITHUB_PATTERN =
            Pattern.compile("^https?://github.com/([\\w-]+)/([\\w-]+)(?:/.*)?$");

    //region Xml -> ApplicationsList

    public static ArrayList<Application> parseFromXml(InputStream in, HashSet<String> installedPackages)
            throws XmlPullParserException, IOException {

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            ArrayList<Application> apps = readFdroid(parser);
            // Now set which applications are installed on the device
            for (Application app : apps) {
                if (installedPackages.contains(app.getPackageName()))
                    app.setInstalled(true);
            }

            return apps;
        } finally {
            try {
                in.close();
            } catch (IOException e) { }
        }
    }

    // Reads the <fdroid> tag and returns a list of its <application> tags
    private static ArrayList<Application> readFdroid(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        ArrayList<Application> apps = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, "fdroid");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;

            String name = parser.getName();
            if (name.equals("application")) {
                Application app = readApplication(parser);
                if (GITHUB_PATTERN.matcher(app.getSourceCodeUrl()).matches()) {
                    apps.add(app);
                } else if (app.getSourceCodeUrl().contains("github")) {
                    Log.w("LONAMIWEBS", "Why didn't this pass?: "+app.getSourceCodeUrl());
                }
            }
            else
                skip(parser);
        }
        return apps;
    }

    // Reads a <application id="...">...</application> tag from the xml
    private static Application readApplication(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        String packageName, lastUpdated, name, description, iconName, sourceCodeUrl;
        packageName = lastUpdated = name = description = iconName = sourceCodeUrl = "";

        parser.require(XmlPullParser.START_TAG, ns, "application");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;

            switch (parser.getName()) {
                case ID:
                    packageName = readText(parser);
                    break;
                case LAST_UPDATED:
                    lastUpdated = readText(parser);
                    break;
                case NAME:
                    name = readText(parser);
                    break;
                case DESCRIPTION:
                    description = readText(parser);
                    break;
                case ICON:
                    iconName = readText(parser);
                    break;
                case SOURCE_URL:
                    sourceCodeUrl = readText(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, "application");

        return new Application(packageName, lastUpdated,
                name, description, iconName, sourceCodeUrl);
    }

    // Reads the text from an xml tag
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Skips a tag in the xml
    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG)
            throw new IllegalStateException();

        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG: --depth; break;
                case XmlPullParser.START_TAG: ++depth; break;
            }
        }
    }

    //endregion

    //region ApplicationList -> Xml

    public static boolean parseToXml(ApplicationList applications, OutputStream out) {
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(out, "UTF-8");
            serializer.startTag(ns, "fdroid");

            for (Application app : applications) {
                serializer.startTag(ns, "application");

                writeTag(serializer, ID, app.getPackageName());
                writeTag(serializer, LAST_UPDATED, app.getLastUpdatedDateString());
                writeTag(serializer, NAME, app.getName());
                writeTag(serializer, DESCRIPTION, app.getDescription());
                writeTag(serializer, ICON, app.getIconName());
                writeTag(serializer, SOURCE_URL, app.getSourceCodeUrl());

                serializer.endTag(ns, "application");
            }
            serializer.endTag(ns, "fdroid");
            serializer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void writeTag(XmlSerializer serializer, String tag, String content)
            throws IOException {
        serializer.startTag(ns, tag);
        serializer.text(content);
        serializer.endTag(ns, tag);
    }

    //endregion
}

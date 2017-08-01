package io.github.lonamiwebs.stringlate.classes.applications;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;

import io.github.lonamiwebs.stringlate.utilities.Constants;

import static io.github.lonamiwebs.stringlate.utilities.Constants.FDROID_REPO_URL;

class ApplicationListParser {
    // parser ids
    private static final String ID = "id";
    private static final String LAST_UPDATED = "lastupdated";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "summary";
    private static final String ICON = "icon";
    private static final String ICON_URL = "sl_iconurl";
    private static final String SOURCE_URL = "source";
    private static final String WEB = "web";

    // We don't use namespaces
    private static final String ns = null;
    private static String fdroidIconPath = null;

    //region Xml -> ApplicationsList

    static ArrayList<ApplicationDetails> parseFromXml(InputStream in, HashSet<String> installedPackages)
            throws XmlPullParserException, IOException {

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            ArrayList<ApplicationDetails> apps = readFdroid(parser);
            // Now set which applications are installed on the device
            for (ApplicationDetails app : apps) {
                if (installedPackages.contains(app.getPackageName()))
                    app.setInstalled();
            }

            return apps;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    // Reads the <fdroid> tag and returns a list of its <application> tags
    private static ArrayList<ApplicationDetails> readFdroid(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        ArrayList<ApplicationDetails> apps = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, "fdroid");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;

            String name = parser.getName();
            if (name.equals("application"))
                apps.add(readApplication(parser));
            else
                skip(parser);
        }
        return apps;
    }

    // Reads a <application id="...">...</application> tag from the xml
    private static ApplicationDetails readApplication(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        String packageName, lastUpdated, name, description, iconUrl, sourceCodeUrl, webUrl;
        packageName = lastUpdated = name = description = iconUrl = sourceCodeUrl = webUrl = "";
        iconUrl = "";
        sourceCodeUrl = "";

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
                case WEB:
                    webUrl = readText(parser);
                    break;
                case ICON:
                    if (fdroidIconPath != null) {
                        iconUrl = fdroidIconPath + readText(parser);
                    }
                    break;
                case ICON_URL:
                    iconUrl = readText(parser);
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

        return new ApplicationDetails(packageName, lastUpdated,
                name, description, iconUrl, sourceCodeUrl, webUrl);
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
                case XmlPullParser.END_TAG:
                    --depth;
                    break;
                case XmlPullParser.START_TAG:
                    ++depth;
                    break;
            }
        }
    }

    //endregion

    //region ApplicationList -> Xml

    static boolean parseToXml(ApplicationList applications, OutputStream out) {
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(out, "UTF-8");
            serializer.startTag(ns, "fdroid");

            for (ApplicationDetails app : applications) {
                serializer.startTag(ns, "application");

                writeTag(serializer, ID, app.getPackageName());
                writeTag(serializer, LAST_UPDATED, app.getLastUpdatedDateString());
                writeTag(serializer, NAME, app.getName());
                writeTag(serializer, DESCRIPTION, app.getDescription());
                writeTag(serializer, ICON_URL, app.getIconUrl());
                writeTag(serializer, SOURCE_URL, app.getSourceCodeUrl());
                writeTag(serializer, WEB, app.getWebUrl());

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

    public static void loadFDroidIconPath(Context context) {
        final double dpi = context.getResources().getDisplayMetrics().densityDpi;
        String iconDir = Constants.FALLBACK_FDROID_ICONS_DIR;
        if (dpi >= 120) iconDir = "/icons-120/";
        if (dpi >= 160) iconDir = "/icons-160/";
        if (dpi >= 240) iconDir = "/icons-240/";
        if (dpi >= 320) iconDir = "/icons-320/";
        if (dpi >= 480) iconDir = "/icons-480/";
        if (dpi >= 640) iconDir = "/icons-640/";
        fdroidIconPath = FDROID_REPO_URL + iconDir;
    }
}

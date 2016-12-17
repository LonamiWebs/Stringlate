package io.github.lonamiwebs.stringlate.classes.resources;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;

// Class used to parse strings.xml files into Resources objects
public class ResourcesParser {

    //region Constants

    // We don't use namespaces
    private final static String ns = null;

    private final static String RESOURCES = "resources";
    private final static String STRING = "string";
    private final static String ID = "name";
    private final static String TRANSLATABLE = "translatable";
    private final static String MODIFIED = "modified";

    private final static boolean DEFAULT_TRANSLATABLE = true;
    private final static boolean DEFAULT_MODIFIED = false;

    //endregion

    //region Xml -> Resources

    public static HashSet<ResourcesString> parseFromXml(InputStream in)
            throws XmlPullParserException, IOException {

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readResources(parser);
        } finally {
            try {
                in.close();
            } catch (IOException e) { }
        }
    }

    // Reads the <resources> tag and returns a list of its <string> tags
    private static HashSet<ResourcesString> readResources(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        HashSet<ResourcesString> strings = new HashSet<>();

        parser.require(XmlPullParser.START_TAG, ns, RESOURCES);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;

            String name = parser.getName();
            if (name.equals(STRING))
                strings.add(readResourceString(parser));
            else
                skip(parser);
        }
        return strings;
    }

    // Reads a <string name="...">...</string> tag from the xml
    private static ResourcesString readResourceString(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        String id, content;
        boolean translatable, modified;

        parser.require(XmlPullParser.START_TAG, ns, STRING);

        id = parser.getAttributeValue(null, ID);
        translatable = readBooleanAttr(parser, TRANSLATABLE, DEFAULT_TRANSLATABLE);

        // Metadata
        modified = readBooleanAttr(parser, MODIFIED, DEFAULT_MODIFIED);

        // The content must be read last, since it also consumes the tag
        content = getInnerXml(parser);
        parser.require(XmlPullParser.END_TAG, ns, STRING);

        return new ResourcesString(id, content, translatable, modified);
    }

    // Reads a boolean attribute from an xml tag
    private static boolean readBooleanAttr(XmlPullParser parser, String attr, boolean defaultV) {
        String value = parser.getAttributeValue(null, attr);
        if (value == null)
            return defaultV;

        return Boolean.parseBoolean(value);
    }

    // Reads the inner xml of a tag before moving to the next one
    // Original source: stackoverflow.com/a/16069754/4759433 by @Maarten
    private static String getInnerXml(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        StringBuilder sb = new StringBuilder();
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    if (depth > 0) {
                        sb.append("</")
                                .append(parser.getName())
                                .append(">");
                    }
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    StringBuilder attrs = new StringBuilder();
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        attrs.append(parser.getAttributeName(i))
                                .append("=\"")
                                .append(parser.getAttributeValue(i))
                                .append("\" ");
                    }
                    sb.append("<")
                            .append(parser.getName())
                            .append(" ")
                            .append(attrs.toString())
                            .append(">");
                    break;
                default:
                    sb.append(parser.getText());
                    break;
            }
        }
        return sb.toString();
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

    //region Resources -> Xml

    public static boolean parseToXml(Resources resources, OutputStream out,
                                     boolean indent, boolean addMetadata) {
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(out, "UTF-8");
            if (indent)
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            //strings.xml do not have the default start declaration
            //serializer.startDocument("UTF-8", true);
            serializer.startTag(ns, RESOURCES);

            for (ResourcesString rs : resources) {
                if (!rs.hasContent())
                    continue;

                serializer.startTag(ns, STRING);
                serializer.attribute(ns, ID, rs.getId());

                // Only save changes that differ from the default, to save space
                if (rs.isTranslatable() != DEFAULT_TRANSLATABLE)
                    serializer.attribute(ns, TRANSLATABLE, Boolean.toString(rs.isTranslatable()));

                if (addMetadata) {
                    if (rs.wasModified() != DEFAULT_MODIFIED)
                        serializer.attribute(ns, MODIFIED, Boolean.toString(rs.wasModified()));
                }

                serializer.text(rs.getContent());
                serializer.endTag(ns, STRING);
            }
            serializer.endTag(ns, RESOURCES);
            serializer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //endregion
}

package io.github.lonamiwebs.stringlate.classes.resources;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Class used to parse strings.xml files into Resources objects
// Please NOTE that strings with `translatable="false"` will NOT be parsed
// The application doesn't need these to work (as of now, if any use is found, revert this file)
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
            if (name.equals(STRING)) {
                ResourcesString rs = readResourceString(parser);
                // Avoid strings that cannot be translated (these will be null)
                if (rs != null)
                    strings.add(rs);
            }
            else
                skip(parser);
        }
        return strings;
    }

    // Reads a <string name="...">...</string> tag from the xml.
    // Returns null if the string cannot be translated
    private static ResourcesString readResourceString(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        String id, content;
        boolean modified;

        parser.require(XmlPullParser.START_TAG, ns, STRING);

        if (!readBooleanAttr(parser, TRANSLATABLE, DEFAULT_TRANSLATABLE)) {
            // We don't care about not-translatable strings
            skipInnerXml(parser);
            parser.require(XmlPullParser.END_TAG, ns, STRING);
            return null;
        } else {
            id = parser.getAttributeValue(null, ID);

            // Metadata
            modified = readBooleanAttr(parser, MODIFIED, DEFAULT_MODIFIED);

            // The content must be read last, since it also consumes the tag
            content = getInnerXml(parser);
            parser.require(XmlPullParser.END_TAG, ns, STRING);

            return new ResourcesString(id, content, modified);
        }
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

    // Skips the inner XML once inside a tag
    private static void skipInnerXml(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG: depth--; break;
                case XmlPullParser.START_TAG: depth++; break;
            }
        }
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

    public static boolean parseToXml(Resources resources, OutputStream out) {
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(out, "UTF-8");
            serializer.startTag(ns, RESOURCES);

            for (ResourcesString rs : resources) {
                if (!rs.hasContent())
                    continue;

                serializer.startTag(ns, STRING);
                serializer.attribute(ns, ID, rs.getId());

                // Only save changes that differ from the default, to save space
                if (rs.wasModified() != DEFAULT_MODIFIED)
                    serializer.attribute(ns, MODIFIED, Boolean.toString(rs.wasModified()));

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

    //region Using another file as a template

    // Matches '<string attrA="value" attrB="value">content</string>'
    private static final Pattern STRING_TAG_PATTERN =
            Pattern.compile("(<string(?:\\s+\\w+\\s*=\\s*\"\\w+\")+\\s*>)(.+?)(</\\s*string\\s*>)");
    //      ^                 <string        attr   =     "value"      >content</    string    >

    // Matches 'name="value"'
    private static final Pattern STRING_NAME_PATTERN =
            Pattern.compile("name\\s*=\\s*\"(\\w+)\"");
    //      ^                name    =     "value__"

    // Replacement holder - on the original xml (the "template")
    // there will exist several of these to indicate where the
    // replacements should be made
    private static class ReplaceHolder {
        int start, end;
        String replacement;

        ReplaceHolder(int start, int end, String replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }
    }

    // TODO < and > should not ALWAYS be replaced (although XmlSerializer does that too)
    // If the xml made by string is valid, then these should be kept
    private static String sanitize(StringBuilder buffer, String string) {
        char c;
        buffer.setLength(0);
        for (int i = 0; i < string.length(); i++) {
            c = string.charAt(i);
            switch (c) {
                case '&': buffer.append("&amp;"); break;
                case '<': buffer.append("&lt;"); break;
                case '>': buffer.append("&gt;"); break;
                default: buffer.append(c); break;
            }
        }
        return buffer.toString();
    }

    public static boolean applyTemplate(File originalFile, Resources resources, OutputStream out) {
        int length = (int)originalFile.length();
        byte[] bytes = new byte[length];
        try {
            FileInputStream in = new FileInputStream(originalFile);
            try { in.read(bytes); }
            finally { in.close(); }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return applyTemplate(new String(bytes), resources, out);
    }

    // Approach used: match all the <string> tags and create
    // ReplaceHolder objects accordingly; then, walk through the
    // original xml string and apply the appropriate replacements
    // TODO A better solution should be used; although I don't know any
    public static boolean applyTemplate(String originalXml, Resources resources, OutputStream out) {
        // Don't create an instance of the buffer all the time
        StringBuilder sanitizeBuffer = new StringBuilder();

        // Match on the original xml to determine where replacements will be made
        Matcher m = STRING_TAG_PATTERN.matcher(originalXml);

        // Matches will be in order, so use a queue (first in, first out)
        Queue<ReplaceHolder> holders = new LinkedList<>();
        while (m.find()) {
            int tagStart = m.start(); // '<string...
            int tagSize = m.group(1).length(); // '<string...>'
            int tagContentSize = m.group(2).length(); // >'...'</

            // Find the resource ID on the first group ('<string name="...">')
            Matcher nameMatcher = STRING_NAME_PATTERN.matcher(m.group(1));
            if (nameMatcher.find()) {
                // Should never fail
                String content = resources.getContent(nameMatcher.group(1));
                if (content != null && !content.isEmpty()) {
                    holders.add(new ReplaceHolder(
                            tagStart+tagSize,
                            tagStart+tagSize+tagContentSize,
                            sanitize(sanitizeBuffer, content)));
                } else {
                    // TODO: This line should be removed (or if it's untranslatable)
                }
            }
        }

        // Write the result by applying the required replacements
        PrintWriter outWriter = new PrintWriter(out);
        ReplaceHolder holder = holders.poll();
        for (int i = 0; i < originalXml.length(); i++) {
            if (holder == null) {
                // There are no more holders left, simply copy the characters
                outWriter.append(originalXml.charAt(i));
            }
            else {
                if (i < holder.start) {
                    // We're below the index of the next replacement, keep copying characters
                    outWriter.append(originalXml.charAt(i));
                } else {
                    // We reached the current replacement holder
                    // Replace the original content with our new content
                    outWriter.append(holder.replacement);
                    i = holder.end-1; // Skip to the end of the tag

                    holder = holders.poll(); // Next holder
                }
            }
        }
        outWriter.flush();
        return true;
    }

    //endregion
}

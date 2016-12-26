package io.github.lonamiwebs.stringlate.classes.resources;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
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

    // Matches '</string>'
    private static final Pattern STRING_CLOSE_TAG_PATTERN =
            Pattern.compile("</\\s*string\\s*>");
    //      ^                </    string    >

    // Matches 'name="value"'
    private static final Pattern STRING_NAME_PATTERN =
            Pattern.compile("name\\s*=\\s*\"(\\w+)\"");
    //      ^                name    =     "value__"

    // Matches 'translatable="false"'
    private static final Pattern STRING_UNTRANSLATABLE_PATTERN =
            Pattern.compile("translatable\\s*=\\s*\"false\"");
    //      ^                translatable    =     "false"

    // TODO < and > should not ALWAYS be replaced (although XmlSerializer does that too)
    // If the xml made by string is valid, then these should be kept
    private static void writeSanitize(BufferedWriter writer, String string)
            throws IOException {
        char c;
        for (int i = 0; i < string.length(); i++) {
            c = string.charAt(i);
            switch (c) {
                case '&': writer.write("&amp;"); break;
                case '<': writer.write("&lt;"); break;
                case '>': writer.write("&gt;"); break;
                default: writer.write(c); break;
            }
        }
    }

    // Approach used: read the original xml file line by line and
    // find the <string> tags with a regex so we can tell where to replace.
    // Then apply the replacements accordingly when writing to the output stream.
    public static boolean applyTemplate(File originalFile, Resources resources, OutputStream out) {
        // Read the file line by line. We will ignore those that contain
        // translatable="false" or those for which we have no translation
        // TODO This case is more common: a line might span across multiple lines!

        try {
            FileInputStream in = new FileInputStream(originalFile);

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

            while ((line = reader.readLine()) != null) {
                // Match on the current line to determine where replacements will be made
                Matcher m = STRING_TAG_PATTERN.matcher(line);
                if (m.find()) {
                    // Count the occurrences of </string>
                    int count = 0;
                    Matcher closeTagMatcher = STRING_CLOSE_TAG_PATTERN.matcher(line);
                    while (closeTagMatcher.find()) {
                        count++;
                    }
                    if (count == 1) {
                        // Best case, single line with a single tag
                        handleSingleTagLine(line, m, resources, writer);
                    } else {
                        // Harder case, we need to handle many tags in a single line
                        handleMultitagLine(line, resources, writer);
                    }
                } else {
                    // Match not found, perhaps this is a tag which spans across multiple lines
                    if (line.contains("<string")) {
                        // Even the <string> tag declaration can span across multiple lines!
                        handleMultilineTag();
                    } else {
                        // This line has no <string> tag, simply append it to the result
                        writer.write(line);
                        writer.write('\n'); // New line character was consumed by .readLine()
                    }
                }
            }

            writer.flush();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static void handleSingleTagLine(String line, Matcher m,
                                            Resources resources, BufferedWriter writer)
            throws IOException {
        // Match found, determine whether it's translatable or not and its name
        // Find the resource ID on the first group ('<string name="...">')
        Matcher nameMatcher = STRING_NAME_PATTERN.matcher(m.group(1));
        Matcher untrMatcher = STRING_UNTRANSLATABLE_PATTERN.matcher(m.group(1));
        boolean untranslatable = untrMatcher.find();

        if (nameMatcher.find()) {
            // Should never fail
            String content = resources.getContent(nameMatcher.group(1));

            if (untranslatable || content == null || content.isEmpty()) {
                // Do not add this line to the final result
            } else {
                // We have the content and we're the replacement should be made
                // so perform the replacement and add this line
                int contentStart = m.start() + m.group(1).length();
                int contentEnd = contentStart + m.group(2).length();

                writer.write(line, 0, contentStart);
                writeSanitize(writer, content);
                writer.write(line, contentEnd, line.length()-contentEnd);
                writer.write('\n'); // New line character was consumed by .readLine()
            }
        }
    }

    private static void handleMultitagLine(String line, Resources resources, BufferedWriter writer)
            throws IOException {
        int lastIndex = 0; // From where we need to copy until the next tag

        // We need to iterate over all the matches on this line
        Matcher m = STRING_TAG_PATTERN.matcher(line);
        while (m.find()) {
            // Match found, determine whether it's translatable or not and its name
            // Find the resource ID on the first group ('<string name="...">')
            Matcher nameMatcher = STRING_NAME_PATTERN.matcher(m.group(1));
            Matcher untrMatcher = STRING_UNTRANSLATABLE_PATTERN.matcher(m.group(1));
            boolean untranslatable = untrMatcher.find();

            if (nameMatcher.find()) {
                // Should never fail
                String content = resources.getContent(nameMatcher.group(1));

                int tagStart = m.start();
                int contentStart = tagStart + m.group(1).length();
                int contentEnd = contentStart + m.group(2).length();
                int tagEnd = contentEnd + m.group(3).length();

                // Always write up to this point
                writer.write(line, lastIndex, tagStart-lastIndex);

                if (untranslatable || content == null || content.isEmpty()) {
                    // Do not add this tag to the final result
                } else {
                    // We have the content and we're the replacement should be made
                    // so perform the replacement and add this line
                    writer.write(line, tagStart, contentStart-tagStart);
                    writeSanitize(writer, content);
                    writer.write(line, contentEnd, tagEnd-contentEnd);
                }

                lastIndex = tagEnd;
            }
        }
        writer.write(line, lastIndex, line.length()-lastIndex);
        writer.write('\n'); // New line character was consumed by .readLine()
    }

    private static void handleMultilineTag() {

    }

    //endregion
}

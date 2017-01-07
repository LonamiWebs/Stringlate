package io.github.lonamiwebs.stringlate.classes.resources;

import android.util.Pair;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.classes.resources.tags.ResPlurals;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResString;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResStringArray;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;

// Class used to parse strings.xml files into Resources objects
// Please NOTE that strings with `translatable="false"` will NOT be parsed
// The application doesn't need these to work (as of now, if any use is found, revert this file)
public class ResourcesParser {

    //region Constants

    // We don't use namespaces
    private final static String ns = null;

    private final static String RESOURCES = "resources";

    private final static String STRING = "string";
    private final static String STRING_ARRAY = "string-array";
    private final static String PLURALS = "plurals";
    private final static String ITEM = "item";

    private final static String ID = "name";
    private final static String TRANSLATABLE = "translatable";
    private final static String QUANTITY = "quantity";
    private final static String MODIFIED = "modified";

    private final static String REMOTE_PATH = "remote";

    private final static boolean DEFAULT_TRANSLATABLE = true;
    private final static boolean DEFAULT_MODIFIED = false;

    //endregion

    //region Xml -> Resources

    public static Pair<HashSet<ResTag>, String> parseFromXml(InputStream in)
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
            } catch (IOException ignored) { }
        }
    }

    private static Pair<HashSet<ResTag>, String> readResources(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        HashSet<ResTag> strings = new HashSet<>();

        parser.require(XmlPullParser.START_TAG, ns, RESOURCES);
        String remotePath = parser.getAttributeValue(null, REMOTE_PATH);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;

            String name = parser.getName();
            switch (name) {
                case STRING:
                    ResTag rs = readResourceString(parser);
                    if (rs != null)
                        strings.add(rs);
                    break;
                case STRING_ARRAY:
                    for (ResTag item : readResourceStringArray(parser))
                        strings.add(item);
                    break;
                case PLURALS:
                    for (ResTag item : readResourcePlurals(parser))
                        strings.add(item);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        return new Pair<>(strings, remotePath);
    }

    // Reads a <string name="...">...</string> tag from the xml.
    // Returns null if the string cannot be translated
    private static ResString readResourceString(XmlPullParser parser)
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
            // Android uses \n for new line, XML doesn't so manually replace it
            // But first, replace the XML new lines with normal spaces
            content = getInnerXml(parser);
            content = content.replace('\n', ' ').replace("\\n", "\n");

            parser.require(XmlPullParser.END_TAG, ns, STRING);

            if (content.isEmpty())
                return null;
            else
                return new ResString(id, content, modified);
        }
    }

    // Reads a <string-array name="...">...</string-array> tag from the xml.
    private static Iterable<ResStringArray.Item> readResourceStringArray(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        ResStringArray result;
        String id;

        parser.require(XmlPullParser.START_TAG, ns, STRING_ARRAY);

        if (!readBooleanAttr(parser, TRANSLATABLE, DEFAULT_TRANSLATABLE)) {
            // We don't care about not-translatable strings
            skipInnerXml(parser);
            parser.require(XmlPullParser.END_TAG, ns, STRING_ARRAY);
            return new ArrayList<>();
        } else {
            id = parser.getAttributeValue(null, ID);
            result = new ResStringArray(id);

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG)
                    continue;

                String name = parser.getName();
                if (name.equals(ITEM)) {
                    parser.require(XmlPullParser.START_TAG, ns, ITEM);
                    boolean modified = readBooleanAttr(parser, MODIFIED, DEFAULT_MODIFIED);
                    String content = getInnerXml(parser).replace('\n', ' ').replace("\\n", "\n");
                    if (!content.isEmpty())
                        result.addItem(content, modified);
                } else {
                    skip(parser);
                }
            }

            return result.expand();
        }
    }

    // Reads a <string-array name="...">...</string-array> tag from the xml.
    private static Iterable<ResPlurals.Item> readResourcePlurals(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        ResPlurals result;
        String id;

        parser.require(XmlPullParser.START_TAG, ns, PLURALS);

        if (!readBooleanAttr(parser, TRANSLATABLE, DEFAULT_TRANSLATABLE)) {
            // We don't care about not-translatable strings
            skipInnerXml(parser);
            parser.require(XmlPullParser.END_TAG, ns, PLURALS);
            return new ArrayList<>();
        } else {
            id = parser.getAttributeValue(null, ID);
            result = new ResPlurals(id);

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG)
                    continue;

                String name = parser.getName();
                if (name.equals(ITEM)) {
                    parser.require(XmlPullParser.START_TAG, ns, ITEM);
                    String quantity = parser.getAttributeValue(null, QUANTITY);
                    boolean modified = readBooleanAttr(parser, MODIFIED, DEFAULT_MODIFIED);
                    String content = getInnerXml(parser).replace('\n', ' ').replace("\\n", "\n");
                    if (!content.isEmpty())
                        result.addItem(quantity, content, modified);
                } else {
                    skip(parser);
                }
            }

            return result.expand();
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

        // We need to keep track of the parents which we have done already.
        // This is because we previously expanded the children, but they're
        // wrapped under the same parent (which we cannot duplicate).
        HashSet<String> doneParents = new HashSet<>();
        try {
            serializer.setOutput(out, "UTF-8");
            serializer.startTag(ns, RESOURCES);
            serializer.attribute(ns, REMOTE_PATH, resources.getRemoteUrl());

            for (ResTag rs : resources) {
                if (!rs.hasContent())
                    continue;

                if (rs instanceof ResString) {
                    parseString(serializer, (ResString)rs);
                }
                else if (rs instanceof ResStringArray.Item) {
                    ResStringArray parent = ((ResStringArray.Item)rs).getParent();
                    if (!doneParents.contains(parent.getId())) {
                        doneParents.add(parent.getId());
                        parseStringArray(serializer, parent);
                    }
                }
                else if (rs instanceof ResPlurals.Item) {
                    ResPlurals parent = ((ResPlurals.Item)rs).getParent();
                    if (!doneParents.contains(parent.getId())) {
                        doneParents.add(parent.getId());
                        parsePlurals(serializer, parent);
                    }
                }
            }
            serializer.endTag(ns, RESOURCES);
            serializer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void parseString(XmlSerializer serializer, ResString string)
            throws IOException {
        serializer.startTag(ns, STRING);
        serializer.attribute(ns, ID, string.getId());

        // Only save changes that differ from the default, to save space
        if (string.wasModified() != DEFAULT_MODIFIED)
            serializer.attribute(ns, MODIFIED, Boolean.toString(string.wasModified()));

        // Replace the new lines by \n again
        serializer.text(string.getContent().replace("\n", "\\n"));
        serializer.endTag(ns, STRING);
    }

    private static void parseStringArray(XmlSerializer serializer, ResStringArray array)
            throws IOException {
        serializer.startTag(ns, STRING_ARRAY);
        serializer.attribute(ns, ID, array.getId());

        // Ensure that we save the strings in the correct order
        array.sort();
        for (ResStringArray.Item item : array.expand()) {
            serializer.startTag(ns, ITEM);
            if (item.wasModified() != DEFAULT_MODIFIED)
                serializer.attribute(ns, MODIFIED, Boolean.toString(item.wasModified()));

            serializer.text(item.getContent().replace("\n", "\\n"));
            serializer.endTag(ns, ITEM);
        }

        serializer.endTag(ns, STRING_ARRAY);
    }

    private static void parsePlurals(XmlSerializer serializer, ResPlurals plurals)
            throws IOException {
        serializer.startTag(ns, PLURALS);
        serializer.attribute(ns, ID, plurals.getId());

        for (ResPlurals.Item item : plurals.expand()) {
            serializer.startTag(ns, ITEM);
            serializer.attribute(ns, QUANTITY, item.getQuantity());

            if (item.wasModified() != DEFAULT_MODIFIED)
                serializer.attribute(ns, MODIFIED, Boolean.toString(item.wasModified()));

            serializer.text(item.getContent().replace("\n", "\\n"));
            serializer.endTag(ns, ITEM);
        }

        serializer.endTag(ns, PLURALS);
    }

    //endregion

    //region Using another file as a template

    //region Matches

    private static final Pattern STRING_TAG_PATTERN =
    //                        <string        attr   =     "value"      >content</    string    >
            Pattern.compile("(<string(?:\\s+\\w+\\s*=\\s*\"\\w+\")+\\s*>)(.+?)(</\\s*string\\s*>)");

    private static final Pattern STRING_CLOSE_TAG_PATTERN =
    //                       </    string    >
            Pattern.compile("</\\s*string\\s*>");

    private static final Pattern STRING_NAME_PATTERN =
    //                       name    =     "value__"
            Pattern.compile("name\\s*=\\s*\"(\\w+)\"");

    private static final Pattern STRING_TRANSLATABLE_PATTERN =
    //                       translatable    =     "value__"
            Pattern.compile("translatable\\s*=\\s*\"(\\w+)\"");

    //endregion

    //region Writer utilities

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
                case '\n': writer.write("\\n"); break;
                default: writer.write(c); break;
            }
        }
    }

    //endregion

    //region Applying the template

    // Approach used: read the original xml file line by line and
    // find the <string> tags with a regex so we can tell where to replace.
    // Then apply the replacements accordingly when writing to the output stream.
    public static boolean applyTemplate(File originalFile, Resources resources, OutputStream out) {
        // Read the file line by line. We will ignore those that contain
        // translatable="false" or those for which we have no translation

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
                        handleMultiTagLine(line, resources, writer);
                    }
                } else {
                    // Match not found, perhaps this is a tag which spans across multiple lines
                    if (line.contains("<string")) {
                        // Even the <string> tag declaration can span across multiple lines!
                        handleMultilineTag(line, reader, resources, writer);
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
        Matcher tranMatcher = STRING_TRANSLATABLE_PATTERN.matcher(m.group(1));
        boolean translatable = tranMatcher.find() ?
                "true".equals(tranMatcher.group(1)) : DEFAULT_TRANSLATABLE;

        if (nameMatcher.find()) {
            // Should never fail
            String content = resources.getContent(nameMatcher.group(1));

            if (translatable && content != null && !content.isEmpty()) {
                // We have the content and we're the replacement should be made
                // so perform the replacement and add this line
                int contentStart = m.start() + m.group(1).length();
                int contentEnd = contentStart + m.group(2).length();

                writer.write(line, 0, contentStart);
                writeSanitize(writer, content);
                writer.write(line, contentEnd, line.length()-contentEnd);
                writer.write('\n'); // New line character was consumed by .readLine()
            }
            // else do not add this line to the final result
        }
    }

    private static void handleMultiTagLine(String line, Resources resources, BufferedWriter writer)
            throws IOException {
        int lastIndex = 0; // From where we need to copy until the next tag

        // We need to iterate over all the matches on this line
        Matcher m = STRING_TAG_PATTERN.matcher(line);
        while (m.find()) {
            // Match found, determine whether it's translatable or not and its name
            // Find the resource ID on the first group ('<string name="...">')
            Matcher nameMatcher = STRING_NAME_PATTERN.matcher(m.group(1));
            Matcher tranMatcher = STRING_TRANSLATABLE_PATTERN.matcher(m.group(1));
            boolean translatable = tranMatcher.find() ?
                    "true".equals(tranMatcher.group(1)) : DEFAULT_TRANSLATABLE;

            if (nameMatcher.find()) {
                // Should never fail
                String content = resources.getContent(nameMatcher.group(1));

                int tagStart = m.start();
                int contentStart = tagStart + m.group(1).length();
                int contentEnd = contentStart + m.group(2).length();
                int tagEnd = contentEnd + m.group(3).length();

                // Always write up to this point
                writer.write(line, lastIndex, tagStart-lastIndex);

                if (translatable && content != null && !content.isEmpty()) {
                    // We have the content and we're the replacement should be made
                    // so perform the replacement and add this line
                    writer.write(line, tagStart, contentStart-tagStart);
                    writeSanitize(writer, content);
                    writer.write(line, contentEnd, tagEnd-contentEnd);
                }
                // else do not add this tag to the final result

                lastIndex = tagEnd;
            }
        }
        writer.write(line, lastIndex, line.length()-lastIndex);
        writer.write('\n'); // New line character was consumed by .readLine()
    }

    private static void handleMultilineTag(String line, BufferedReader reader,
                                           Resources resources, BufferedWriter writer)
            throws IOException {
        // We need to find the translated contents, and whether it's translatable or not
        String content = null;
        Boolean translatable = null;
        boolean consumeTag = false;

        // If the string is valid, we'll need to know how the tag was defined
        StringBuilder sb = new StringBuilder();
        boolean contentStarted = false;

        do {
            if (content == null) {
                Matcher nameMatcher = STRING_NAME_PATTERN.matcher(line);
                if (nameMatcher.find()) {
                    content = resources.getContent(nameMatcher.group(1));
                    // If this string has no translation, then consume the tag
                    if (content == null || content.isEmpty())
                        consumeTag = true;
                }
            }
            if (translatable == null) {
                Matcher tranMatcher = STRING_TRANSLATABLE_PATTERN.matcher(line);
                if (tranMatcher.find()) {
                    translatable = "true".equals(tranMatcher.group(1));
                    if (!translatable) {
                        // This tag cannot be translated, then consume it
                        consumeTag = true;
                    }
                }
            }
            if (line.contains(">")) {
                contentStarted = true;
                if (content == null) {
                    // If the content started but we have no translation, consume the tag
                    consumeTag = true;
                } else {
                    // Otherwise, append the start of this line, since it's part of the tag
                    // Then, append our translated content and reach '</string', we're done!
                    sb.append(line.substring(0, line.indexOf('>')+1));
                    writer.write(sb.toString());
                    writeSanitize(writer, content);

                    int endTag;
                    while ((endTag = line.indexOf("</string")) == -1)
                        line = reader.readLine();
                    writer.write(line, endTag, line.length()-endTag);
                    writer.write('\n');
                    // Exit since we're done
                    break;
                }
            }

            if (consumeTag) {
                while (!line.contains("</string"))
                    line = reader.readLine();
                break;
            } else if (!contentStarted) {
                // If the content hasn't started yet, append the lines which are part of the tag
                sb.append(line);
                sb.append('\n');
            }
        } while ((line = reader.readLine()) != null);
    }

    //endregion

    //endregion
}

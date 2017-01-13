package io.github.lonamiwebs.stringlate.classes.resources;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
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
    private final static String INDEX = "index";
    private final static String MODIFIED = "modified";

    private final static String REMOTE_PATH = "remote";

    private final static boolean DEFAULT_TRANSLATABLE = true;
    private final static boolean DEFAULT_MODIFIED = false;
    private final static int DEFAULT_INDEX = -1;

    //endregion

    //region Xml -> Resources

    public static HashSet<ResTag> parseFromXml(InputStream in)
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

    private static HashSet<ResTag> readResources(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        HashSet<ResTag> strings = new HashSet<>();

        parser.require(XmlPullParser.START_TAG, ns, RESOURCES);

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

        return strings;
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
            // TODO Actually there shouldn't be any though if we already cleaned the .xml file?
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
                    int index = readIntAttr(parser, INDEX, DEFAULT_INDEX);

                    String content = getInnerXml(parser).replace('\n', ' ').replace("\\n", "\n");
                    if (!content.isEmpty())
                        result.addItem(content, modified, index);
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

    private static int readIntAttr(XmlPullParser parser, String attr, int defaultV) {
        String value = parser.getAttributeValue(null, attr);
        if (value == null)
            return defaultV;

        return Integer.parseInt(value);
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

    static boolean parseToXml(Resources resources, OutputStream out) {
        XmlSerializer serializer = Xml.newSerializer();

        // We need to keep track of the parents which we have done already.
        // This is because we previously expanded the children, but they're
        // wrapped under the same parent (which we cannot duplicate).
        HashSet<String> doneParents = new HashSet<>();
        try {
            serializer.setOutput(out, "UTF-8");
            serializer.startTag(ns, RESOURCES);

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

        for (ResStringArray.Item item : array.expand()) {
            serializer.startTag(ns, ITEM);
            if (item.wasModified() != DEFAULT_MODIFIED)
                serializer.attribute(ns, MODIFIED, Boolean.toString(item.wasModified()));

            // We MUST save the index because the user might have
            // translated first the non-first item from the array. Darn it!
            serializer.attribute(ns, INDEX, Integer.toString(item.getIndex()));
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

    //region Xml -> Xml without untranslatable strings

    private static final Pattern TAG_UNTRANSLATABLE_PATTERN =
            //                 tagName    translatable    =     "false"
            Pattern.compile("<([\\w-]+).*?translatable\\s*=\\s*\"false\".*?>");

    public static void cleanXml(File inFile, File outFile) {
        try {
            if (!outFile.getParentFile().isDirectory())
                outFile.getParentFile().mkdirs();

            FileInputStream in = new FileInputStream(inFile);
            FileOutputStream out = new FileOutputStream(outFile);

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

            while ((line = reader.readLine()) != null) {
                // Check whether this line is translatable or not
                Matcher m = TAG_UNTRANSLATABLE_PATTERN.matcher(line);
                if (m.find()) {
                    // TODO We assume that there are not two tags in the same line,
                    // neither the tag spans across multiple lines (either start or end tag)
                    String tagName = m.group(1);
                    switch (tagName) {
                        case STRING:
                        case STRING_ARRAY:
                        case PLURALS:
                            // Look for the closing tag and omit all the lines in between
                            Pattern closing = Pattern.compile("</\\s*" + tagName);
                            while (!closing.matcher(line).find())
                                line = reader.readLine();
                            break;
                        default:
                            // What is this tag doing here…? What tag even could it be?
                            writer.write(line);
                            writer.write('\n');
                            break;
                    }
                } else {
                    // This line may be a comment, or translatable, we don't care. Simply append it
                    writer.write(line);
                    writer.write('\n');
                }
            }

            writer.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //endregion

    //region Using another file as a template

    //region Writer utilities

    private static void writeSanitize(PrintWriter writer, String string) {
        char c;
        int length = string.length();
        // First count '<' and '>' on the string. If there is the
        // same amount  of each on the string, assume it's valid HTML
        int open = 0;
        for (int i = 0; i < length; i++) {
            switch (string.charAt(i)) {
                case '<': open++; break;
                case '>': open--; break;
            }
        }
        boolean replaceLtGt = open > 0; // Using > is actually OK (open will be negative)

        for (int i = 0; i < length; i++) {
            c = string.charAt(i);
            switch (c) {
                // These should always be escaped
                case '&': writer.append("&amp;"); break;
                case '\'': writer.append("\\'"); break;

                // We might or not need to replace <>
                case '<': writer.append(replaceLtGt ? "&lt;" : "<"); break;
                case '>': writer.append(replaceLtGt ? "&gt;" : ">"); break;

                // Quotes are a bit special
                case '"':
                    if (i == 0 || i == string.length() - 1)
                        writer.append('"');
                    else
                        writer.append("\\\"");
                    break;

                // Normal character
                default: writer.append(c); break;
            }
        }
    }

    //endregion

    //region Applying the template

    // This will match either <string>, <string-array>, <plurals> or <item> from start to end.
    // It will also match the attributes (attribute_a="value" attribute_b="value") and the content.
    private static Pattern RES_TAG_PATTERN = Pattern.compile(
            "<(string(?:-array)?|plurals|item)((?:\\s+\\w+\\s*=\\s*\"\\w+\")*)\\s*>([\\S\\s]*?)(?:</\\s*\\1\\s*>)");

    // This should be matched against the .group(2) from the above pattern
    private static Pattern ATTRIBUTE_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*\"(\\w+)\"");

    //region Actual code

    private static class DirtyRange {
        final int start, end;
        DirtyRange(int s, int e) {
            start = s;
            end = e;
        }
    }

    // Replacement holder - on the original xml (the "template")
    // there will exist several of these to indicate where the
    // replacements should be made
    private static class ReplaceHolder {
        final int start, end;
        final String replacement;

        ReplaceHolder(int start, int end, String replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }
    }

    private static String getAttr(String attrs, String attrName) {
        Matcher m = ATTRIBUTE_PATTERN.matcher(attrs);
        while (m.find()) {
            if (m.group(1).equals(attrName))
                return m.group(2);
        }
        return "";
    }

    private static boolean isWhitespace(String string) {
        for (int i = 0; i < string.length(); i++)
            if (!Character.isWhitespace(string.charAt(i)))
                return false;
        return true;
    }

    // Will return an empty string if there is no available translation
    private static String cleanMissingStrings(String xml, Resources resources) {
        // TODO Ignore "<!-- <string name="missing">value</string> -->" comments?
        // Maybe scan for comments and if a match is found inside a comment… Discard it?
        // Or maybe it's just over complication.
        //
        // TODO Instead a replacement range, perhaps I could just eat that part up…?
        // Well that's what I do anyway when writing the replacement.
        boolean haveAny = false;

        // 1. Find dirty tags (those which we have no translation for)
        Queue<DirtyRange> dirtyRanges = new LinkedList<>();

        Matcher mTag = RES_TAG_PATTERN.matcher(xml);
        while (mTag.find()) {
            String id = getAttr(mTag.group(2), ID);
            if (resources.contains(id)) {
                // This file contains at least one translation
                haveAny = true;
            } else {
                // We don't have a translation, so this tag is dirty. Decrease
                // the range by 1 not to eat up the next character (due to the i++)
                dirtyRanges.add(new DirtyRange(mTag.start(), mTag.end()-1));
            }
        }

        // There is no string we have a translation for, so return an empty string
        if (!haveAny)
            return "";

        // We might want to early terminate if we have a translation for all the strings
        if (dirtyRanges.isEmpty())
            return xml;

        // 2. Remove the dirty tags and mark those lines as dirty too
        int line = 0;
        int lastLine = -1; // To avoid adding the same line twice
        Queue<Integer> dirtyLines = new LinkedList<>();

        // Save result here
        StringBuilder noDirty = new StringBuilder();

        // Get first range and iterate over the characters of the xml
        DirtyRange range = dirtyRanges.poll();
        for (int i = 0; i < xml.length(); i++) {
            char c = xml.charAt(i);

            if (range == null || i < range.start) {
                // Copy this character since it's not part of the dirty tag
                noDirty.append(c);

                // Note how we increment the line iff it was copied,
                // otherwise it was removed and should be excluded
                if (c == '\n') {
                    line++;
                }
            } else {
                // Omit these characters since we're in a dirty tag,
                // and mark the line as dirty iff it wasn't marked before
                if (lastLine != line) {
                    dirtyLines.add(line);
                    lastLine = line;
                }

                // >= not to skip to the next character
                if (i >= range.end) {
                    // We're outside the range now, so pick up the next range
                    range = dirtyRanges.poll();
                }
            }
        }

        // Store our new xml
        xml = noDirty.toString();

        // 3. Clean the dirty lines iff they're whitespace only
        String[] lines = xml.split("\\n");
        Integer dirtyLine = dirtyLines.poll();

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            // If there are no more dirty lines
            // Or we are not at a dirty line yet
            // Or this line is not all whitespace, append it
            if (dirtyLine == null || i != dirtyLine || !isWhitespace(lines[i])) {
                result.append(lines[i]);
                result.append('\n');
            } else {
                // Get the next dirty line while ignoring this line too
                dirtyLine = dirtyLines.poll();
            }
        }

        // We can finally return the clean xml
        return result.toString();
    }

    // This assumes a clean xml, i.e., we have a translation for all the strings in it
    private static boolean writeReplaceStrings(String xml, Resources resources, OutputStream out) {
        // Match on the original xml to determine where replacements will be made
        Matcher mTag = RES_TAG_PATTERN.matcher(xml);

        // Matches will be in order, so use a queue (first in, first out)
        Queue<ReplaceHolder> holders = new LinkedList<>();
        while (mTag.find()) {
            int contentStart = mTag.start(3);
            int contentEnd = contentStart + mTag.group(3).length();

            String id = getAttr(mTag.group(2), ID);
            if (id.isEmpty())
                continue;

            // Should never fail - plus do we even care it's empty? No resource would be found
            // Also, we obviously have this string translated (xml was cleaned, right?),
            // thus there is no need to ensure whether we need to delete the line or not
            Matcher mItem;
            switch (mTag.group(1)) {
                case STRING:
                    holders.add(new ReplaceHolder(contentStart, contentEnd,
                            resources.getContent(id)));
                    break;
                case STRING_ARRAY:
                    ResStringArray array = ((ResStringArray.Item)resources.getTag(id)).getParent();
                    int i = 0;
                    mItem = RES_TAG_PATTERN.matcher(mTag.group(3));
                    while (mItem.find()) {
                        if (mItem.group(1).equals(ITEM)) {
                            // We must take the base offset (contentStart) into account…
                            int cs = contentStart + mItem.start(3);
                            int ce = cs + mItem.group(3).length();

                            // We might not have this content, but we wish to keep the order
                            ResStringArray.Item item = array.getItem(i);
                            String content = item == null ? "" : item.getContent();
                            holders.add(new ReplaceHolder(cs, ce, content));
                            i++;
                        }
                    }
                    break;
                case PLURALS:
                    ResPlurals plurals = ((ResPlurals.Item)resources.getTag(id)).getParent();
                    mItem = RES_TAG_PATTERN.matcher(mTag.group(3));
                    while (mItem.find()) {
                        if (mItem.group(1).equals(ITEM)) {
                            String quantity = getAttr(mItem.group(2), QUANTITY);
                            int cs = contentStart + mItem.start(3);
                            int ce = cs + mItem.group(3).length();

                            // We might not have this content, but we wish to keep the order
                            ResPlurals.Item item = plurals.getItem(quantity);
                            String content = item == null ? "" : item.getContent();
                            holders.add(new ReplaceHolder(cs, ce, content));
                        }
                    }
                    break;
                // case ITEM: break; // Should not be on the wild though
            }
        }

        // Write the result by applying the required replacements
        PrintWriter writer = new PrintWriter(out);
        ReplaceHolder holder = holders.poll();
        for (int i = 0; i < xml.length(); i++) {
            if (holder == null || i < holder.start) {
                // There are no more holders left, simply copy the characters
                writer.append(xml.charAt(i));
            } else {
                // We reached the current replacement holder
                // Replace the original content with our new content
                writeSanitize(writer, holder.replacement);
                i = holder.end - 1; // Skip to the end of the tag
                // Decrease the range by 1 not to eat up the next character (due to the i++)

                holder = holders.poll(); // Next holder
            }
        }
        writer.close();
        return writer.checkError();
    }

    public static boolean applyTemplate(File template, Resources resources, OutputStream out) {
        try {
            StringBuilder sb = new StringBuilder();
            FileInputStream in = new FileInputStream(template);

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null)
                sb.append(line).append('\n');

            // The xml will be empty if we have no translation for this file.
            String xml = cleanMissingStrings(sb.toString(), resources);
            return !xml.isEmpty() && writeReplaceStrings(xml, resources, out);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //endregion

    //endregion

    //endregion
}

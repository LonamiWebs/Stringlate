package io.github.lonamiwebs.stringlate.classes.resources;

import android.support.annotation.NonNull;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.gsantner.opoc.util.HelpersFiles;
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

    private final static boolean DEFAULT_TRANSLATABLE = true;
    private final static boolean DEFAULT_MODIFIED = false;
    private final static int DEFAULT_INDEX = -1;

    //endregion

    //region Xml -> Resources

    static void loadFromXml(final InputStream in, final Resources resources)
            throws XmlPullParserException, IOException {

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readResourcesInto(parser, resources);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void readResourcesInto(final XmlPullParser parser, final Resources resources)
            throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, RESOURCES);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;

            String name = parser.getName();
            switch (name) {
                case STRING:
                    ResTag rt = readResourceString(parser);
                    if (rt != null)
                        resources.loadTag(rt);
                    break;
                case STRING_ARRAY:
                    for (ResTag item : readResourceStringArray(parser))
                        resources.loadTag(item);
                    break;
                case PLURALS:
                    for (ResTag item : readResourcePlurals(parser))
                        resources.loadTag(item);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
    }

    // Reads a <string name="...">...</string> tag from the xml.
    // This assumes that the .xml has been cleaned (i.e. there are no untranslatable strings)
    private static ResString readResourceString(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        String id, content;
        boolean modified;

        parser.require(XmlPullParser.START_TAG, ns, STRING);

        id = parser.getAttributeValue(null, ID);

        // Metadata
        modified = readBooleanAttr(parser, MODIFIED, DEFAULT_MODIFIED);

        // The content must be read last, since it also consumes the tag
        content = ResTag.desanitizeContent(getInnerXml(parser));

        parser.require(XmlPullParser.END_TAG, ns, STRING);

        if (id == null || content.isEmpty())
            return null;
        else
            return new ResString(id, content, modified);
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

                    String content = ResTag.desanitizeContent(getInnerXml(parser));
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
                    String content = ResTag.desanitizeContent(getInnerXml(parser));
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
    // TODO This will fail with: &lt;a&gt;text</a>
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
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
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
                    parseString(serializer, (ResString) rs);
                } else if (rs instanceof ResStringArray.Item) {
                    ResStringArray parent = ((ResStringArray.Item) rs).getParent();
                    if (!doneParents.contains(parent.getId())) {
                        doneParents.add(parent.getId());
                        parseStringArray(serializer, parent);
                    }
                } else if (rs instanceof ResPlurals.Item) {
                    ResPlurals parent = ((ResPlurals.Item) rs).getParent();
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

        serializer.text(ResTag.sanitizeContent(string.getContent()));
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
            serializer.text(ResTag.sanitizeContent(item.getContent()));
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

            serializer.text(ResTag.sanitizeContent(item.getContent()));
            serializer.endTag(ns, ITEM);
        }

        serializer.endTag(ns, PLURALS);
    }

    //endregion

    //region Xml -> Xml without untranslatable strings

    private static final Pattern TAG_UNTRANSLATABLE_PATTERN =
            //                 tagName    translatable    =     "false"
            Pattern.compile("<([\\w-]+).*?translatable\\s*=\\s*\"false\".*?>");

    public static boolean cleanXml(final File inFile, final File outFile) {
        return cleanXml(HelpersFiles.readTextFile(inFile), outFile);
    }

    public static boolean cleanXml(final String xml, final File outFile) {
        try {
            boolean haveAny = false;

            // 1. Find dirty tags (those which are untranslatable)
            Queue<DirtyRange> dirtyRanges = new LinkedList<>();

            Matcher mTag = RES_TAG_PATTERN.matcher(xml);
            while (mTag.find()) {
                String translatable = getAttr(mTag.group(2), TRANSLATABLE);
                if (translatable.equals("false")) {
                    // Decrease the range by 1 not to eat up the next character (due to the i++)
                    dirtyRanges.add(new DirtyRange(mTag.start(), mTag.end() - 1));
                } else {
                    // This file contains at least one translatable string
                    haveAny = true;
                }
            }

            // TODO The rest of code is copied from cleanMissingStrings I only change one part…
            // There are no translatable strings, so do nothing (and don't create any file)
            if (!haveAny)
                return false;

            // There is at least one translatable string, so we need to clean the xml
            if (!outFile.getParentFile().isDirectory())
                outFile.getParentFile().mkdirs();

            FileOutputStream out = new FileOutputStream(outFile);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

            // We might want to early terminate if all strings are translatable
            if (dirtyRanges.isEmpty()) {
                // Simply copy the file, there's nothing to clean
                writer.write(xml);
                writer.close();
                return true;
            }

            removeDirtyRanges(xml, dirtyRanges, out);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //endregion

    //region Using another file as a template

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

    @NonNull
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
                dirtyRanges.add(new DirtyRange(mTag.start(), mTag.end() - 1));
            }
        }

        // There is no string we have a translation for, so return an empty string
        if (!haveAny)
            return "";

        // We might want to early terminate if we have a translation for all the strings
        if (dirtyRanges.isEmpty())
            return xml;

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // 2. Remove the dirty tags and mark those lines as dirty too
            removeDirtyRanges(xml, dirtyRanges, output);

            // Return the clean xml
            return output.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    // This assumes a clean xml, i.e., we have a translation for all the strings in it
    // Returns TRUE if there were no errors
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
                    ResStringArray array = ((ResStringArray.Item) resources.getTag(id)).getParent();
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
                    ResPlurals plurals = ((ResPlurals.Item) resources.getTag(id)).getParent();
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
                writer.append(ResTag.sanitizeContent(holder.replacement));
                i = holder.end - 1; // Skip to the end of the tag
                // Decrease the range by 1 not to eat up the next character (due to the i++)

                holder = holders.poll(); // Next holder
            }
        }
        writer.close();
        return !writer.checkError();
    }

    // Returns TRUE if the template was applied successfully
    public static boolean applyTemplate(File template, Resources resources, OutputStream out) {
        // The xml will be empty if we have no translation for this file.
        String xml = cleanMissingStrings(HelpersFiles.readTextFile(template), resources);
        return !xml.isEmpty() && writeReplaceStrings(xml, resources, out);
    }

    //endregion

    //endregion

    //endregion

    //region Private utilities

    // Removes the dirty ranges on string. If a line containing
    // a dirty range is then empty, this line will also be removed.
    // The result will be output to the given output stream.
    private static void removeDirtyRanges(final String string,
                                          final Queue<DirtyRange> dirtyRanges,
                                          final OutputStream output)
            throws IOException {
        int line = 0;
        int lastLine = -1; // To avoid adding the same line twice
        Queue<Integer> dirtyLines = new LinkedList<>();

        // Save result here
        StringBuilder noDirty = new StringBuilder();

        // Get first range and iterate over the characters of the xml
        DirtyRange range = dirtyRanges.poll();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

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

        // Clean the dirty lines iff they're whitespace only
        String[] lines = noDirty.toString().split("\\n");
        Integer dirtyLine = dirtyLines.poll();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
        for (int i = 0; i < lines.length; i++) {
            // If there are no more dirty lines
            // Or we are not at a dirty line yet
            // Or this line is not all whitespace, append it
            if (dirtyLine == null || i != dirtyLine || !isWhitespace(lines[i])) {
                writer.write(lines[i]);
                writer.write('\n');
            } else {
                // Get the next dirty line while ignoring this line too
                dirtyLine = dirtyLines.poll();
            }
        }
        writer.close();
    }

    //endregion
}

package io.github.lonamiwebs.stringlate.ResourcesStrings;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ResourcesParser {
    // We don't use namespaces
    private static final String ns = null;

    public Resources parse(InputStream in)
            throws XmlPullParserException, IOException {

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return new Resources(readResources(parser));
        } finally {
            in.close();
        }
    }

    private ArrayList<ResourcesString> readResources(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        ArrayList<ResourcesString> strings = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, "resources");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;

            String name = parser.getName();
            if (name.equals("string"))
                strings.add(readResourceString(parser));
            else
                skip(parser);
        }
        return strings;
    }

    private ResourcesString readResourceString(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        String id, content;
        boolean translatable = true;

        parser.require(XmlPullParser.START_TAG, ns, "string");

        id = parser.getAttributeValue(null, "name");
        if ("false".equals(parser.getAttributeValue(null, "translatable")))
            translatable = false;
        content = readText(parser);

        parser.require(XmlPullParser.END_TAG, ns, "string");

        return new ResourcesString(id, content, translatable);
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
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
}

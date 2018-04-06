package io.github.lonamiwebs.stringlate.classes.resources.tags;

public enum ResType {
    STRING,
    STRING_ARRAY,
    PLURALS,
    ITEM,
    UNKNOWN;

    private static final String STRING_VALUE = "string";
    private static final String STRING_ARRAY_VALUE = "string-array";
    private static final String PLURALS_VALUE = "plurals";
    private static final String ITEM_VALUE = "item";

    public static ResType fromTagName(final String name) {
        switch (name) {
            case STRING_VALUE:
                return STRING;
            case STRING_ARRAY_VALUE:
                return STRING_ARRAY;
            case PLURALS_VALUE:
                return PLURALS;
            case ITEM_VALUE:
                return ITEM;
            default:
                return UNKNOWN;
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case STRING:
                return STRING_VALUE;
            case STRING_ARRAY:
                return STRING_ARRAY_VALUE;
            case PLURALS:
                return PLURALS_VALUE;
            case ITEM:
                return ITEM_VALUE;
            default:
                return "unknown";
        }
    }

    // Marks the ID so that it can't be confused with others based on a type.
    // See issue #184, where a "<string>" and a "<string-array>" have the same name.
    //
    // This identifier is only used internally by the application.
    // The '#' character is currently used for types while ':' for indices.
    public String markID(String id) {
        switch (this) {
            case STRING_ARRAY:
                return id + "#a";
            case PLURALS:
                return id + "#p";
            default:
                return id;
        }
    }

    // Inverse operation to markID.
    public static String resolveID(String id) {
        int index = id.indexOf('#');
        return index == -1 ? id : id.substring(0, index);
    }
}

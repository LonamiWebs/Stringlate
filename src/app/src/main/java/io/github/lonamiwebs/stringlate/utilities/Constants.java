package io.github.lonamiwebs.stringlate.utilities;

public class Constants {
    public final static String EXTRA_REPO = "io.github.lonamiwebs.stringlate.REPO";
    public final static String EXTRA_LOCALE = "io.github.lonamiwebs.stringlate.LOCALE";
    public final static String EXTRA_ID = "io.github.lonamiwebs.stringlate.ID";

    // Arbitrary numbers were chosen
    public final static int RESULT_REPO_DISCOVERED = 707;
    public final static int RESULT_CREATE_FILE = 708;
    public final static int RESULT_STRING_SELECTED = 709;
    public final static int RESULT_OPEN_TREE = 710;
    public final static int RESULT_OPEN_FILE = 711;

    // Online help
    public final static String ONLINE_HELP_DEFAULT_LOCALE = "en";
    public final static String[] ONLINE_HELP_LOCALES = {"en", "es"};
    public final static String ONLINE_HELP_INDEX =
            "https://github.com/LonamiWebs/Stringlate/blob/master/help/index.md";

    // Material colors (700): https://material.io/guidelines/style/color.html
    public final static int[] MATERIAL_COLORS = {
            0xFFD32F2F, 0xFFC2185B, 0xFF7B1FA2, 0xFF512DA8, 0xFF303F9F, 0xFF1976D2,
            0xFF0288D1, 0xFF0097A7, 0xFF00796B, 0xFF388E3C, 0xFF689F38, 0xFFAFB42B,
            0xFFFBC02D, 0xFFFFA000, 0xFFF57C00, 0xFFE64A19, 0xFF5D4037, 0xFF616161
    };
}

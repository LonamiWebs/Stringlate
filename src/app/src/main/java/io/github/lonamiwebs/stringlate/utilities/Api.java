package io.github.lonamiwebs.stringlate.utilities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

// Stringlate API
// https://github.com/LonamiWebs/Stringlate/blob/master/src/app/src/main/java/io/github/lonamiwebs/stringlate/utilities/Api.java
//
// External applications are free to copy and redistribute this
// file on their projects in order to make a fair use of it.
//
// Otherwise, they are free to use this file as reference and
// implement their own solutions.
public class Api {

    //region Available actions

    // ACTION_TRANSLATE:
    //   Opens Stringlate with one of the following three behaviours:
    //
    //   * If EXTRA_GIT_URL is not given, Stringlate will finish() with
    //     no further action being taken.
    //
    //   * If EXTRA_GIT_URL is given, despite being valid or not:
    //     a) If this repository has already been added by the user before,
    //        the `Translate` activity will be opened automatically.
    //
    //     b) If this repository has not yet been added, the `Add repository`
    //        fragment will be shown, with the given URL automatically entered.
    public static final String ACTION_TRANSLATE = "io.github.lonamiwebs.stringlate.TRANSLATE";

    //endregion

    //region Extras

    // EXTRA_GIT_URL
    //   Used to pass a String representing the git URL pointing to the
    //   source code of the application that should be opened for translation.
    public static final String EXTRA_GIT_URL = "GIT_URL";

    //endregion

    //region Mime type

    // You *must* manually define an intent's `mime-type` when using a
    // custom action with data (http://stackoverflow.com/a/12315898/4759433).
    //
    // Since there is no uri implied (only custom extras) it is recommended
    // to use "text/plain", but any mime type (existing or not) can be used.
    public static final String MIME_TYPE = "text/plain";

    //endregion

    //region Public methods

    // ACTION_TRANSLATE wrapper.
    //
    // Will throw ActivityNotFoundException if Stringlate is not installed on the device.
    public void translate(final Context context, final String gitUrl)
            throws ActivityNotFoundException {
        Intent intent = new Intent(ACTION_TRANSLATE);
        intent.setType(MIME_TYPE);
        intent.putExtra(EXTRA_GIT_URL, gitUrl);
        context.startActivity(intent);
    }

    //endregion
}

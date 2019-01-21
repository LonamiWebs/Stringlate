/*#######################################################
 *
 *   Maintained by Gregor Santner, 2016-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package io.github.lonamiwebs.stringlate.utilities;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.widget.Toast;

public class ContextUtils extends net.gsantner.opoc.util.ContextUtils {
    public ContextUtils(Context context) {
        super(context);
    }

    public boolean isOffline(@Nullable @StringRes Integer warnMessageStringRes) {
        final boolean result = isConnectedToInternet();
        if (!result && warnMessageStringRes != null)
            Toast.makeText(_context, _context.getString(warnMessageStringRes), Toast.LENGTH_SHORT).show();

        return !result;
    }
}

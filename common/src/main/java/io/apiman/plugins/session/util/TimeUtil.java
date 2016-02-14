package io.apiman.plugins.session.util;

import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Utility methods for handling times/dates.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class TimeUtil {
    public static long getNowInMillis() {
        return GregorianCalendar.getInstance(Locale.getDefault()).getTimeInMillis();
    }

    public static boolean isAfterNow(long millis) {
        return (millis > getNowInMillis());
    }
}

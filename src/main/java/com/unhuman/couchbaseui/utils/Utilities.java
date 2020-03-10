package com.unhuman.couchbaseui.utils;

/** Helper methods
 *  Similar to some of these might be available elsewhere, but are here to limit external dependencies
 */

public class Utilities {
    public static boolean stringIsNullOrEmpty(String check) {
        if ((check == null) || (check.equals(""))) {
            return true;
        }

        return false;
    }

    public static String trimString(String value) {
        return (value != null) ? value.trim() : null;
    }
}

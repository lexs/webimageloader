package com.webimageloader.util;

import java.net.URLConnection;

public class HeaderParser {
    private static final String MAX_AGE = "max-age";

    public static long getMaxAge(URLConnection urlConnection) {
        String cacheControl = urlConnection.getHeaderField("Cache-Control");
        if (cacheControl == null) {
            return -1;
        }

        int pos = cacheControl.indexOf(MAX_AGE);
        if (pos == -1) {
            return -1;
        }

        pos += MAX_AGE.length();
        pos = skipWhitespace(cacheControl, pos);

        // Consume '='
        pos++;

        pos = skipWhitespace(cacheControl, pos);

        int start = pos;
        pos = skipUntil(cacheControl, pos, ",");
        String value = cacheControl.substring(start, pos).trim();

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Returns the next index in {@code input} at or after {@code pos} that
     * contains a character from {@code characters}. Returns the input length if
     * none of the requested characters can be found.
     */
    private static int skipUntil(String input, int pos, String characters) {
        for (; pos < input.length(); pos++) {
            if (characters.indexOf(input.charAt(pos)) != -1) {
                break;
            }
        }
        return pos;
    }

    /**
     * Returns the next non-whitespace character in {@code input} that is white
     * space. Result is undefined if input contains newline characters.
     */
    private static int skipWhitespace(String input, int pos) {
        for (; pos < input.length(); pos++) {
            char c = input.charAt(pos);
            if (c != ' ' && c != '\t') {
                break;
            }
        }
        return pos;
    }

    private HeaderParser() {}
}

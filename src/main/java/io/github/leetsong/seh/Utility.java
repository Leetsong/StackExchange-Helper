package io.github.leetsong.seh;

public class Utility {

    public static String timeInterval(long from, long end) {
        long diff = end - from;
        // less than 1s
        if (diff < 1_000) {
            return String.format("%dms", diff);
        }
        // less than 1min
        else if (diff < 60_000) {
            return String.format("%fs", (double)diff / 1_000);
        }
        // less than 1h
        else if (diff < 3600_000) {
            return String.format("%dmin %fs", diff / 60_000, (double)(diff % 60_000) / 1000);
        }
        // more than 1h
        else {
            return String.format("%fh", (double)diff / 3600_000);
        }
    }

    public static String camelToUnderline(String camel) {
        if (null == camel) { return null; }

        char          c;
        int           l = camel.length();
        StringBuilder s = new StringBuilder(2 * l);

        for (int i = 0; i < l; i ++) {
            c = camel.charAt(i);
            if ('A' <= c &&  c <= 'Z') {
                if (i != 0 && Character.isAlphabetic(camel.charAt(i - 1))) { s.append('_'); }
                s.append(Character.toLowerCase(c));
            } else {
                s.append(c);
            }
        }

        return s.toString();
    }

    public static String underlineToCamel(String underline) {
        if (null == underline) { return null; }

        char          c;
        int           l = underline.length();
        StringBuilder s = new StringBuilder(l);

        int i = 0;
        while (i < l) {
            c = underline.charAt(i);

            if ('_' == c) {
                while ('_' == c) {
                    i += 1; c = underline.charAt(i);
                }
                s.append(Character.toUpperCase(c));
            } else {
                s.append(c);
            }

            i += 1;
        }

        return s.toString();
    }
}

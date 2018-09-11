package com.example.stackoverflow.fetcher;

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
}

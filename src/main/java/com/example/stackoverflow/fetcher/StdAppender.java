package com.example.stackoverflow.fetcher;

public class StdAppender extends AbstractAppender {

    // type of this appender
    public static final String APPENDER_TYPE = "std";

    public static Appender newInstance(String path) {
        return new StdAppender(path);
    }

    @Override
    public void append(SearchResult result) {
        System.out.println(String.format("||std::%s||: %s", mPath, result.toString()));
    }

    @Override
    public void close() {}

    private StdAppender(String path) {
        super(path);
    }
}

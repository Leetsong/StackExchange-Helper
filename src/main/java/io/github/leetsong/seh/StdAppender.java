package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.CsvItemable;

import java.util.List;

public class StdAppender extends AbstractAppender<CsvItemable> {

    // type of this appender
    public static final String APPENDER_TYPE = "std";

    public static StdAppender newInstance(String path) {
        return new StdAppender(path);
    }

    @Override
    public void append(List<CsvItemable> items) {
        System.out.println(String.format("||std::%s||: %s", mPath, items.toString()));
    }

    @Override
    public void close() {}

    private StdAppender(String path) {
        super(path);
    }
}

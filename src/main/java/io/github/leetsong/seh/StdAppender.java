package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.ItemContainer;
import io.github.leetsong.seh.data.stackexchange.SearchItem;

public class StdAppender extends AbstractAppender {

    // type of this appender
    public static final String APPENDER_TYPE = "std";

    public static StdAppender newInstance(String path) {
        return new StdAppender(path);
    }

    @Override
    public void append(ItemContainer<SearchItem> result) {
        System.out.println(String.format("||std::%s||: %s", mPath, result.toString()));
    }

    @Override
    public void close() {}

    private StdAppender(String path) {
        super(path);
    }
}

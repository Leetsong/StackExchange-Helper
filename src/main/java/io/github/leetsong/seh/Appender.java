package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.ItemContainer;
import io.github.leetsong.seh.data.stackexchange.SearchItem;

/**
 * Appender is a class that handle ItemContainer<SearchItem>,
 * each Appender type has to:
 *  1. have a public static field called APPENDER_TYPE, which indicate its type
 *  2. have a public static method typed `Appender newInstance(String)' as a constructor
 *  3. have a field called path to indicate the path of this appender
 *  4. implement the interface methods, i.e., `append' to handle the ItemContainer<SearchItem>
 */
public interface Appender {

    String NEW_INSTANCE_METHOD_NAME = "newInstance";

    /**
     * append handles the ItemContainer<SearchItem>
     * @param result
     */
    void append(ItemContainer<SearchItem> result);

    /**
     * close closes this appender
     */
    void close();
}

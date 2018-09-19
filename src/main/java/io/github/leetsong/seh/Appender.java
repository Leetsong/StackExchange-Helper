package io.github.leetsong.seh;

import io.github.leetsong.seh.data.stackexchange.AppendableItem;

import java.util.List;

/**
 * Appender is a class that handle List<\? extends AppendableItem>
 * each Appender type has to:
 *  1. have a public static field called APPENDER_TYPE, which indicate its type
 *  2. have a public static method typed `Appender newInstance(String)' as a constructor
 *  3. have a field called path to indicate the path of this appender
 *  4. implement the interface methods, i.e., `append' to handle the List<\? extends AppendableItem>
 */
public interface Appender<T extends AppendableItem> {

    String NEW_INSTANCE_METHOD_NAME = "newInstance";

    /**
     * append handles the List<\? extends AppendableItem>
     * @param items
     */
    void append(List<T> items);

    /**
     * close closes this appender
     */
    void close();
}

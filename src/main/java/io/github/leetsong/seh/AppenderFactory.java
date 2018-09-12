package io.github.leetsong.seh;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class AppenderFactory {

    // appender map
    private static final
    Map<String, Class<? extends Appender>> mAppenderMap = new HashMap<>();

    public static void
    registerAppender(String type, Class<? extends Appender> appenderType) {
        mAppenderMap.put(type, appenderType);
    }

    public static void
    unregisterAppender(String type) {
        mAppenderMap.remove(type);
    }

    public static void initialize() {
        registerAppender(StdAppender.APPENDER_TYPE, StdAppender.class);
        registerAppender(CsvAppender.APPENDER_TYPE, CsvAppender.class);
    }

    public static Class<? extends Appender> getAppenderType(String type) {
        Class<? extends Appender> appenderType = mAppenderMap.get(type);
        return (appenderType == null) ? StdAppender.class : appenderType;
    }

    public static Appender
    getAppender(String type, String appenderPath) {
        Class<? extends Appender> appenderType = getAppenderType(type);
        Appender appender;

        try {
            appender = (Appender) appenderType.getMethod(Appender.NEW_INSTANCE_METHOD_NAME, String.class)
                    .invoke(appenderType, appenderPath);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }

        return appender;
    }
}

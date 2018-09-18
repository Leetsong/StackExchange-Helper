package io.github.leetsong.seh;

import org.apache.commons.cli.*;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

public class ConfigCmd extends CLI.Cmd
        implements CLI.OnSetUpListener, CLI.OnTearDownListener {

    private static class CLI_OPTIONS {
        static final String  OPT_SET_SHORT = "s";
        static final String  OPT_SET_LONG = "set";
        static final boolean OPT_SET_HAS_ARGS = true;
        static final String  OPT_SET_DESCRIPTION = "set configs, separated by ';'";

        static final String  OPT_GET_SHORT = "g";
        static final String  OPT_GET_LONG = "get";
        static final boolean OPT_GET_HAS_ARGS = true;
        static final String  OPT_GET_DESCRIPTION = "get configs, separated by ';'";

        static final String  OPT_LIST_SHORT = "l";
        static final String  OPT_LIST_LONG = "list";
        static final boolean OPT_LIST_HAS_ARGS = false;
        static final String  OPT_LIST_DESCRIPTION = "list all configs";
    }

    public static final String COMMAND = "config";
    public static final String SEHCONFIG_FILE_PATH =
            String.format("%s%s.sehconfig", System.getProperty("user.home"), File.separator);
    public static final Set<String> ALL_PROPERTIES = new HashSet<>();

    // put this block after the static field ALL_PROPERTIES
    static {
        // private declared property definition functions are in
        // the form of `property$ParentProperty_ChildProperty',
        // such as `property$Socks5_Url' indicates `socks5.url'
        Method[] methods = ConfigCmd.class.getDeclaredMethods();
        for (int i = 0; i < methods.length; i ++) {
            String methodName = methods[i].getName();
            if (methodName.startsWith("property$")) {
                String methodProperty = methodName.substring("property$".length());
                String property = Utility.camelToUnderline(
                        methodProperty.replace("_", ".")).toLowerCase();
                ALL_PROPERTIES.add(property);
            }
        }
    }

    private Properties mProperties = new Properties();
    private boolean mIsDirty = false;

    public ConfigCmd(CLI cli) {
        super(cli);
        cli.setOnSetUpListener(this);
        cli.setOnTearDownListener(this);
    }

    @Override
    public void execute(String[] opts) {
        try {
            CommandLineParser cliParser = new DefaultParser();
            CommandLine cli = cliParser.parse(setUpOptions(), opts);

            // priority: --list > --get > --set
            if (cli.hasOption(CLI_OPTIONS.OPT_LIST_SHORT)) {
                listProperties();
            } else if (cli.hasOption(CLI_OPTIONS.OPT_GET_SHORT)) {
                getProperties(cli.getOptionValue(CLI_OPTIONS.OPT_GET_SHORT)
                        .split(";"));
            } else if (cli.hasOption(CLI_OPTIONS.OPT_SET_SHORT)) {
                setProperties(cli.getOptionValue(CLI_OPTIONS.OPT_SET_SHORT)
                        .split(";"));
            } else {
                mCli.stderr("Missing required argument");
                help();
                exit(1);
            }
        } catch (ParseException e) {
            mCli.stderr(e.getMessage());
            help();
            exit(1);
        }
    }

    @Override
    public void help() {
        mCli.stdout("seh config --list|-l");
        mCli.stdout("seh config --set|-s <key>=<value>[;<key>=<value>;...]");
        mCli.stdout("seh config --get|-g <key>[;<key>;...]");
        mCli.stdout("<key>");
        mCli.stdout("  " + property$Socks5_Url() + "\turl of socks5 proxy");
        mCli.stdout("  " + property$Socks5_Port() + "\tport of socks5 port");
        mCli.stdout("<value>");
        mCli.stdout("  value to be set for this key");
    }

    @Override
    public void onSetUp() {
        load();
    }

    @Override
    public void onTearDown() {
        store();
    }

    public void load() {
        try {
            File file = new File(SEHCONFIG_FILE_PATH);
            if (file.exists()) {
                mProperties.load(new FileReader(file));

                // system settings
                String socks5Url = mProperties.getProperty(property$Socks5_Url());
                String socks5Port = mProperties.getProperty(property$Socks5_Port());
                if (socks5Url != null) {
                    System.setProperty("socksProxyHost", socks5Url);
                }
                if (socks5Port != null) {
                    System.setProperty("socksProxyPort", socks5Port);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            mCli.exit(1);
        }
    }

    public void store() {
        try {
            if (!mProperties.isEmpty()) {
                mProperties.store(new FileWriter(SEHCONFIG_FILE_PATH), null);
            }
        } catch (IOException e) {
            e.printStackTrace();
            mCli.exit(1);
        }
    }

    public static boolean hasProperty(String property) {
        return ALL_PROPERTIES.contains(property);
    }

    private Options setUpOptions() {
        Options options = new Options();
        options.addOption(
                CLI_OPTIONS.OPT_LIST_SHORT,
                CLI_OPTIONS.OPT_LIST_LONG,
                CLI_OPTIONS.OPT_LIST_HAS_ARGS,
                CLI_OPTIONS.OPT_LIST_DESCRIPTION);
        options.addOption(
                CLI_OPTIONS.OPT_GET_SHORT,
                CLI_OPTIONS.OPT_GET_LONG,
                CLI_OPTIONS.OPT_GET_HAS_ARGS,
                CLI_OPTIONS.OPT_GET_DESCRIPTION);
        options.addOption(
                CLI_OPTIONS.OPT_SET_SHORT,
                CLI_OPTIONS.OPT_SET_LONG,
                CLI_OPTIONS.OPT_SET_HAS_ARGS,
                CLI_OPTIONS.OPT_SET_DESCRIPTION);
        return options;
    }

    private void listProperties() {
        getProperties(ALL_PROPERTIES.toArray(new String[0]));
    }

    private void getProperties(String[] props) {
        List<String> undefinedProps = new ArrayList<>();
        for (String prop : props) {
            if (hasProperty(prop)) {
                String value = mProperties.getProperty(prop);
                // maybe this value has not been set
                if (value != null) {
                    mCli.stdout(String.format("%s=%s", prop, value));
                } else {
                    mCli.stdout(String.format("%s=%s", prop, "<not set>"));
                }
            } else {
                undefinedProps.add(prop);
            }
        }
        if (!undefinedProps.isEmpty()) {
            mCli.stdwarn(String.format("Properties { %s}, are not defined",
                    String.join(", ", undefinedProps)));
        }
    }

    private void setProperties(String[] keyValuePairs) {
        List<String> undefinedProps = new ArrayList<>();
        for (String keyValuePair : keyValuePairs) {
            String[] pair = keyValuePair.split("=");
            if (hasProperty(pair[0])) {
                String oldValue = mProperties.getProperty(pair[0]);
                if (!pair[1].equals(oldValue)) {
                    mProperties.setProperty(pair[0], pair[1]);
                    mIsDirty = true;
                }
            } else {
                undefinedProps.add(pair[0]);
            }
        }
        if (!undefinedProps.isEmpty()) {
            mCli.stdwarn(String.format("Properties { %s}, are not defined",
                    String.join(", ", undefinedProps)));
        }
    }

    private String property$Socks5_Url() {
        return "socks5.url";
    }

    private String property$Socks5_Port() {
        return "socks5.port";
    }
}

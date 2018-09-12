package io.github.leetsong.seh;

import org.apache.commons.cli.*;

public class FetcherCmd extends CLI.Cmd {

    private static class CLI_OPTIONS {
        static final String  OPT_TAGS_SHORT = "t";
        static final String  OPT_TAGS_LONG = "tags";
        static final boolean OPT_TAGS_HAS_ARGS = true;
        static final String  OPT_TAGS_DESCRIPTION = "tags to search";
    }

    public static final String COMMAND = "fetch";

    public FetcherCmd(CLI cli) {
        super(cli);
    }

    @Override
    public void execute(String[] opts) {
        try {
            CommandLineParser cliParser = new DefaultParser();
            CommandLine cli = cliParser.parse(setUpOptions(), opts);

            if (cli.hasOption(CLI_OPTIONS.OPT_TAGS_SHORT)) {
                String tags = cli.getOptionValue(CLI_OPTIONS.OPT_TAGS_SHORT);
                new Fetcher(tags.split(";")).fetch();
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
        // TODO
        mCli.stdout("seh fetch --tag|-t <tags>");
        mCli.stdout("<tags> tags you want to search, separated by ';'");
        mCli.stdout("       e.g., android;android-webview;webview");
    }

    private Options setUpOptions() {
        Options options = new Options();
        options.addOption(
                CLI_OPTIONS.OPT_TAGS_SHORT,
                CLI_OPTIONS.OPT_TAGS_LONG,
                CLI_OPTIONS.OPT_TAGS_HAS_ARGS,
                CLI_OPTIONS.OPT_TAGS_DESCRIPTION);
        return options;
    }
}

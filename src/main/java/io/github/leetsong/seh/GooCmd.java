package io.github.leetsong.seh;

import org.apache.commons.cli.*;

public class GooCmd extends CLI.Cmd {

    private static class CLI_OPTIONS {
        static final String  OPT_QUERY_SHORT = "q";
        static final String  OPT_QUERY_LONG = "query";
        static final boolean OPT_QUERY_HAS_ARGS = true;
        static final String  OPT_QUERY_DESCRIPTION = "query to search";

        static final String  OPT_TOTAL_SHORT = "t";
        static final String  OPT_TOTAL_LONG = "total";
        static final boolean OPT_TOTAL_HAS_ARGS = true;
        static final String  OPT_TOTAL_DESCRIPTION = "total number of results";
    }

    public static final String COMMAND = "goo";

    public GooCmd(CLI cli) {
        super(cli);
    }

    @Override
    public void execute(String[] opts) {
        try {
            CommandLineParser cliParser = new DefaultParser();
            CommandLine cli = cliParser.parse(setUpOptions(), opts);

            if (cli.hasOption(CLI_OPTIONS.OPT_QUERY_SHORT) &&
                    cli.hasOption(CLI_OPTIONS.OPT_TOTAL_SHORT)) {
                String query = cli.getOptionValue(CLI_OPTIONS.OPT_QUERY_SHORT);
                int total = Integer.parseInt(cli.getOptionValue(CLI_OPTIONS.OPT_TOTAL_SHORT));
                new GooFetcher(query, total).fetch();
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
        mCli.stdout("seh goo --query|-q <query> --total|-t <total_number>");
        mCli.stdout("<query>        query you want to search");
        mCli.stdout("<total_number> total number of results");
    }

    private Options setUpOptions() {
        Options options = new Options();
        options.addOption(
                CLI_OPTIONS.OPT_QUERY_SHORT,
                CLI_OPTIONS.OPT_QUERY_LONG,
                CLI_OPTIONS.OPT_QUERY_HAS_ARGS,
                CLI_OPTIONS.OPT_QUERY_DESCRIPTION);
        options.addOption(
                CLI_OPTIONS.OPT_TOTAL_SHORT,
                CLI_OPTIONS.OPT_TOTAL_LONG,
                CLI_OPTIONS.OPT_TOTAL_HAS_ARGS,
                CLI_OPTIONS.OPT_TOTAL_DESCRIPTION);
        return options;
    }
}

package io.github.leetsong.seh;

import org.apache.commons.cli.*;

import java.util.Arrays;

public class CombinerCmd extends CLI.Cmd {

    private static class CLI_OPTIONS {
        static final String  OPT_SRC_SHORT = "s";
        static final String  OPT_SRC_LONG = "src";
        static final boolean OPT_SRC_HAS_ARGS = true;
        static final String  OPT_SRC_DESCRIPTION = "source files";

        static final String  OPT_DEST_SHORT = "d";
        static final String  OPT_DEST_LONG = "dest";
        static final boolean OPT_DEST_HAS_ARGS = true;
        static final String  OPT_DEST_DESCRIPTION = "destination file";
    }

    public static final String COMMAND = "combine";

    public CombinerCmd(CLI cli) {
        super(cli);
    }

    @Override
    public void execute(String[] opts) {
        try {
            CommandLineParser cliParser = new DefaultParser();
            CommandLine cli = cliParser.parse(setUpOptions(), opts);

            if (cli.hasOption(CLI_OPTIONS.OPT_SRC_SHORT) &&
                cli.hasOption(CLI_OPTIONS.OPT_DEST_SHORT)) {
                String srcFiles = cli.getOptionValue(CLI_OPTIONS.OPT_SRC_SHORT);
                String destFile = cli.getOptionValue(CLI_OPTIONS.OPT_DEST_SHORT);
                new CsvCombiner(Arrays.asList(srcFiles.split(";")),
                        destFile).combine();
            } else {
                mCli.stderr("Missing required arguments");
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
        mCli.stdout("seh combine --src|-s <src> --dest|-d <dest>");
        mCli.stdout("<src>  files you want to combine from, separated by ';'");
        mCli.stdout("       e.g., a.csv;b.csv;c.csv");
        mCli.stdout("<dest> file you want to combine to");
    }

    private Options setUpOptions() {
        Options options = new Options();
        options.addOption(
                CLI_OPTIONS.OPT_SRC_SHORT,
                CLI_OPTIONS.OPT_SRC_LONG,
                CLI_OPTIONS.OPT_SRC_HAS_ARGS,
                CLI_OPTIONS.OPT_SRC_DESCRIPTION);
        options.addOption(
                CLI_OPTIONS.OPT_DEST_SHORT,
                CLI_OPTIONS.OPT_DEST_LONG,
                CLI_OPTIONS.OPT_DEST_HAS_ARGS,
                CLI_OPTIONS.OPT_DEST_DESCRIPTION);
        return options;
    }
}

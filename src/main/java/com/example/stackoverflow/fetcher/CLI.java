package com.example.stackoverflow.fetcher;

public class CLI {

    public static abstract class Cmd {

        // the CLI
        protected CLI mCli;

        public Cmd(CLI cli) {
            this.mCli = cli;
        }

        /**
         * execute executes this command with args
         * @param args arguments of this command
         */
        public abstract void execute(String[] args);

        /**
         * exit terminates the program with code status
         * @param status exit status
         */
        public void exit(int status) {
            mCli.exit(status);
        }

        /**
         * help shows the help page
         */
        public abstract void help();
    }

    private String   mCommand;
    private String[] mCommandArgs;

    public CLI(String[] commands) {
        parseCommands(commands);
    }

    public void run() {
        if (mCommand.equals("help")) {
            if (mCommandArgs != null) {
                help(mCommandArgs[0]);
            } else {
                help();
            }
            exit(0);
        } else if (mCommand.equals("version")) {
            version();
            exit(0);
        }

        switch (mCommand) {
            case FetcherCmd.COMMAND:
                new FetcherCmd(this).execute(mCommandArgs);
                break;
            case CombinerCmd.COMMAND:
                new CombinerCmd(this).execute(mCommandArgs);
                break;
            default:
                stderr("'" + mCommand + "' is not defined, " +
                        "see 'StackOverflow-Fetcher help'");
                exit(0);
        }
    }

    public void exit(int status) {
        System.exit(status);
    }

    public void stdout(String message) {
        System.out.println(message);
    }

    public void stderr(String message) {
        System.err.println("StackOverflow-Fetcher: " + message);
    }

    private void parseCommands(String[] commands) {
        if (commands.length < 1) {
            stderr("No command is designated");
            help();
            exit(0);
        } else {
            mCommand = commands[0];
            if (commands.length != 1) {
                mCommandArgs = new String[commands.length - 1];
                System.arraycopy(commands, 1,
                        mCommandArgs, 0,
                        mCommandArgs.length);
            }
        }
    }

    private void version() {
        // TODO
        stdout("StackOverflow-Fetcher 1.0");
    }

    private void help() {
        // TODO
        stdout("usage: StackOverflow-Fetcher command [options]");
        stdout("[options]:");
        stdout("  fetch    fetch interested queries");
        stdout("  combine  combine fetched csv results");
        stdout("  version  show version");
        stdout("  help     show this");
    }

    private void help(String cmd) {
        switch (cmd) {
            case FetcherCmd.COMMAND:
                new FetcherCmd(this).help();
                break;
            case CombinerCmd.COMMAND:
                new CombinerCmd(this).help();
                break;
            default:
                stderr("'" + cmd + "' is not defined, " +
                        "see 'StackOverflow-Fetcher help'");
                exit(0);
        }
    }

    public static void main(String[] args) {
        new CLI(args).run();
    }
}

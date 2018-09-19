package io.github.leetsong.seh;

import java.util.ArrayList;
import java.util.List;

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

    public interface OnSetUpListener {
        void onSetUp();
    }

    public interface OnTearDownListener {
        void onTearDown();
    }

    // put these two listeners ahead, they have to be loaded ahead of those
    // using them
    private List<OnSetUpListener>    mSetUpListeners = new ArrayList<>();
    private List<OnTearDownListener> mTearDownListeners = new ArrayList<>();

    private String    mCommand;
    private String[]  mCommandArgs;
    private ConfigCmd mConfigCmd = new ConfigCmd(this);

    public CLI(String[] commands) {
        parseCommands(commands);
    }

    public void run() {
        setUp();

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
            case FetchCmd.COMMAND:
                new FetchCmd(this).execute(mCommandArgs);
                break;
            case CombinerCmd.COMMAND:
                new CombinerCmd(this).execute(mCommandArgs);
                break;
            case ConfigCmd.COMMAND:
                mConfigCmd.execute(mCommandArgs);
                break;
            case GooCmd.COMMAND:
                new GooCmd(this).execute(mCommandArgs);
                break;
            default:
                stderr("'" + mCommand + "' is not defined, " +
                        "see 'seh help'");
                exit(0);
        }

        tearDown();
    }

    public void exit(int status) {
        System.exit(status);
    }

    public void setOnSetUpListener(OnSetUpListener l) {
        mSetUpListeners.add(l);
    }

    public void setOnTearDownListener(OnTearDownListener l) {
        mTearDownListeners.add(l);
    }

    public void stdout(String message) {
        System.out.println(message);
    }

    public void stdwarn(String message) {
        System.out.print("seh: warning: " + message);
    }

    public void stderr(String message) {
        System.err.println("seh: error: " + message);
    }

    private void setUp() {
        for (OnSetUpListener l : mSetUpListeners) {
            l.onSetUp();
        }
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
        stdout("StackExchange-Helper (SEH) 1.0");
    }

    private void help() {
        // TODO
        stdout("usage: seh <command> [options]");
        stdout("<command>:");
        stdout("  fetch    fetch interested queries using StackExchange API");
        stdout("  goo      fetch interested queries using Google Search");
        stdout("  combine  combine fetched csv results");
        stdout("  config   change seh configs");
        stdout("  version  show version");
        stdout("  help     show this");
    }

    private void help(String cmd) {
        switch (cmd) {
            case FetchCmd.COMMAND:
                new FetchCmd(this).help();
                break;
            case CombinerCmd.COMMAND:
                new CombinerCmd(this).help();
                break;
            case ConfigCmd.COMMAND:
                mConfigCmd.help();
                break;
            case GooCmd.COMMAND:
                new GooCmd(this).help();
                break;
            default:
                stderr("'" + cmd + "' is not defined, " +
                        "see 'seh help'");
                exit(0);
        }
    }

    private void tearDown() {
        for (OnTearDownListener l : mTearDownListeners) {
            l.onTearDown();
        }
    }

    public static void main(String[] args) {
        new CLI(args).run();
    }
}

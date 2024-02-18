package android.os;

import java.io.PrintWriter;

public abstract class ShellCommand {

    /**
     * Return a PrintWriter for formatting output to {@link #getRawOutputStream()}.
     */
    public abstract PrintWriter getOutPrintWriter();
    public abstract PrintWriter getErrPrintWriter();

    public abstract String[] getAllArgs();

    /**
     * Return the next argument on the command line, whatever it is; if there are
     * no arguments left, throws an IllegalArgumentException to report this to the user.
     */
    public abstract String getNextArgRequired();


    /**
     * Returns number of arguments that haven't been processed yet.
     */
    public abstract int getRemainingArgsCount();

    /**
     * @return all the remaining arguments in the command without moving the current position.
     */
    public abstract String[] peekRemainingArgs();

}

package edu.rit.swen755.threadpool;

import java.io.PrintStream;

/**
 * Command-line entry point for the thread-pooling prime counter.
 *
 * <p>The program finds and counts the primes in {@code [1, N]} by splitting the range into
 * {@link #CHUNK_COUNT} chunks and running them through one of several execution strategies. The
 * pool size and chunk count are fixed constants, not options.
 *
 * <p>The final command-line contract (implemented in slice 4) is:
 * <ul>
 *   <li>{@code --n <N>}: the upper bound of the range. Default {@value #DEFAULT_N}. Must be at
 *       least {@link #CHUNK_COUNT}; a smaller value is a usage error.</li>
 *   <li>{@code --mode single|pool|per-chunk|compare}: which strategy to run. Default
 *       {@code compare}, which runs all three and prints the comparison table.</li>
 *   <li>{@code --output <file>}: write every prime in {@code [1, N]} in ascending order to a file.
 *       Allowed with {@code single}, {@code pool}, and {@code per-chunk}; combined with
 *       {@code compare} it is a usage error.</li>
 * </ul>
 *
 * <p>{@link #run(String[], PrintStream, PrintStream)} returns the process exit code rather than
 * calling {@link System#exit(int)}, so argument handling is testable: {@code 0} on success,
 * {@code 2} on a usage error (unknown flag, non-numeric or too-small {@code --n}, or
 * {@code --output} with {@code compare}), and {@code 1} on a runtime failure (such as an
 * unwritable output file).
 */
public final class Main {

    /** The fixed number of worker threads in the pool. */
    public static final int POOL_SIZE = 10;

    /** The fixed number of chunks the range is split into. */
    public static final int CHUNK_COUNT = 100;

    /** The default upper bound of the range when {@code --n} is not given. */
    public static final long DEFAULT_N = 50_000_000L;

    private Main() {
    }

    /**
     * Runs the program and returns its exit code.
     *
     * <p>For now this is a tracer. With no arguments it prints the fixed configuration to
     * {@code out} and returns {@code 0}. With any arguments it prints a note that argument parsing
     * and the run modes are not implemented yet to {@code err} and returns {@code 2}, since it
     * cannot yet honour a request. Slice 4 replaces the body with the full command-line contract
     * described on this class.
     *
     * @param args the command-line arguments
     * @param out  the stream for normal output
     * @param err  the stream for usage and error messages
     * @return the process exit code: {@code 0} success, {@code 2} usage error, {@code 1} runtime failure
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length > 0) {
            err.println("argument parsing and run modes are not implemented yet.");
            return 2;
        }
        out.println("thread-pooling tactic (SWEN-755)");
        out.println("pool size:            " + POOL_SIZE + " worker threads");
        out.println("chunk count:          " + CHUNK_COUNT + " chunks");
        out.println("default N:            " + DEFAULT_N);
        out.println("available processors: " + Runtime.getRuntime().availableProcessors());
        // TODO(slice 4: Mallikarjuna): parse --n/--mode/--output, dispatch the strategies and
        // Compare, print the single-mode summary and known-pi check, and write the primes file.
        return 0;
    }

    /**
     * Process entry point. Delegates to {@link #run(String[], PrintStream, PrintStream)} and exits
     * with its return value.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }
}

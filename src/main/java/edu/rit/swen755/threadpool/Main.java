package edu.rit.swen755.threadpool;

import edu.rit.swen755.threadpool.primes.Chunk;
import edu.rit.swen755.threadpool.primes.ChunkResult;
import edu.rit.swen755.threadpool.primes.Chunks;
import edu.rit.swen755.threadpool.primes.KnownPrimeCounts;
import edu.rit.swen755.threadpool.run.Compare;
import edu.rit.swen755.threadpool.run.ExecutionStrategy;
import edu.rit.swen755.threadpool.run.RunReport;
import edu.rit.swen755.threadpool.run.SingleThreadStrategy;
import edu.rit.swen755.threadpool.run.ThreadPerChunkStrategy;
import edu.rit.swen755.threadpool.run.ThreadPoolStrategy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

/**
 * Command-line entry point for the thread-pooling prime counter.
 *
 * <p>The program finds and counts the primes in {@code [1, N]} by splitting the range into
 * {@link #CHUNK_COUNT} chunks and running them through one of several execution strategies. The
 * pool size and chunk count are fixed constants, not options.
 *
 * <p>The command-line contract is:
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

    private static final String USAGE = String.join(System.lineSeparator(),
            "usage: java -jar target/thread-pooling.jar [--n <N>] [--mode <mode>] [--output <file>]",
            "  --n <N>          upper bound of the range [1, N]; default " + DEFAULT_N
                    + ", minimum " + CHUNK_COUNT,
            "  --mode <mode>    single | pool | per-chunk | compare; default compare",
            "  --output <file>  write every prime in [1, N], one per line, ascending;",
            "                   not allowed with --mode compare");

    /**
     * Runs the program and returns its exit code.
     *
     * <p>Parses the arguments, splits {@code [1, N]} into {@link #CHUNK_COUNT} chunks, and runs
     * the requested mode. {@code compare} prints the comparison table. The other modes print a
     * one-line summary; {@code pool} follows it with the chunk-to-worker map. Every mode ends with
     * a check of the count against the published value, when there is one for {@code N}.
     *
     * @param args the command-line arguments
     * @param out  the stream for normal output
     * @param err  the stream for usage and error messages
     * @return the process exit code: {@code 0} success, {@code 2} usage error, {@code 1} runtime failure
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        long n = DEFAULT_N;
        String mode = "compare";
        String output = null;

        for (int i = 0; i < args.length; i += 2) {
            String flag = args[i];
            if (!flag.equals("--n") && !flag.equals("--mode") && !flag.equals("--output")) {
                return usageError(err, "unknown argument: " + flag);
            }
            if (i + 1 >= args.length) {
                return usageError(err, flag + " needs a value");
            }
            String value = args[i + 1];
            switch (flag) {
                case "--n" -> {
                    try {
                        n = Long.parseLong(value);
                    } catch (NumberFormatException notANumber) {
                        return usageError(err, "--n must be a whole number, got: " + value);
                    }
                }
                case "--mode" -> mode = value;
                default -> output = value;
            }
        }
        if (n < CHUNK_COUNT) {
            return usageError(err, "--n must be at least " + CHUNK_COUNT + ", got: " + n);
        }
        if (!List.of("single", "pool", "per-chunk", "compare").contains(mode)) {
            return usageError(err, "unknown mode: " + mode);
        }
        if (mode.equals("compare") && output != null) {
            return usageError(err, "--output cannot be combined with --mode compare");
        }

        out.println("thread-pooling tactic (SWEN-755): primes in [1, " + n + "], "
                + CHUNK_COUNT + " chunks, pool of " + POOL_SIZE + " worker threads");
        try {
            List<Chunk> chunks = Chunks.split(n, CHUNK_COUNT);
            long total;
            if (mode.equals("compare")) {
                List<RunReport> reports = Compare.runAll(chunks, POOL_SIZE, out);
                out.println(Compare.formatTable(reports));
                total = reports.get(0).totalPrimes();
                for (RunReport report : reports) {
                    if (report.totalPrimes() != total) {
                        err.println("error: the strategies disagree on the prime count");
                        return 1;
                    }
                }
                out.println("all three strategies found " + total + " primes");
            } else {
                ExecutionStrategy strategy = switch (mode) {
                    case "single" -> new SingleThreadStrategy();
                    case "pool" -> new ThreadPoolStrategy(POOL_SIZE);
                    default -> new ThreadPerChunkStrategy();
                };
                RunReport report = strategy.run(chunks, output != null);
                total = report.totalPrimes();
                out.println(String.format(Locale.ROOT, "%s: %d primes in %.3f ms on %d thread(s)",
                        report.strategy(), total, report.wallNanos() / 1_000_000.0,
                        report.threadsUsed()));
                if (mode.equals("pool")) {
                    out.println(Compare.formatChunkMap(report));
                }
                if (output != null) {
                    writePrimes(report, Path.of(output));
                    out.println("wrote " + total + " primes to " + output);
                }
            }

            OptionalLong known = KnownPrimeCounts.lookup(n);
            if (known.isEmpty()) {
                out.println("known-pi check: no published value for N = " + n);
            } else if (known.getAsLong() == total) {
                out.println("known-pi check: OK, pi(" + n + ") = " + total);
            } else {
                err.println("error: known-pi check failed, expected " + known.getAsLong()
                        + " but counted " + total);
                return 1;
            }
            return 0;
        } catch (IOException | RuntimeException failure) {
            err.println("error: " + failure);
            return 1;
        }
    }

    private static int usageError(PrintStream err, String message) {
        err.println("error: " + message);
        err.println(USAGE);
        return 2;
    }

    /** Writes the primes one per line; chunks are in order and each chunk's primes are sorted. */
    private static void writePrimes(RunReport report, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (ChunkResult result : report.results()) {
                for (long prime : result.primes()) {
                    writer.write(Long.toString(prime));
                    writer.newLine();
                }
            }
        }
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

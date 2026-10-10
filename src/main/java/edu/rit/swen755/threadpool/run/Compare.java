package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.Chunk;
import edu.rit.swen755.threadpool.primes.Chunks;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Runs all three strategies on the same chunks and formats the comparison output.
 *
 * <p>An untimed warm-up uses the same number of chunks over {@code [1, 10^6]}, followed by the
 * measured runs.
 */
public final class Compare {

    private Compare() {
    }

    /**
     * Warms up, then runs the single, pool, and per-chunk strategies on the same chunks.
     *
     * <p>Before measuring, an untimed warm-up pass runs all three strategies over a smaller range
     * (a split of {@code 10^6} into the same number of chunks) so that JIT compilation does not
     * skew the first measured strategy. The measured runs then execute in the order single, pool,
     * per-chunk, and a {@code running <strategy>...} line is written to {@code progress} before each
     * one, so a long run visibly stays alive. The warm-up and progress lines are the only output;
     * nothing is written inside a timed region.
     *
     * @param chunks   the chunks to measure
     * @param poolSize the pool size for the pool strategy
     * @param progress the stream that receives the {@code running <strategy>...} lines
     * @return the three reports in the order single, pool, per-chunk
     */
    public static List<RunReport> runAll(List<Chunk> chunks, int poolSize, PrintStream progress) {
        List<Chunk> warmupChunks = Chunks.split(1_000_000L, chunks.size());
        new SingleThreadStrategy().run(warmupChunks, false);
        new ThreadPoolStrategy(poolSize).run(warmupChunks, false);
        new ThreadPerChunkStrategy().run(warmupChunks, false);

        List<RunReport> reports = new ArrayList<>(3);
        progress.println("running single...");
        reports.add(new SingleThreadStrategy().run(chunks, false));
        progress.println("running pool...");
        reports.add(new ThreadPoolStrategy(poolSize).run(chunks, false));
        progress.println("running per-chunk...");
        reports.add(new ThreadPerChunkStrategy().run(chunks, false));
        return List.copyOf(reports);
    }

    /**
     * Formats the comparison table that goes in the README.
     *
     * <p>The table begins with a header line containing the exact text
     * {@code available processors: <n>}, where {@code <n>} is {@link Runtime#availableProcessors()},
     * then shows wall time, threads used, and chunks per thread for each strategy. The per-chunk
     * strategy is summarised as one row reading {@code 100 threads × 1 chunk} rather than listed as
     * 100 separate rows.
     *
     * @param reports the reports to tabulate, as returned by {@link #runAll}
     * @return the formatted table as a multi-line string
     */
    public static String formatTable(List<RunReport> reports) {
        StringBuilder table = new StringBuilder()
                .append("available processors: ")
                .append(Runtime.getRuntime().availableProcessors())
                .append(System.lineSeparator())
                .append(String.format(Locale.ROOT, "%-12s | %12s | %-13s | %s",
                        "Strategy", "Wall time (ms)", "Threads used", "Chunks per thread"))
                .append(System.lineSeparator())
                .append("-------------|--------------|---------------|------------------");
        for (RunReport report : reports) {
            var chunksPerThread = report.chunksPerThread();
            String chunkSummary;
            if ("per-chunk".equals(report.strategy())
                    && !chunksPerThread.isEmpty()
                    && chunksPerThread.values().stream().allMatch(count -> count == 1)) {
                chunkSummary = report.threadsUsed() + " threads × 1 chunk";
            } else {
                chunkSummary = chunksPerThread.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("0 chunks");
            }
            table.append(System.lineSeparator())
                    .append(String.format(Locale.ROOT, "%-12s | %12.3f | %-13d | %s",
                            report.strategy(), report.wallNanos() / 1_000_000.0,
                            report.threadsUsed(), chunkSummary));
        }
        return table.toString();
    }

    /**
     * Formats a chunk-to-worker map for a pool run, listing every chunk and the worker that ran it.
     *
     * <p>This is the reuse evidence printed after timing in {@code --mode pool}: one line per chunk,
     * in chunk order, so all 100 chunks appear and several map to the same {@code pool-worker-*}.
     *
     * @param poolReport the report from a pool run
     * @return the formatted chunk-to-worker map as a multi-line string
     */
    public static String formatChunkMap(RunReport poolReport) {
        StringBuilder map = new StringBuilder();
        for (var result : poolReport.results()) {
            if (!map.isEmpty()) {
                map.append(System.lineSeparator());
            }
            map.append("chunk-").append(result.index()).append(" -> ").append(result.workerName());
        }
        return map.toString();
    }
}

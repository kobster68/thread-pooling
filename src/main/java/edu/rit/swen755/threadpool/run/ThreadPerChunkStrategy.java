package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.Chunk;
import edu.rit.swen755.threadpool.primes.ChunkResult;
import edu.rit.swen755.threadpool.primes.PrimeRangeTask;

import java.util.ArrayList;
import java.util.List;

/**
 * The no-pooling alternative: starts one new thread per chunk, with no reuse.
 *
 * <p>Its reported name is {@code per-chunk}. The strategy creates one {@link Thread} per chunk,
 * named {@code chunk-thread-<index>} after the chunk's index, starts them all, and joins them all
 * before reading results. Because every chunk gets its own one-shot thread, {@code threadsUsed}
 * equals the number of chunks (100 in the shipped program).
 */
public final class ThreadPerChunkStrategy implements ExecutionStrategy {

    /** The strategy name, matching the {@code --mode per-chunk} value. */
    private static final String NAME = "per-chunk";

    /**
     * Starts one thread per chunk, named {@code chunk-thread-<index>}, joins them all, and returns
     * the report.
     *
     * @param chunks     the chunks to process, in chunk order
     * @param keepPrimes whether each task should keep its chunk's primes
     * @return a report named {@code per-chunk} whose {@code threadsUsed} equals the chunk count
     * @throws IllegalStateException if any result slot is still empty after the threads finish
     */
    @Override
    public RunReport run(List<Chunk> chunks, boolean keepPrimes) {
        ChunkResult[] results = new ChunkResult[chunks.size()];
        long started = System.nanoTime();
        List<Thread> threads = new ArrayList<>(chunks.size());
        for (Chunk chunk : chunks) {
            threads.add(new Thread(new PrimeRangeTask(chunk, keepPrimes, results),
                    "chunk-thread-" + chunk.index()));
        }
        try {
            for (Thread thread : threads) {
                thread.start();
            }
        } catch (RuntimeException | Error failure) {
            if (joinThreads(threads)) {
                Thread.currentThread().interrupt();
            }
            throw failure;
        }
        boolean interrupted = joinThreads(threads);
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for chunk threads");
        }
        long elapsed = System.nanoTime() - started;

        List<ChunkResult> orderedResults = RunResults.completedInOrder(results);
        return new RunReport(NAME, elapsed, (int) orderedResults.stream()
                .map(ChunkResult::workerName)
                .distinct()
                .count(), orderedResults);
    }

    private static boolean joinThreads(List<Thread> threads) {
        boolean interrupted = false;
        for (Thread thread : threads) {
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
        }
        return interrupted;
    }
}

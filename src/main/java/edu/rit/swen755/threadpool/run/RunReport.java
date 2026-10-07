package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.ChunkResult;

import java.util.List;
import java.util.SortedMap;

/**
 * The result of one {@link ExecutionStrategy} run: its name, wall time, threads used, and the
 * per-chunk results in chunk order.
 *
 * <p>The record itself is complete. The two derived views, {@link #totalPrimes()} and
 * {@link #chunksPerThread()}, are stubs until slice 3 lands.
 *
 * @param strategy    the strategy name, equal to the matching {@code --mode} value
 *                    ({@code single}, {@code pool}, or {@code per-chunk})
 * @param wallNanos   wall time of the whole run, in nanoseconds
 * @param threadsUsed the number of distinct threads that ran at least one task: 1 for single,
 *                    the number of chunks for per-chunk, and at most the pool size (normally all
 *                    of them) for the pool
 * @param results     the per-chunk results, in chunk order
 */
public record RunReport(String strategy, long wallNanos, int threadsUsed, List<ChunkResult> results) {

    /**
     * Returns the total number of primes across all chunks.
     *
     * @return the sum of every chunk's prime count
     */
    public long totalPrimes() {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }

    /**
     * Returns how many chunks each thread ran, keyed by worker name.
     *
     * <p>The counts sum to the number of chunks. For a pool run the keys are the
     * {@code pool-worker-*} names that ran at least one chunk (at most the pool size); for a
     * per-chunk run every key is a distinct {@code chunk-thread-*} name.
     *
     * <p>The map orders keys by their name prefix and then by the numeric suffix read as a number,
     * not as text, so {@code pool-worker-2} comes before {@code pool-worker-10} and
     * {@code chunk-thread-2} before {@code chunk-thread-10}.
     *
     * @return a map from worker name to the count of chunks that worker ran, ordered by name prefix
     *         then numeric suffix
     */
    public SortedMap<String, Integer> chunksPerThread() {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }
}

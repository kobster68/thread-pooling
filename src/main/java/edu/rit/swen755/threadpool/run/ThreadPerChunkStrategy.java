package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.Chunk;

import java.util.List;

/**
 * The no-pooling alternative: starts one new thread per chunk, with no reuse.
 *
 * <p>Its reported name is {@code per-chunk}. The strategy creates one {@link Thread} per chunk,
 * named {@code chunk-thread-<index>} after the chunk's index, starts them all, and joins them all
 * before reading results. Because every chunk gets its own one-shot thread, {@code threadsUsed}
 * equals the number of chunks (100 in the shipped program). This is a stub until slice 3 lands.
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
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }
}

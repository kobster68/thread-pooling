package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.Chunk;

import java.util.List;

/**
 * Baseline strategy: runs every chunk on the calling thread, one after another.
 *
 * <p>Its reported name is {@code single} and its {@code threadsUsed} is always 1, since no
 * extra threads are started. This is a stub until slice 3 lands.
 */
public final class SingleThreadStrategy implements ExecutionStrategy {

    /** The strategy name, matching the {@code --mode single} value. */
    private static final String NAME = "single";

    /**
     * Runs every chunk on the calling thread and returns the report.
     *
     * @param chunks     the chunks to process, in chunk order
     * @param keepPrimes whether each task should keep its chunk's primes
     * @return a report named {@code single} with {@code threadsUsed == 1}
     * @throws IllegalStateException if any result slot is still empty after the run
     */
    @Override
    public RunReport run(List<Chunk> chunks, boolean keepPrimes) {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }
}

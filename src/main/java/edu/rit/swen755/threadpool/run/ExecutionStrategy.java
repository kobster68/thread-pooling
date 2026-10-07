package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.Chunk;

import java.util.List;

/**
 * One way of running the same set of {@link Chunk}s: on a single thread, on the thread pool,
 * or on one thread per chunk.
 *
 * <p>Every strategy times its whole run with {@link System#nanoTime()}, prints nothing inside
 * the timed region, waits for its threads to finish before reading results, and reports which
 * threads ran work. After the threads finish, a strategy throws {@link IllegalStateException}
 * if any result slot is still empty, so a failed task surfaces as an error rather than a wrong
 * count.
 */
public interface ExecutionStrategy {

    /**
     * Runs every chunk and returns a report of the run.
     *
     * @param chunks     the chunks to process; the report's results are in chunk order
     * @param keepPrimes whether each task should keep its chunk's primes (primes-file mode)
     * @return a {@link RunReport} with the strategy name, wall time, threads used, and results
     * @throws IllegalStateException if any result slot is still empty after the threads finish
     */
    RunReport run(List<Chunk> chunks, boolean keepPrimes);
}

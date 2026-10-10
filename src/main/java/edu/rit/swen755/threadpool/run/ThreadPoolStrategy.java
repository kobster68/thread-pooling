package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.Chunk;
import edu.rit.swen755.threadpool.primes.ChunkResult;
import edu.rit.swen755.threadpool.primes.PrimeRangeTask;
import edu.rit.swen755.threadpool.pool.ThreadPool;

import java.util.List;

/**
 * The tactic under test: runs every chunk on a hand-built {@code ThreadPool}.
 *
 * <p>Its reported name is {@code pool}. The strategy creates a pool of {@code poolSize} workers,
 * submits all chunks, shuts the pool down, and awaits termination before reading results. Because
 * the fixed pool reuses its workers across chunks, {@code threadsUsed} is at most {@code poolSize}
 * (normally all of them, since there are far more chunks than workers).
 */
public final class ThreadPoolStrategy implements ExecutionStrategy {

    /** The strategy name, matching the {@code --mode pool} value. */
    private static final String NAME = "pool";

    private final int poolSize;

    /**
     * Creates the strategy with the pool size to use.
     *
     * @param poolSize the number of worker threads the pool should create
     */
    public ThreadPoolStrategy(int poolSize) {
        this.poolSize = poolSize;
    }

    /**
     * Submits every chunk to a {@code ThreadPool} of {@code poolSize} workers, waits for the pool
     * to drain and terminate, and returns the report.
     *
     * @param chunks     the chunks to process, in chunk order
     * @param keepPrimes whether each task should keep its chunk's primes
     * @return a report named {@code pool} whose {@code threadsUsed} is the number of workers that ran work
     * @throws IllegalStateException if any result slot is still empty after the pool terminates
     */
    @Override
    public RunReport run(List<Chunk> chunks, boolean keepPrimes) {
        ChunkResult[] results = new ChunkResult[chunks.size()];
        long started = System.nanoTime();
        ThreadPool pool = new ThreadPool(poolSize);
        try {
            for (Chunk chunk : chunks) {
                pool.submit(new PrimeRangeTask(chunk, keepPrimes, results));
            }
        } finally {
            pool.shutdown();
        }

        try {
            pool.awaitTermination();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for the thread pool", interrupted);
        }
        long elapsed = System.nanoTime() - started;

        List<ChunkResult> orderedResults = RunResults.completedInOrder(results);
        int threadsUsed = (int) orderedResults.stream()
                .map(ChunkResult::workerName)
                .distinct()
                .count();
        return new RunReport(NAME, elapsed, threadsUsed, orderedResults);
    }

}

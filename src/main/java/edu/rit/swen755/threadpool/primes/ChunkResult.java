package edu.rit.swen755.threadpool.primes;

/**
 * The outcome of scanning one {@link Chunk} for primes.
 *
 * <p>A {@link PrimeRangeTask} produces exactly one of these and writes it into its
 * own slot of a shared results array. The {@code index} and bounds echo the source
 * chunk so a result is self-describing. {@code workerName} is the name of the thread
 * that ran the task (for example {@code pool-worker-3} or {@code chunk-thread-42}),
 * which is how thread reuse and load balance are reported. {@code elapsedNanos} is
 * the wall time the task spent on its chunk.
 *
 * <p>{@code primes} holds the chunk's primes in ascending order only when the task was
 * asked to keep them (primes-file mode); otherwise it is an empty array, so a count-only
 * run stays light on memory.
 *
 * @param index        zero-based chunk position, matching the source {@link Chunk}
 * @param first        first integer in the chunk, inclusive
 * @param last         last integer in the chunk, inclusive
 * @param primeCount   number of primes found in the chunk
 * @param workerName   name of the thread that ran the task
 * @param elapsedNanos time the task spent scanning the chunk, in nanoseconds
 * @param primes       the chunk's primes in ascending order, or an empty array when not kept
 */
public record ChunkResult(int index, long first, long last, long primeCount,
                          String workerName, long elapsedNanos, long[] primes) {
}

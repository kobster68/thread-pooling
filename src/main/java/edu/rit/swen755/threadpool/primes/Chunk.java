package edu.rit.swen755.threadpool.primes;

/**
 * One contiguous, inclusive sub-range of {@code [1, N]} to be scanned for primes.
 *
 * <p>The range {@code [1, N]} is split into a fixed number of chunks (100 in the
 * shipped program). {@code index} is the chunk's position in that split, starting
 * at 0, and is used as the slot index when a task writes its {@link ChunkResult}
 * into the shared results array. Both {@code first} and {@code last} are inclusive,
 * so the chunk covers every integer {@code k} with {@code first <= k <= last}.
 *
 * @param index zero-based position of this chunk in the split
 * @param first first integer in the range, inclusive
 * @param last  last integer in the range, inclusive
 */
public record Chunk(int index, long first, long last) {
}

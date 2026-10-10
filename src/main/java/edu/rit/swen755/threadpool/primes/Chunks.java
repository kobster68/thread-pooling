package edu.rit.swen755.threadpool.primes;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits {@code [1, N]} into contiguous, inclusive chunks of equal width.
 *
 * <p>This is the workload partitioner. Any remainder belongs to the last chunk.
 */
public final class Chunks {

    private Chunks() {
    }

    /**
     * Splits {@code [1, n]} into {@code count} contiguous, inclusive, non-overlapping chunks.
     *
     * <p>Every chunk has width {@code floor(n / count)} except the last, which absorbs any
     * remainder so that the final chunk ends exactly at {@code n} and no integer is dropped.
     * The chunks cover {@code [1, n]} with no gaps and no overlaps, and their {@code index}
     * values run from {@code 0} to {@code count - 1} in range order. For example,
     * {@code split(1000, 100)} yields 100 chunks of width 10 (chunk 0 is {@code [1, 10]},
     * chunk 99 is {@code [991, 1000]}), and {@code split(1005, 100)} puts the extra 5 integers
     * in the last chunk ({@code [991, 1005]}).
     *
     * @param n     upper bound of the range, inclusive
     * @param count number of chunks to produce
     * @return the chunks in range order, exactly {@code count} of them
     * @throws IllegalArgumentException if {@code count <= 0} or {@code n < count}
     */
    public static List<Chunk> split(long n, int count) {
        if (count <= 0 || n < count) {
            throw new IllegalArgumentException("count must be positive and n must be at least count");
        }

        long width = n / count;
        List<Chunk> chunks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long first = i * width + 1;
            long last = i == count - 1 ? n : (i + 1L) * width;
            chunks.add(new Chunk(i, first, last));
        }
        return chunks;
    }
}

package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.ChunkResult;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The result of one {@link ExecutionStrategy} run: its name, wall time, threads used, and the
 * per-chunk results in chunk order.
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
        return results.stream().mapToLong(ChunkResult::primeCount).sum();
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
        SortedMap<String, Integer> chunksByThread = new TreeMap<>(RunReport::compareThreadNames);
        for (ChunkResult result : results) {
            chunksByThread.merge(result.workerName(), 1, Integer::sum);
        }
        return chunksByThread;
    }

    private static int compareThreadNames(String left, String right) {
        ThreadName leftName = ThreadName.parse(left);
        ThreadName rightName = ThreadName.parse(right);
        int prefixOrder = leftName.prefix().compareTo(rightName.prefix());
        if (prefixOrder != 0) {
            return prefixOrder;
        }
        if (leftName.numericSuffix() == null || rightName.numericSuffix() == null) {
            return left.compareTo(right);
        }
        int suffixOrder = compareNumbers(leftName.numericSuffix(), rightName.numericSuffix());
        return suffixOrder != 0 ? suffixOrder : left.compareTo(right);
    }

    private static int compareNumbers(String left, String right) {
        String normalizedLeft = stripLeadingZeroes(left);
        String normalizedRight = stripLeadingZeroes(right);
        int lengthOrder = Integer.compare(normalizedLeft.length(), normalizedRight.length());
        return lengthOrder != 0 ? lengthOrder : normalizedLeft.compareTo(normalizedRight);
    }

    private static String stripLeadingZeroes(String number) {
        int firstNonZero = 0;
        while (firstNonZero < number.length() - 1 && number.charAt(firstNonZero) == '0') {
            firstNonZero++;
        }
        return number.substring(firstNonZero);
    }

    private record ThreadName(String prefix, String numericSuffix) {
        private static ThreadName parse(String name) {
            int suffixStart = name.length();
            while (suffixStart > 0 && Character.isDigit(name.charAt(suffixStart - 1))) {
                suffixStart--;
            }
            if (suffixStart == name.length()) {
                return new ThreadName(name, null);
            }
            return new ThreadName(name.substring(0, suffixStart), name.substring(suffixStart));
        }
    }
}

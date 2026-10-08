package edu.rit.swen755.threadpool.primes;

import java.util.Arrays;

/**
 * Counts (and optionally keeps) the primes in one {@link Chunk}, as a {@link Runnable}.
 *
 * <p>This is the performance-critical unit of work. Each task scans its own range and
 * publishes its result only after the scan is complete.
 */
public final class PrimeRangeTask implements Runnable {

    private final Chunk chunk;
    private final boolean keepPrimes;
    private final ChunkResult[] slots;

    /**
     * Creates a task for one chunk.
     *
     * @param chunk      the inclusive range to scan
     * @param keepPrimes whether to keep the chunk's primes in the result (primes-file mode)
     * @param slots      the shared results array; the task writes only {@code slots[chunk.index()]}
     */
    public PrimeRangeTask(Chunk chunk, boolean keepPrimes, ChunkResult[] slots) {
        this.chunk = chunk;
        this.keepPrimes = keepPrimes;
        this.slots = slots;
    }

    /**
     * Scans the chunk for primes and writes the result into the task's own slot.
     *
     * <p>Primality is decided by trial division with a {@code long} divisor {@code d}, looping
     * while {@code d <= n / d} (equivalent to testing divisors up to the square root, but phrased
     * as a division so it cannot overflow for any {@code long n}). By convention 1 is
     * not prime and 2 is prime. The task writes only {@code slots[chunk.index()]}, so no
     * locking is needed: each task owns a distinct slot. The written {@link ChunkResult}
     * records the running thread's name ({@link Thread#getName()}) and the elapsed time of
     * the scan. When {@code keepPrimes} is set, the result carries the chunk's primes in
     * ascending order; otherwise it carries an empty array.
     */
    @Override
    public void run() {
        long started = System.nanoTime();
        long primeCount = 0;
        long[] primes = new long[keepPrimes ? 16 : 0];
        int keptCount = 0;

        for (long number = chunk.first(); number <= chunk.last(); number++) {
            if (isPrime(number)) {
                primeCount++;
                if (keepPrimes) {
                    if (keptCount == primes.length) {
                        primes = Arrays.copyOf(primes, Math.multiplyExact(primes.length, 2));
                    }
                    primes[keptCount++] = number;
                }
            }
            // Stop before incrementing Long.MAX_VALUE, which would wrap around.
            if (number == chunk.last()) {
                break;
            }
        }

        if (keepPrimes) {
            primes = Arrays.copyOf(primes, keptCount);
        }
        long elapsed = System.nanoTime() - started;
        slots[chunk.index()] = new ChunkResult(chunk.index(), chunk.first(), chunk.last(),
                primeCount, Thread.currentThread().getName(), elapsed, primes);
    }

    private static boolean isPrime(long number) {
        if (number < 2) {
            return false;
        }
        if (number % 2 == 0) {
            return number == 2;
        }
        for (long divisor = 3; divisor <= number / divisor; divisor += 2) {
            if (number % divisor == 0) {
                return false;
            }
        }
        return true;
    }
}

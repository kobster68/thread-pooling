package edu.rit.swen755.threadpool.primes;

import java.util.OptionalLong;

/**
 * Published prime-counting-function values, used to check the program's own count.
 *
 * <p>This is a stub until slice 2 lands.
 */
public final class KnownPrimeCounts {

    private KnownPrimeCounts() {
    }

    /**
     * Returns the known number of primes in {@code [1, n]} for a handful of round values.
     *
     * <p>The recognised values are the counts of primes up to each bound:
     * {@code 10^6 -> 78,498}, {@code 10^7 -> 664,579}, {@code 5x10^7 -> 3,001,134},
     * and {@code 10^8 -> 5,761,455}. Any other {@code n} returns an empty result, since
     * there is no published value to compare against.
     *
     * @param n the upper bound of the range, inclusive
     * @return the known prime count for {@code n}, or empty if {@code n} is not a recognised value
     */
    public static OptionalLong lookup(long n) {
        throw new UnsupportedOperationException("TODO(slice 2: Kobe)");
    }
}

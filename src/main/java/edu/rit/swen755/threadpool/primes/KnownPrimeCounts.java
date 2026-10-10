package edu.rit.swen755.threadpool.primes;

import java.util.OptionalLong;

/**
 * Published prime-counting-function values, used to check the program's own count.
 *
 * <p>Only the four reference bounds used by the assignment are recognised.
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
        if (n == 1_000_000L) {
            return OptionalLong.of(78_498);
        }
        if (n == 10_000_000L) {
            return OptionalLong.of(664_579);
        }
        if (n == 50_000_000L) {
            return OptionalLong.of(3_001_134);
        }
        if (n == 100_000_000L) {
            return OptionalLong.of(5_761_455);
        }
        return OptionalLong.empty();
    }
}

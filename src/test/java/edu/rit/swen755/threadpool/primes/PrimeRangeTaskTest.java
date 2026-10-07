package edu.rit.swen755.threadpool.primes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance tests for {@link PrimeRangeTask} and {@link KnownPrimeCounts} (slice 2). Disabled
 * until the slice lands.
 */
@Disabled("TODO(slice 2: Kobe) — enable when implementing")
class PrimeRangeTaskTest {

    @Test
    void chunkFromOneToTenCountsFourPrimes() {
        ChunkResult[] slots = new ChunkResult[1];
        new PrimeRangeTask(new Chunk(0, 1, 10), false, slots).run();

        assertEquals(4, slots[0].primeCount(), "primes in [1,10] are 2, 3, 5, 7");
    }

    @Test
    void chunksOverOneMillionSumToKnownCount() {
        List<Chunk> chunks = Chunks.split(1_000_000, 100);
        ChunkResult[] slots = new ChunkResult[chunks.size()];
        for (Chunk chunk : chunks) {
            new PrimeRangeTask(chunk, false, slots).run();
        }

        long total = 0;
        for (ChunkResult result : slots) {
            total += result.primeCount();
        }
        assertEquals(78_498, total, "there are 78,498 primes in [1, 10^6]");
    }

    @Test
    void keepsActualPrimesInAscendingOrderOnlyWhenAsked() {
        ChunkResult[] kept = new ChunkResult[1];
        new PrimeRangeTask(new Chunk(0, 1, 10), true, kept).run();
        assertArrayEquals(new long[]{2, 3, 5, 7}, kept[0].primes());

        ChunkResult[] countOnly = new ChunkResult[1];
        new PrimeRangeTask(new Chunk(0, 1, 10), false, countOnly).run();
        assertEquals(0, countOnly[0].primes().length, "primes are empty unless keepPrimes is set");
    }

    @Test
    void writesOnlyItsOwnSlotAndRecordsWorkerNameAndElapsed() {
        ChunkResult[] slots = new ChunkResult[3];
        new PrimeRangeTask(new Chunk(1, 11, 20), false, slots).run();

        assertEquals(null, slots[0], "a task must not touch another task's slot");
        assertEquals(null, slots[2], "a task must not touch another task's slot");
        assertEquals(1, slots[1].index());
        assertEquals(Thread.currentThread().getName(), slots[1].workerName(),
                "the result records the thread that ran the task");
        assertTrue(slots[1].elapsedNanos() >= 0, "elapsed time is recorded");
    }

    @Test
    void oneCountsAsNotPrimeAndTwoCountsAsPrime() {
        ChunkResult[] slots = new ChunkResult[1];
        new PrimeRangeTask(new Chunk(0, 1, 2), true, slots).run();

        assertEquals(1, slots[0].primeCount(), "only 2 is prime in [1,2]");
        assertArrayEquals(new long[]{2}, slots[0].primes());
    }

    @Test
    void knownPrimeCountsReturnPublishedValuesAndEmptyForUnknown() {
        assertEquals(OptionalLong.of(78_498), KnownPrimeCounts.lookup(1_000_000));
        assertEquals(OptionalLong.of(664_579), KnownPrimeCounts.lookup(10_000_000));
        assertEquals(OptionalLong.of(3_001_134), KnownPrimeCounts.lookup(50_000_000));
        assertEquals(OptionalLong.of(5_761_455), KnownPrimeCounts.lookup(100_000_000));

        assertFalse(KnownPrimeCounts.lookup(12_345).isPresent(), "an unknown N returns empty");
    }
}

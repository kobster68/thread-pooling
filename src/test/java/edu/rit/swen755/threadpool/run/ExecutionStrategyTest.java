package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.Chunk;
import edu.rit.swen755.threadpool.primes.Chunks;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance tests for the three {@link ExecutionStrategy} implementations and the derived views
 * on {@link RunReport} (slice 3). These run real prime tasks on the real pool, so they need
 * slices 1 and 2 as well. Disabled until slice 3 lands.
 */
@Disabled("TODO(slice 3: Chase) — enable when implementing")
class ExecutionStrategyTest {

    /** Range used for the correctness checks; the count is the known prime count for 10^6. */
    private static final long N = 1_000_000L;
    private static final long KNOWN_PI = 78_498L;

    /** A larger range so that every one of the 10 pool workers reliably takes at least one chunk. */
    private static final long PARTICIPATION_N = 5_000_000L;

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void allThreeStrategiesReportTheSameTotal() {
        List<Chunk> chunks = Chunks.split(N, 100);

        long single = new SingleThreadStrategy().run(chunks, false).totalPrimes();
        long pool = new ThreadPoolStrategy(10).run(chunks, false).totalPrimes();
        long perChunk = new ThreadPerChunkStrategy().run(chunks, false).totalPrimes();

        assertEquals(KNOWN_PI, single, "single-thread total should match the known prime count");
        assertEquals(single, pool, "the pool should agree with the single-thread baseline");
        assertEquals(single, perChunk, "per-chunk should agree with the single-thread baseline");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void threadsUsedIsOneForSingleBoundedByPoolSizeAndOnePerChunk() {
        List<Chunk> chunks = Chunks.split(PARTICIPATION_N, 100);

        assertEquals(1, new SingleThreadStrategy().run(chunks, false).threadsUsed(),
                "single thread uses only the calling thread");

        int poolThreads = new ThreadPoolStrategy(10).run(chunks, false).threadsUsed();
        assertTrue(poolThreads >= 2 && poolThreads <= 10,
                "the pool reuses at most its 10 workers, and more than one takes part: was " + poolThreads);

        assertEquals(100, new ThreadPerChunkStrategy().run(chunks, false).threadsUsed(),
                "per-chunk starts one thread per chunk");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void eachStrategyNameMatchesItsMode() {
        List<Chunk> chunks = Chunks.split(1000, 100);

        assertEquals("single", new SingleThreadStrategy().run(chunks, false).strategy());
        assertEquals("pool", new ThreadPoolStrategy(10).run(chunks, false).strategy());
        assertEquals("per-chunk", new ThreadPerChunkStrategy().run(chunks, false).strategy());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void poolChunksPerThreadSumToHundredOverAtMostTenPoolWorkers() {
        List<Chunk> chunks = Chunks.split(PARTICIPATION_N, 100);

        SortedMap<String, Integer> perThread = new ThreadPoolStrategy(10).run(chunks, false).chunksPerThread();

        assertTrue(perThread.size() <= 10, "a 10-worker pool uses at most 10 workers");
        assertEquals(100, perThread.values().stream().mapToInt(Integer::intValue).sum(),
                "the chunk counts should sum to all 100 chunks");
        assertTrue(perThread.keySet().stream().allMatch(name -> name.matches("pool-worker-\\d+")),
                "pool workers are named pool-worker-<number>");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void chunksPerThreadOrdersKeysByNumericSuffixNotText() {
        List<Chunk> chunks = Chunks.split(1000, 100);

        SortedMap<String, Integer> perThread = new ThreadPerChunkStrategy().run(chunks, false).chunksPerThread();
        List<String> keys = new java.util.ArrayList<>(perThread.keySet());

        assertEquals("chunk-thread-0", keys.get(0), "the lowest-numbered thread comes first");
        assertTrue(keys.indexOf("chunk-thread-2") < keys.indexOf("chunk-thread-10"),
                "keys order by numeric suffix, so chunk-thread-2 precedes chunk-thread-10");
        assertTrue(keys.indexOf("chunk-thread-9") < keys.indexOf("chunk-thread-10"),
                "keys order by numeric suffix, so chunk-thread-9 precedes chunk-thread-10");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void perChunkKeysAreOneHundredDistinctChunkThreads() {
        List<Chunk> chunks = Chunks.split(1000, 100);

        SortedMap<String, Integer> perThread = new ThreadPerChunkStrategy().run(chunks, false).chunksPerThread();

        assertEquals(100, perThread.size(), "per-chunk uses one distinct thread per chunk");
        assertEquals(100, perThread.values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(perThread.keySet().stream().allMatch(name -> name.startsWith("chunk-thread-")),
                "per-chunk threads are named chunk-thread-*");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void everyResultSlotIsFilledAndInChunkOrder() {
        List<Chunk> chunks = Chunks.split(1000, 100);

        List<edu.rit.swen755.threadpool.primes.ChunkResult> results =
                new ThreadPoolStrategy(10).run(chunks, false).results();

        assertEquals(100, results.size());
        assertTrue(results.stream().allMatch(Objects::nonNull), "no slot may be left empty");
        for (int i = 0; i < results.size(); i++) {
            assertEquals(i, results.get(i).index(), "results are in chunk order");
        }
    }
}

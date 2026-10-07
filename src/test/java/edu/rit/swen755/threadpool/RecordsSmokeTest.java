package edu.rit.swen755.threadpool;

import edu.rit.swen755.threadpool.primes.Chunk;
import edu.rit.swen755.threadpool.primes.ChunkResult;
import edu.rit.swen755.threadpool.run.RunReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Day-one smoke test: the shared records hold the fields they are given. This runs green from
 * step 0, before any slice is implemented, and does not touch the stubbed behaviour.
 */
class RecordsSmokeTest {

    @Test
    void chunkHoldsItsFields() {
        Chunk chunk = new Chunk(3, 21, 30);
        assertEquals(3, chunk.index());
        assertEquals(21, chunk.first());
        assertEquals(30, chunk.last());
    }

    @Test
    void chunkResultHoldsItsFields() {
        long[] primes = {2, 3, 5, 7};
        ChunkResult result = new ChunkResult(0, 1, 10, 4, "pool-worker-0", 1_234L, primes);
        assertEquals(0, result.index());
        assertEquals(1, result.first());
        assertEquals(10, result.last());
        assertEquals(4, result.primeCount());
        assertEquals("pool-worker-0", result.workerName());
        assertEquals(1_234L, result.elapsedNanos());
        assertArrayEquals(primes, result.primes());
    }

    @Test
    void runReportHoldsItsFields() {
        ChunkResult result = new ChunkResult(0, 1, 10, 4, "single", 10L, new long[0]);
        RunReport report = new RunReport("single", 5_000L, 1, List.of(result));
        assertEquals("single", report.strategy());
        assertEquals(5_000L, report.wallNanos());
        assertEquals(1, report.threadsUsed());
        assertEquals(1, report.results().size());
        assertSame(result, report.results().get(0));
    }
}

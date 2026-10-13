package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.Chunk;
import edu.rit.swen755.threadpool.primes.Chunks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance tests for {@link Compare} (slice 3). These run all three strategies, so they need
 * slices 1 and 2 as well. {@link Compare#runAll} is executed once and its reports and progress
 * output are shared across the tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompareTest {

    private List<RunReport> reports;
    private String progressOutput;

    @BeforeAll
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void runCompareOnce() {
        List<Chunk> chunks = Chunks.split(1000, 100);
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        reports = Compare.runAll(chunks, 10, new PrintStream(sink, true, StandardCharsets.UTF_8));
        progressOutput = sink.toString(StandardCharsets.UTF_8);
    }

    @Test
    void runAllReturnsThreeReportsInStrategyOrder() {
        assertEquals(3, reports.size());
        assertEquals("single", reports.get(0).strategy());
        assertEquals("pool", reports.get(1).strategy());
        assertEquals("per-chunk", reports.get(2).strategy());
    }

    @Test
    void runAllWritesAProgressLineForEachStrategy() {
        assertEquals("running single..." + System.lineSeparator()
                        + "running pool..." + System.lineSeparator()
                        + "running per-chunk..." + System.lineSeparator(),
                progressOutput);
    }

    @Test
    void formatTableShowsCoreCountAndPerChunkSummaryRow() {
        String table = Compare.formatTable(reports);
        int cores = Runtime.getRuntime().availableProcessors();

        assertTrue(table.contains("available processors: " + cores),
                "the header should read 'available processors: <n>'");
        assertTrue(table.contains("single") && table.contains("pool") && table.contains("per-chunk"),
                "the table should have a row per strategy");
        assertTrue(table.contains("100 threads") && table.contains("1 chunk"),
                "per-chunk should be summarised, not listed as 100 rows");
        assertTrue(table.contains("Wall time") && table.contains("Threads used")
                        && table.contains("Chunks per thread"),
                "the table should show every reported measurement");
        assertEquals(6, table.lines().count(), "header, columns, divider, and three strategy rows");
    }

    @Test
    void formatChunkMapListsAllHundredChunks() {
        String chunkMap = Compare.formatChunkMap(reports.get(1));

        long mappedChunks = chunkMap.lines().filter(line -> line.contains("->")).count();
        assertEquals(100, mappedChunks, "every one of the 100 chunks should be mapped to a worker");
        assertTrue(chunkMap.matches("(?s)chunk-0 -> pool-worker-\\d+.*chunk-99 -> pool-worker-\\d+$"));
    }
}

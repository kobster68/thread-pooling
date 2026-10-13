package edu.rit.swen755.threadpool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance tests for the command-line contract in {@link Main#run(String[], PrintStream, PrintStream)}
 * (slice 4).
 */
class MainArgsTest {

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

    private int run(String... args) {
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
        return Main.run(args, out, err);
    }

    private String out() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void unknownFlagReturnsUsageErrorAndPrintsUsageToErr() {
        assertEquals(2, run("--bogus"));
        assertTrue(err().toLowerCase().contains("usage"), "usage should be printed to err");
    }

    @Test
    void nonNumericNReturnsUsageError() {
        assertEquals(2, run("--n", "lots"));
        assertTrue(err().toLowerCase().contains("usage"));
    }

    @Test
    void nBelowTheChunkCountReturnsUsageError() {
        assertEquals(2, run("--n", "50"));
        assertTrue(err().toLowerCase().contains("usage"));
    }

    @Test
    void outputCombinedWithCompareReturnsUsageError() {
        assertEquals(2, run("--mode", "compare", "--output", "primes.txt"));
        assertTrue(err().toLowerCase().contains("usage"));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void validSmallRunReturnsZero() {
        assertEquals(0, run("--n", "1000", "--mode", "single"));
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void outputWritesPrimesInAscendingOrderMatchingTheKnownCount(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("primes.txt");

        assertEquals(0, run("--n", "1000000", "--mode", "single", "--output", file.toString()));

        List<String> lines = Files.readAllLines(file);
        assertEquals(78_498, lines.size(), "the file should hold every prime in [1, 10^6]");

        long previous = 0;
        for (String line : lines) {
            long prime = Long.parseLong(line.trim());
            assertTrue(prime > previous, "primes should be written in strictly ascending order");
            previous = prime;
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void unwritableOutputPathReturnsRuntimeFailure(@TempDir Path tempDir) {
        Path unwritable = tempDir.resolve("does-not-exist").resolve("primes.txt");

        assertEquals(1, run("--n", "1000", "--mode", "single", "--output", unwritable.toString()));
    }

    @Test
    void flagWithoutAValueReturnsUsageError() {
        assertEquals(2, run("--n"));
        assertTrue(err().toLowerCase().contains("usage"));
    }

    @Test
    void unknownModeReturnsUsageError() {
        assertEquals(2, run("--mode", "turbo"));
        assertTrue(err().toLowerCase().contains("usage"));
    }

    @Test
    void outputWithTheDefaultCompareModeReturnsUsageError() {
        assertEquals(2, run("--output", "primes.txt"));
        assertTrue(err().toLowerCase().contains("usage"));
    }

    @Test
    void usageErrorsPrintNothingToOut() {
        run("--bogus");
        assertEquals("", out());
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void poolModePrintsTheSummaryTheChunkMapAndTheKnownCountCheck() {
        assertEquals(0, run("--n", "1000000", "--mode", "pool"));

        assertTrue(out().contains("pool: 78498 primes"), "summary line should give the count");
        assertEquals(100, out().lines().filter(line -> line.contains("-> pool-worker-")).count(),
                "the chunk map should list all 100 chunks");
        assertTrue(out().contains("known-pi check: OK"), "the count should match the known value");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void perChunkModeDoesNotPrintTheChunkMap() {
        assertEquals(0, run("--n", "1000", "--mode", "per-chunk"));

        assertTrue(out().contains("per-chunk: 168 primes"));
        assertTrue(out().lines().noneMatch(line -> line.contains("->")));
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void compareModePrintsTheTableAndTheAgreedCount() {
        assertEquals(0, run("--n", "1000000"));

        assertTrue(out().contains("available processors: "));
        assertTrue(out().contains("all three strategies found 78498 primes"));
        assertTrue(out().contains("known-pi check: OK"));
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void everyStrategyWritesTheSamePrimesFile(@TempDir Path tempDir) throws Exception {
        Path single = tempDir.resolve("single.txt");
        Path pool = tempDir.resolve("pool.txt");
        Path perChunk = tempDir.resolve("per-chunk.txt");

        assertEquals(0, run("--n", "100000", "--mode", "single", "--output", single.toString()));
        assertEquals(0, run("--n", "100000", "--mode", "pool", "--output", pool.toString()));
        assertEquals(0, run("--n", "100000", "--mode", "per-chunk", "--output", perChunk.toString()));

        List<String> expected = Files.readAllLines(single);
        assertEquals(9_592, expected.size());
        assertEquals("2", expected.get(0));
        assertEquals(expected, Files.readAllLines(pool));
        assertEquals(expected, Files.readAllLines(perChunk));
    }
}

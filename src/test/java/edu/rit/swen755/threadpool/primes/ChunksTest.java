package edu.rit.swen755.threadpool.primes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Acceptance tests for {@link Chunks#split(long, int)} (slice 2). Disabled until the slice lands.
 */
@Disabled("TODO(slice 2: Kobe) — enable when implementing")
class ChunksTest {

    @Test
    void splitsThousandIntoHundredWidthTenChunksCoveringRangeWithNoGaps() {
        List<Chunk> chunks = Chunks.split(1000, 100);

        assertEquals(100, chunks.size());
        assertEquals(1, chunks.get(0).first());
        assertEquals(1000, chunks.get(99).last());

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            assertEquals(i, chunk.index(), "chunk index should match its position");
            assertEquals(10, chunk.last() - chunk.first() + 1, "each chunk should have width 10");
            if (i > 0) {
                assertEquals(chunks.get(i - 1).last() + 1, chunk.first(),
                        "chunks should be contiguous with no gaps or overlaps");
            }
        }
    }

    @Test
    void lastChunkAbsorbsTheRemainder() {
        List<Chunk> chunks = Chunks.split(1005, 100);

        assertEquals(100, chunks.size());
        Chunk last = chunks.get(99);
        assertEquals(991, last.first());
        assertEquals(1005, last.last());
        assertEquals(15, last.last() - last.first() + 1, "the last chunk should hold the extra 5");
        assertEquals(10, chunks.get(0).last() - chunks.get(0).first() + 1,
                "earlier chunks keep the base width");
    }

    @Test
    void rejectsNonPositiveCount() {
        assertThrows(IllegalArgumentException.class, () -> Chunks.split(1000, 0));
    }

    @Test
    void rejectsNSmallerThanCount() {
        assertThrows(IllegalArgumentException.class, () -> Chunks.split(99, 100));
    }
}

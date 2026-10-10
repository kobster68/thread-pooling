package edu.rit.swen755.threadpool.primes;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Acceptance and boundary tests for {@link Chunks#split(long, int)}.
 */
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
        for (int i = 0; i < 99; i++) {
            assertEquals(new Chunk(i, i * 10L + 1, (i + 1L) * 10), chunks.get(i),
                    "every earlier chunk keeps the base width");
        }
    }

    @Test
    void rejectsNonPositiveCount() {
        assertThrows(IllegalArgumentException.class, () -> Chunks.split(1000, 0));
        assertThrows(IllegalArgumentException.class, () -> Chunks.split(1000, -1));
    }

    @Test
    void rejectsNSmallerThanCount() {
        assertThrows(IllegalArgumentException.class, () -> Chunks.split(99, 100));
        assertThrows(IllegalArgumentException.class, () -> Chunks.split(0, 1));
        assertThrows(IllegalArgumentException.class, () -> Chunks.split(-1, 1));
    }

    @Test
    void oneChunkCoversTheWholeRange() {
        assertEquals(List.of(new Chunk(0, 1, 1005)), Chunks.split(1005, 1));
        assertEquals(List.of(new Chunk(0, 1, 1)), Chunks.split(1, 1));
    }

    @Test
    void oneNumberPerChunkWhenNEqualsCount() {
        assertEquals(List.of(new Chunk(0, 1, 1), new Chunk(1, 2, 2),
                new Chunk(2, 3, 3)), Chunks.split(3, 3));
    }

    @Test
    void handlesTheLargestLongWithoutOverflow() {
        long width = Long.MAX_VALUE / 2;
        assertEquals(List.of(new Chunk(0, 1, width),
                new Chunk(1, width + 1, Long.MAX_VALUE)), Chunks.split(Long.MAX_VALUE, 2));
        assertEquals(List.of(new Chunk(0, 1, Long.MAX_VALUE)), Chunks.split(Long.MAX_VALUE, 1));
    }
}

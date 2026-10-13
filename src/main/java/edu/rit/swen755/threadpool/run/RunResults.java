package edu.rit.swen755.threadpool.run;

import edu.rit.swen755.threadpool.primes.ChunkResult;

import java.util.Arrays;
import java.util.List;

/**
 * Shares result-slot validation and ordered-list conversion across all execution strategies.
 */
final class RunResults {

    private RunResults() {
    }

    static List<ChunkResult> completedInOrder(ChunkResult[] resultSlots) {
        for (int i = 0; i < resultSlots.length; i++) {
            if (resultSlots[i] == null) {
                throw new IllegalStateException("result slot " + i + " was not filled");
            }
        }
        return List.copyOf(Arrays.asList(resultSlots));
    }
}

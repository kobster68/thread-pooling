package edu.rit.swen755.threadpool.pool;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance tests for {@link ThreadPool} (slice 1). Disabled until the slice lands.
 *
 * <p>The tasks sleep about 20 ms each so that one worker cannot drain the whole queue alone;
 * both workers of a two-worker pool are forced to take part. Assertions are made only after
 * {@code awaitTermination()}, and every test is bounded by a {@link Timeout} so a hang fails
 * the build rather than blocking it.
 */
@Disabled("TODO(slice 1: Godson) — enable when implementing")
class ThreadPoolTest {

    /** A task that records the worker that ran it after a short, pool-sharing sleep. */
    private static Runnable recordingTask(List<String> workerNames) {
        return () -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            workerNames.add(Thread.currentThread().getName());
        };
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void twoWorkersRunAllTwentyTasksAndAreBothReused() throws InterruptedException {
        ThreadPool pool = new ThreadPool(2);
        List<String> workerNames = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 20; i++) {
            pool.submit(recordingTask(workerNames));
        }
        pool.shutdown();
        pool.awaitTermination();

        assertEquals(20, workerNames.size(), "all 20 tasks should run");
        assertEquals(2, pool.threadsCreated(), "the pool should never grow beyond its size");

        Set<String> distinct = new HashSet<>(workerNames);
        assertEquals(Set.of("pool-worker-0", "pool-worker-1"), distinct,
                "both workers should be reused across the 20 tasks");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void everySubmittedTaskHasRunAfterShutdownAndAwait() throws InterruptedException {
        ThreadPool pool = new ThreadPool(3);
        List<String> ran = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 30; i++) {
            pool.submit(() -> ran.add(Thread.currentThread().getName()));
        }
        pool.shutdown();
        pool.awaitTermination();

        assertEquals(30, ran.size(), "queued work must finish before the workers exit");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void aThrowingTaskDoesNotKillItsWorker() throws InterruptedException {
        ThreadPool pool = new ThreadPool(2);
        List<String> workerNames = Collections.synchronizedList(new ArrayList<>());

        pool.submit(() -> {
            throw new RuntimeException("boom");
        });
        for (int i = 0; i < 20; i++) {
            pool.submit(recordingTask(workerNames));
        }
        pool.shutdown();
        pool.awaitTermination();

        assertEquals(20, workerNames.size(), "the 20 normal tasks still run after one throws");
        assertEquals(2, pool.threadsCreated(), "a throwing task must not shrink or grow the pool");
        assertEquals(Set.of("pool-worker-0", "pool-worker-1"), new HashSet<>(workerNames),
                "both workers keep running after a task throws");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void submitAfterShutdownThrowsIllegalState() {
        ThreadPool pool = new ThreadPool(2);
        pool.shutdown();

        assertThrows(IllegalStateException.class, () -> pool.submit(() -> { }));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void awaitTerminationWithoutShutdownThrowsIllegalState() {
        ThreadPool pool = new ThreadPool(2);

        assertThrows(IllegalStateException.class, pool::awaitTermination,
                "awaitTermination must refuse to block forever when shutdown was never called");

        pool.shutdown();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shutdownIsIdempotentAndAwaitReturns() throws InterruptedException {
        ThreadPool pool = new ThreadPool(2);
        List<String> ran = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 4; i++) {
            pool.submit(() -> ran.add("x"));
        }

        pool.shutdown();
        pool.shutdown();
        pool.awaitTermination();

        assertEquals(4, ran.size(), "calling shutdown twice is harmless and loses no work");
        assertTrue(pool.threadsCreated() == 2);
    }
}

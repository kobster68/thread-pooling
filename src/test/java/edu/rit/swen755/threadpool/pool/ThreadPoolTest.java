package edu.rit.swen755.threadpool.pool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
 * Tests for {@link ThreadPool}: worker reuse, draining on shutdown, surviving a failing task,
 * the shutdown and argument rules, worker naming, and stopping a worker on interrupt.
 *
 * <p>The tasks sleep about 20 ms each so that one worker cannot drain the whole queue alone;
 * both workers of a two-worker pool are forced to take part. Assertions are made only after
 * {@code awaitTermination()}, and every test is bounded by a {@link Timeout} so a hang fails
 * the build rather than blocking it.
 */
class ThreadPoolTest {

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

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

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void aFailingTaskPrintsOneTaskFailedLineToStandardError() throws InterruptedException {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            ThreadPool pool = new ThreadPool(1);
            pool.submit(() -> {
                throw new IllegalStateException("boom");
            });
            pool.shutdown();
            pool.awaitTermination();
        } finally {
            System.setErr(originalErr);
        }

        List<String> failureLines = captured.toString(StandardCharsets.UTF_8).lines()
                .filter(line -> line.contains("task failed"))
                .toList();
        assertEquals(1, failureLines.size(), "exactly one line per failed task: " + failureLines);
        String line = failureLines.get(0);
        assertTrue(line.startsWith("pool-worker-0: task failed: "), line);
        assertTrue(line.contains("boom"), line);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void submitRejectsNull() throws InterruptedException {
        ThreadPool pool = new ThreadPool(1);
        try {
            assertThrows(NullPointerException.class, () -> pool.submit(null));
        } finally {
            pool.shutdown();
            pool.awaitTermination();
        }
    }

    @Test
    void rejectsAPoolWithNoWorkers() {
        assertThrows(IllegalArgumentException.class, () -> new ThreadPool(0));
        assertThrows(IllegalArgumentException.class, () -> new ThreadPool(-1));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void aTenWorkerPoolRunsAHundredTasksOnOnlyItsOwnNamedWorkers() throws InterruptedException {
        ThreadPool pool = new ThreadPool(10);
        List<String> workerNames = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 100; i++) {
            pool.submit(() -> workerNames.add(Thread.currentThread().getName()));
        }
        pool.shutdown();
        pool.awaitTermination();

        assertEquals(100, workerNames.size(), "all 100 tasks should run");
        assertEquals(10, pool.threadsCreated(), "a pool of 10 creates exactly 10 threads");
        Set<String> allowed = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            allowed.add("pool-worker-" + i);
        }
        assertTrue(allowed.containsAll(workerNames), "only the pool's own workers ran tasks: " + new HashSet<>(workerNames));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void anInterruptedWorkerStopsAfterItsCurrentTaskAndTheRestStillRun() throws InterruptedException {
        ThreadPool pool = new ThreadPool(2);
        List<String> interrupted = Collections.synchronizedList(new ArrayList<>());
        List<String> laterWorkers = Collections.synchronizedList(new ArrayList<>());

        // the first task holds its worker for 100 ms, then interrupts it; the later tasks take
        // 20 ms each, so work is still queued when the interrupted worker comes back for more
        pool.submit(() -> {
            interrupted.add(Thread.currentThread().getName());
            sleepQuietly(100);
            Thread.currentThread().interrupt();
        });
        for (int i = 0; i < 10; i++) {
            pool.submit(() -> {
                sleepQuietly(20);
                laterWorkers.add(Thread.currentThread().getName());
            });
        }
        pool.shutdown();
        pool.awaitTermination();

        assertEquals(10, laterWorkers.size(), "the other worker still runs every remaining task");
        assertTrue(!laterWorkers.contains(interrupted.get(0)),
                "the interrupted worker must stop after its current task, but it also ran: " + laterWorkers);
    }
}

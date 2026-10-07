package edu.rit.swen755.threadpool.pool;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance tests for {@link TaskQueue} (slice 1). Disabled until the slice lands.
 */
@Disabled("TODO(slice 1: Godson) — enable when implementing")
class TaskQueueTest {

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void takeBlocksUntilAnotherThreadPuts() throws InterruptedException {
        TaskQueue queue = new TaskQueue();
        Runnable marker = () -> { };
        List<Runnable> taken = Collections.synchronizedList(new ArrayList<>());

        Thread consumer = new Thread(() -> {
            try {
                taken.add(queue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();

        Thread.sleep(150);
        assertTrue(taken.isEmpty(), "take() should block while the queue is empty");

        queue.put(marker);
        consumer.join();

        assertEquals(1, taken.size(), "take() should return once a task is put");
        assertSame(marker, taken.get(0), "take() should return the task that was put");
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void handsTasksBackInFirstInFirstOutOrder() throws InterruptedException {
        TaskQueue queue = new TaskQueue();
        Runnable first = () -> { };
        Runnable second = () -> { };
        Runnable third = () -> { };

        queue.put(first);
        queue.put(second);
        queue.put(third);

        assertSame(first, queue.take());
        assertSame(second, queue.take());
        assertSame(third, queue.take());
    }
}

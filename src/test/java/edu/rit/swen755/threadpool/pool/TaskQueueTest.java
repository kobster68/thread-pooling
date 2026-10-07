package edu.rit.swen755.threadpool.pool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TaskQueue}: FIFO order, blocking take, null rejection, and a two-consumer
 * hand-off where every item must come out exactly once.
 */
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

    @Test
    void putRejectsNull() {
        TaskQueue queue = new TaskQueue();

        assertThrows(NullPointerException.class, () -> queue.put(null));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void twoConsumersTakeEveryItemExactlyOnce() throws InterruptedException {
        TaskQueue queue = new TaskQueue();
        Runnable stop = () -> { };
        List<Runnable> taken = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> consumerFailures = Collections.synchronizedList(new ArrayList<>());

        Runnable consume = () -> {
            try {
                for (Runnable item = queue.take(); item != stop; item = queue.take()) {
                    taken.add(item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                consumerFailures.add(e);
            }
        };
        Thread first = new Thread(consume);
        Thread second = new Thread(consume);
        first.start();
        second.start();

        List<Runnable> items = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            // each item must be a distinct object; a lambda that captures nothing is a single
            // shared instance, which would make the exactly-once check below meaningless
            Runnable item = new NumberedItem(i);
            items.add(item);
            queue.put(item);
            if (i % 50 == 0) {
                // let both consumers drain the queue and start waiting, so each put wakes two waiters
                Thread.sleep(2);
            }
        }
        queue.put(stop);
        queue.put(stop);
        first.join();
        second.join();

        assertTrue(consumerFailures.isEmpty(), "a consumer failed: " + consumerFailures);
        assertEquals(1_000, taken.size(), "no item may be lost or taken twice");
        Set<Runnable> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
        distinct.addAll(taken);
        assertEquals(new HashSet<>(items), new HashSet<>(distinct), "every item put is taken exactly once");
    }

    /** A distinct no-op task, so identity checks can tell the items apart. */
    private record NumberedItem(int number) implements Runnable {
        @Override
        public void run() {
        }
    }
}

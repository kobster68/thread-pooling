package edu.rit.swen755.threadpool.pool;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A hand-built, unbounded, FIFO hand-off between the submitter and the pool's workers.
 *
 * <p>The queue is built only from {@code synchronized} and {@code wait()}/{@code notify()};
 * it uses no executors, locks, atomics, or library queues from the JDK's concurrency utilities.
 * {@link #take()} waits in a
 * {@code while (empty)} loop so that neither a lost wakeup nor a spurious wakeup can let a
 * worker return without a task, and {@link #put(Runnable)} calls {@code notify()} to wake one
 * waiting worker for the one task it added. The queue is unbounded because the program submits a fixed, small
 * number of tasks, so bounding it would add nothing here.
 *
 */
public final class TaskQueue {

    private final Deque<Runnable> tasks = new ArrayDeque<>();

    /**
     * Creates an empty queue.
     */
    public TaskQueue() {
    }

    /**
     * Adds a task to the back of the queue and wakes one waiting worker.
     *
     * <p>Tasks are handed to workers in first-in, first-out order.
     *
     * @param task the task to enqueue
     * @throws NullPointerException if {@code task} is null, so null never enters the queue
     */
    public synchronized void put(Runnable task) {
        tasks.addLast(task);
        // one item was added, so one waiter is enough: every waiter waits for the same thing
        // (a non-empty queue) and any of them can take it. Waking all of them would just send
        // the rest straight back to wait().
        notify();
    }

    /**
     * Removes and returns the task at the front of the queue, blocking until one is available.
     *
     * <p>If the queue is empty the caller waits (without spinning) in a guarded loop until a
     * {@link #put(Runnable)} makes a task available. A worker that is interrupted while waiting
     * sees the {@link InterruptedException} propagate, which is how it leaves its loop.
     *
     * @return the next task in FIFO order
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public synchronized Runnable take() throws InterruptedException {
        while (tasks.isEmpty()) {
            wait();
        }
        return tasks.removeFirst();
    }
}

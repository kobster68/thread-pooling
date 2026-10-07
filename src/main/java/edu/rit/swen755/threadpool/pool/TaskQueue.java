package edu.rit.swen755.threadpool.pool;

/**
 * A hand-built, unbounded, FIFO hand-off between the submitter and the pool's workers.
 *
 * <p>The queue is built only from {@code synchronized} and {@code wait()}/{@code notifyAll()};
 * it uses no executors, locks, atomics, or library queues from the JDK's concurrency utilities.
 * {@link #take()} waits in a
 * {@code while (empty)} loop so that neither a lost wakeup nor a spurious wakeup can let a
 * worker return without a task, and {@link #put(Runnable)} calls {@code notifyAll()} to wake
 * any waiting workers. The queue is unbounded because the program submits a fixed, small
 * number of tasks, so bounding it would add nothing here.
 *
 * <p>This is a stub until slice 1 lands.
 */
public final class TaskQueue {

    /**
     * Creates an empty queue.
     */
    public TaskQueue() {
        throw new UnsupportedOperationException("TODO(slice 1: Godson)");
    }

    /**
     * Adds a task to the back of the queue and wakes any waiting worker.
     *
     * <p>Tasks are handed to workers in first-in, first-out order.
     *
     * @param task the task to enqueue
     */
    public void put(Runnable task) {
        throw new UnsupportedOperationException("TODO(slice 1: Godson)");
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
    public Runnable take() throws InterruptedException {
        throw new UnsupportedOperationException("TODO(slice 1: Godson)");
    }
}

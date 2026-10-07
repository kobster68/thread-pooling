package edu.rit.swen755.threadpool.pool;

/**
 * A fixed pool of worker threads that take tasks from a shared {@link TaskQueue} and reuse
 * themselves across tasks.
 *
 * <p>This is the tactic itself. All {@code size} workers are created and started once, in the
 * constructor, and named {@code pool-worker-0} through {@code pool-worker-(size-1)}. A worker
 * loops: take a task, run it, take the next. A worker that finishes one task goes straight back
 * for another rather than being destroyed and recreated, which is the reuse the assignment
 * requires. The pool is built only from {@link Thread}, {@link Runnable}, {@code synchronized},
 * {@code wait()}/{@code notifyAll()} (inside {@link TaskQueue}) and {@link Thread#join()}; it uses
 * no executors, locks, atomics, or library queues from the JDK's concurrency utilities.
 *
 * <p>This is a stub until slice 1 lands.
 */
public final class ThreadPool {

    /**
     * Creates and starts {@code size} worker threads.
     *
     * <p>Each worker is named {@code pool-worker-i} for {@code i} in {@code [0, size)} and begins
     * taking tasks immediately. A task that throws is logged and does not kill its worker, so the
     * pool keeps its size and keeps reusing threads.
     *
     * @param size the number of worker threads to create
     */
    public ThreadPool(int size) {
        throw new UnsupportedOperationException("TODO(slice 1: Godson)");
    }

    /**
     * Enqueues a task for a worker to run, returning immediately.
     *
     * <p>The shutdown-flag check and the enqueue happen together under the pool's own monitor
     * (both this method and {@link #shutdown()} synchronize on the pool), so a task can never slip
     * in after the flag is set and land behind the shutdown markers: either it is enqueued before
     * shutdown and runs, or the flag is already set and this method throws.
     *
     * @param task the task to run
     * @throws IllegalStateException if the pool has already been shut down, so that a task can
     *                               never be lost behind the shutdown markers
     */
    public void submit(Runnable task) {
        throw new UnsupportedOperationException("TODO(slice 1: Godson)");
    }

    /**
     * Begins an orderly shutdown: already-submitted tasks finish, then the workers exit.
     *
     * <p>Setting the shutdown flag and enqueuing one shutdown marker per worker happen together
     * under the pool's own monitor (the same monitor {@link #submit(Runnable)} holds), so the
     * markers are placed behind every task that was accepted and ahead of any that is rejected.
     * FIFO order then guarantees all real work drains before any worker sees its marker and exits.
     * This method is idempotent: calling it more than once sets the flag and enqueues the markers
     * only on the first call and has no further effect.
     */
    public void shutdown() {
        throw new UnsupportedOperationException("TODO(slice 1: Godson)");
    }

    /**
     * Blocks until every worker has exited.
     *
     * <p>{@link #shutdown()} must have been called first; if it has not, this method throws
     * {@link IllegalStateException} immediately rather than blocking forever (no markers would ever
     * be enqueued, so the workers would never exit). When the precondition holds, this method joins
     * each worker thread and returns only once all of them have finished. Because it joins the
     * workers, the happens-before guarantee of {@link Thread#join()} makes every result the
     * workers wrote visible to the caller afterwards.
     *
     * @throws IllegalStateException if {@link #shutdown()} has not been called
     * @throws InterruptedException  if the calling thread is interrupted while waiting
     */
    public void awaitTermination() throws InterruptedException {
        throw new UnsupportedOperationException("TODO(slice 1: Godson)");
    }

    /**
     * Returns the number of worker threads the pool created, which is the {@code size} passed to
     * the constructor and never changes.
     *
     * @return the fixed worker-thread count
     */
    public int threadsCreated() {
        throw new UnsupportedOperationException("TODO(slice 1: Godson)");
    }
}

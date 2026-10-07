package edu.rit.swen755.threadpool.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
 */
public final class ThreadPool {

    /** Queued once per worker by {@link #shutdown()}; a worker that takes it exits its loop. */
    private static final Runnable SHUTDOWN_MARKER = () -> { };

    private final TaskQueue queue = new TaskQueue();
    private final List<Thread> workers = new ArrayList<>();

    /** Guarded by {@code this}; once true, it never goes back to false. */
    private boolean shutdown;

    /**
     * Creates and starts {@code size} worker threads.
     *
     * <p>Each worker is named {@code pool-worker-i} for {@code i} in {@code [0, size)} and begins
     * taking tasks immediately. A task that throws is logged and does not kill its worker, so the
     * pool keeps its size and keeps reusing threads.
     *
     * @param size the number of worker threads to create
     * @throws IllegalArgumentException if {@code size} is zero or negative
     */
    public ThreadPool(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("pool size must be positive, was " + size);
        }
        for (int i = 0; i < size; i++) {
            Thread worker = new Thread(new Worker(queue), "pool-worker-" + i);
            workers.add(worker);
            worker.start();
        }
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
     * @throws NullPointerException  if {@code task} is null
     * @throws IllegalStateException if the pool has already been shut down, so that a task can
     *                               never be lost behind the shutdown markers
     */
    public synchronized void submit(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (shutdown) {
            throw new IllegalStateException("cannot submit after shutdown()");
        }
        queue.put(task);
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
    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        for (int i = 0; i < workers.size(); i++) {
            queue.put(SHUTDOWN_MARKER);
        }
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
        synchronized (this) {
            if (!shutdown) {
                throw new IllegalStateException("awaitTermination() called before shutdown()");
            }
        }
        // join outside the lock so waiting here never blocks submit() or shutdown() callers
        for (Thread worker : workers) {
            worker.join();
        }
    }

    /**
     * Returns the number of worker threads the pool created, which is the {@code size} passed to
     * the constructor and never changes.
     *
     * @return the fixed worker-thread count
     */
    public int threadsCreated() {
        return workers.size();
    }

    /** One pool thread's loop: take a task, run it, and go back for the next until shut down. */
    private static final class Worker implements Runnable {

        private final TaskQueue queue;

        Worker(TaskQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Runnable task = queue.take();
                    if (task == SHUTDOWN_MARKER) {
                        return;
                    }
                    try {
                        task.run();
                    } catch (Throwable failure) {
                        // report it and keep looping: a failing task must not kill its worker,
                        // or the pool would quietly shrink
                        System.err.println(Thread.currentThread().getName() + ": task failed: " + failure);
                    }
                    // an interrupt means "stop after the current task", even when more work is
                    // queued (take() only notices an interrupt when it has to wait)
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                // interrupted while waiting for work: leave the loop and let the thread end
            }
        }
    }
}

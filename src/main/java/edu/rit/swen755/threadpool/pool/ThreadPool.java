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
 * {@code wait()}/{@code notify()} (inside {@link TaskQueue}) and {@link Thread#join()}; it uses
 * no executors, locks, atomics, or library queues from the JDK's concurrency utilities.
 *
 * <p>Workers are ordinary (non-daemon) threads and only exit through {@link #shutdown()}, so the
 * JVM cannot end while a pool is still open. Callers should shut the pool down in a
 * {@code finally} block:
 *
 * <pre>{@code
 * ThreadPool pool = new ThreadPool(10);
 * try {
 *     // submit tasks
 * } finally {
 *     pool.shutdown();
 * }
 * pool.awaitTermination();
 * }</pre>
 */
public final class ThreadPool {

    /** Queued once per worker by {@link #shutdown()}; a worker that takes it exits its loop. */
    private static final Runnable SHUTDOWN_MARKER = () -> { };

    private final TaskQueue queue = new TaskQueue();
    private final List<Thread> workers = new ArrayList<>();

    /** Private, so outside code that happens to lock the pool object can't interfere. */
    private final Object lock = new Object();

    /** Guarded by {@code lock}; once true, it never goes back to false. */
    private boolean shutdown;

    /**
     * Creates and starts {@code size} worker threads.
     *
     * <p>Each worker is named {@code pool-worker-i} for {@code i} in {@code [0, size)} and begins
     * taking tasks immediately. A task that throws an exception is logged and does not kill its
     * worker, so the pool keeps reusing the same threads. If starting a worker fails partway, the
     * workers already started are told to exit before the failure is rethrown, so none are left
     * waiting forever.
     *
     * @param size the number of worker threads to create
     * @throws IllegalArgumentException if {@code size} is zero or negative
     */
    public ThreadPool(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("pool size must be positive, was " + size);
        }
        try {
            for (int i = 0; i < size; i++) {
                Thread worker = new Thread(new Worker(queue), "pool-worker-" + i);
                // list it before starting it, so cleanup can never miss a running thread
                workers.add(worker);
                worker.start();
            }
        } catch (RuntimeException | Error failure) {
            // no caller can reach this half-built pool to shut it down, so do it here. One marker
            // per listed thread: if the last one never started, its marker just goes unused.
            for (int i = 0; i < workers.size(); i++) {
                queue.put(SHUTDOWN_MARKER);
            }
            throw failure;
        }
    }

    /**
     * Enqueues a task for a worker to run, returning immediately.
     *
     * <p>The shutdown-flag check and the enqueue happen together under the pool's private lock
     * (both this method and {@link #shutdown()} hold it), so a task can never slip
     * in after the flag is set and land behind the shutdown markers: either it is enqueued before
     * shutdown and runs, or the flag is already set and this method throws.
     *
     * @param task the task to run
     * @throws NullPointerException  if {@code task} is null
     * @throws IllegalStateException if the pool has already been shut down, so that a task can
     *                               never be lost behind the shutdown markers
     */
    public void submit(Runnable task) {
        Objects.requireNonNull(task, "task");
        synchronized (lock) {
            if (shutdown) {
                throw new IllegalStateException("cannot submit after shutdown()");
            }
            queue.put(task);
        }
    }

    /**
     * Begins an orderly shutdown: already-submitted tasks finish, then the workers exit.
     *
     * <p>The one exception is interruption: a worker that is interrupted stops after its current
     * task and does not come back, so if every worker is interrupted, tasks still in the queue
     * never run. (The execution strategies detect this as an empty result slot.)
     *
     * <p>Setting the shutdown flag and enqueuing one shutdown marker per worker happen together
     * under the pool's private lock (the same lock {@link #submit(Runnable)} holds), so the
     * markers are placed behind every task that was accepted and ahead of any that is rejected.
     * FIFO order then guarantees all real work drains before any worker sees its marker and exits.
     * This method is idempotent: calling it more than once sets the flag and enqueues the markers
     * only on the first call and has no further effect.
     */
    public void shutdown() {
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            shutdown = true;
            for (int i = 0; i < workers.size(); i++) {
                queue.put(SHUTDOWN_MARKER);
            }
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
     * <p>It also throws {@link IllegalStateException} when called from one of the pool's own
     * workers, since a worker waiting for itself to finish would wait forever.
     *
     * @throws IllegalStateException if {@link #shutdown()} has not been called, or if the caller
     *                               is one of this pool's workers
     * @throws InterruptedException  if the calling thread is interrupted while waiting
     */
    public void awaitTermination() throws InterruptedException {
        synchronized (lock) {
            if (!shutdown) {
                throw new IllegalStateException("awaitTermination() called before shutdown()");
            }
        }
        if (workers.contains(Thread.currentThread())) {
            throw new IllegalStateException("awaitTermination() called from a pool worker");
        }
        // join outside the lock so waiting here never blocks submit() or shutdown() callers
        for (Thread worker : workers) {
            worker.join();
        }
    }

    /**
     * Returns the number of worker threads the pool created, which is the {@code size} passed to
     * the constructor. It counts threads started, not threads still alive: a worker that was
     * interrupted, or hit an {@link Error}, has ended but is still counted.
     *
     * @return the number of worker threads started
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
                    } catch (Exception failure) {
                        // report it and keep looping: a failing task must not kill its worker,
                        // or the pool would quietly shrink. Errors (out of memory, stack
                        // overflow) are not caught: the JVM is in trouble, so the worker stops.
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

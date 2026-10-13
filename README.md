# thread-pooling

SWEN-755 Assignment 3: an implementation of the Thread Pooling tactic in Java.

**Repository:** https://github.com/kobster68/thread-pooling

## Summary

This program is a worked example of the Thread Pooling tactic. The domain is numeric
computation: it finds and counts the prime numbers in the range [1, N]. The range is split
into 100 equal-width chunks, and a pool of 10 worker threads, built by hand, pulls chunks off
a shared queue until they are all done. A worker that finishes one chunk goes straight back for
the next, so 10 threads handle all 100 chunks and each thread is reused many times rather than
created once per chunk. The same workload can also be run on a single thread or on one thread
per chunk, so the pool can be measured against a serial baseline and against a no-pooling
alternative. The tactic is covered in *Software Architecture in Practice*, 4th ed. (Bass,
Clements, and Kazman), Chapter 9, as Introduce Concurrency (p. 141) and Schedule Resources
(p. 142), with the thread pool itself as a managed resource (p. 138).

## Prerequisites

- JDK 21 (the build targets Java 21).
- Maven 3.9 or newer.

Java and Maven must be on your `PATH`.

## Build and run

The short path uses the run script, which builds the runnable jar and then runs it, forwarding
any arguments to the program:

```bash
scripts/run.sh                                 # compare mode at the default N
scripts/run.sh --n 1000000 --mode pool
scripts/run.sh --mode single --output primes.txt
```

If you do not have a POSIX shell, build and run with plain Maven and Java instead:

```bash
mvn -q -DskipTests package
java -jar target/thread-pooling.jar
```

The build produces `target/thread-pooling.jar`. To compile and run the tests:

```bash
mvn verify
```

## Libraries and build tools

| Component | Use |
| --- | --- |
| Java / JDK 21 | The whole runtime: records, `Thread`, `Runnable`, `synchronized`, `wait()`/`notify()`, and `join()` |
| JUnit Jupiter 5.14.4 | Unit and concurrency tests (test scope only) |
| Maven 3.9+ | Build and test runner |
| Maven Compiler 3.14.0 / Surefire 3.6.0 / Jar 3.4.2 | Compilation, test execution, and the runnable jar |
| PlantUML | Optional rendering of the UML source files under `uml/` |

At runtime the program depends on the JDK standard library only. There are no third-party runtime
dependencies.

## No library thread pooling

The shipped program builds its own thread pool. Nothing from `java.util.concurrent` is used under
`src/main`: no executors, no `ForkJoinPool`, no parallel streams, no async `CompletableFuture`, no
atomics, no locks, and no library queues. The pool, the worker loop, and the FIFO work queue are
written by hand from `Thread`, `Runnable`, `synchronized`, `wait()`/`notify()`, and `join()`.
The assignment allows existing libraries for everything except thread pooling itself, so the pool
must be built from scratch. A continuous-integration check greps `src/main` for
`java.util.concurrent` and for parallel streams (`parallelStream(` and `.parallel()`), and fails
the build if it finds any of them.

## Team and slice ownership

The work is split into four slices built in parallel against a shared contract of compiling stubs.

| Slice | Area | Owner |
| --- | --- | --- |
| 1 | Thread pool and UML diagrams | Godson Umoren |
| 2 | Prime workload (chunking and the prime range task) | Kobe LaPrade |
| 3 | Execution strategies and compare | Chase Michael |
| 4 | Command-line interface and README | Mallikarjuna |

## Changing the shared contract

The records and public signatures in the `pool`, `primes`, and `run` packages are a frozen
contract. Any change to one of these shared classes goes through its own small pull request,
reviewed by the slice 1 owner, so the seam between slices does not drift while four people are
working at once.

## Usage and command-line options

```
java -jar target/thread-pooling.jar [--n <N>] [--mode <mode>] [--output <file>]
```

| Option | Default | Meaning |
| --- | --- | --- |
| `--n <N>` | `50000000` | Upper bound of the range [1, N]. Must be a whole number of at least 100, so that each of the 100 chunks holds at least one integer. |
| `--mode <mode>` | `compare` | Which strategy to run: `single`, `pool`, `per-chunk`, or `compare`. |
| `--output <file>` | none | Write every prime in [1, N] to the file, one per line, in ascending order. Allowed with `single`, `pool`, and `per-chunk`, but not with `compare`. |

The pool size (10) and the chunk count (100) are fixed constants, not options.

| Mode | What it runs | What it prints |
| --- | --- | --- |
| `single` | All 100 chunks on the main thread | A one-line summary |
| `pool` | The hand-built pool of 10 workers | A one-line summary, then which worker ran each of the 100 chunks |
| `per-chunk` | 100 threads, one per chunk | A one-line summary |
| `compare` | A warm-up, then all three in turn | A progress line per strategy, then the comparison table |

Every mode finishes with a check of its count against the published number of primes up to N.
Published values are built in for N = 10^6, 10^7, 5x10^7, and 10^8; for any other N the program
says there is no value to check against.

A short run in `single` mode looks like this (the time varies by machine):

```
$ scripts/run.sh --n 1000000 --mode single
thread-pooling tactic (SWEN-755): primes in [1, 1000000], 100 chunks, pool of 10 worker threads
single: 78498 primes in 39.234 ms on 1 thread(s)
known-pi check: OK, pi(1000000) = 78498
```

Exit codes:

| Code | Meaning |
| --- | --- |
| 0 | The run succeeded. |
| 1 | Runtime failure: the output file could not be written, a task failed, or the count did not match the published value. |
| 2 | Usage error: an unknown option or mode, a missing value, a non-numeric or too-small `--n`, or `--output` with `compare`. The usage text is printed to standard error. |

## How the tactic maps to the course

Thread pooling is a performance tactic on the "manage resources" side of the catalogue in
*Software Architecture in Practice*, 4th ed., Chapter 9. The program applies three ideas from that
chapter and deliberately leaves one out.

| Idea in the book | Where it is in the program |
| --- | --- |
| Introduce Concurrency (p. 141) | The 100 chunks are independent, so `ThreadPoolStrategy` runs up to 10 of them at once instead of one after another. |
| Schedule Resources (p. 142) | `TaskQueue` is a first-in, first-out queue. A free worker always takes the oldest waiting chunk, so the scheduling policy is FIFO. |
| The thread pool as a managed resource (p. 138) | `ThreadPool` creates its 10 workers once and keeps them for the whole run. The number of threads is fixed by the pool, not by the amount of work. |
| Bound Queue Sizes (related, not applied) | The queue is unbounded. All 100 chunks are submitted up front and the number is known, so there is nothing to push back on. A server taking open-ended requests would need a bound. |

The single-thread and thread-per-chunk strategies are not part of the tactic. They are the two
alternatives it is measured against: no concurrency at all, and concurrency with no pooling.

## Quality attribute scenario

The quality attribute is performance.

| Part | Value |
| --- | --- |
| Source | A user at the command line. |
| Stimulus | A request to count the primes in [1, 50,000,000], which arrives as 100 chunks of work at once. |
| Artifact | The prime-counting program: the thread pool, its work queue, and the prime range tasks. |
| Environment | Normal operation on one multi-core machine, with no other heavy load. |
| Response | The pool's 10 workers take chunks from the queue in FIFO order until all 100 are done, and the program reports the total, which matches the published count. |
| Response measure | All 100 chunks finish on exactly 10 threads, with every worker handling more than one chunk, and the wall time is lower than the single-thread run on the same machine. |

## Results

The comparison runs the same 100 chunks serially on the calling thread, through a reusable
10-worker thread pool, and on 100 one-shot threads (one per chunk). After an untimed warm-up over
the first million integers, it measures each complete run and reports wall time, participating
threads, and chunks handled per thread; the chunk-to-worker map makes reuse by the pool visible.

### Measured runs

These numbers are from one machine: an Apple M4 with 10 cores (4 performance and 6 efficiency),
macOS 15.8, OpenJDK 21.0.12. Each command was run three times. The output shown is the first run,
and the text gives the range across all three.

Compare at the default N of 50,000,000 (`scripts/run.sh`):

```
thread-pooling tactic (SWEN-755): primes in [1, 50000000], 100 chunks, pool of 10 worker threads
running single...
running pool...
running per-chunk...
available processors: 10
Strategy     | Wall time (ms) | Threads used  | Chunks per thread
-------------|--------------|---------------|------------------
single       |     8183.747 | 1             | main=100
pool         |     1880.695 | 10            | pool-worker-0=10, pool-worker-1=9, pool-worker-2=9, pool-worker-3=10, pool-worker-4=10, pool-worker-5=10, pool-worker-6=11, pool-worker-7=9, pool-worker-8=11, pool-worker-9=11
per-chunk    |     1875.125 | 100           | 100 threads × 1 chunk
all three strategies found 3001134 primes
known-pi check: OK, pi(50000000) = 3001134
```

Compare at N = 1,000,000 (`scripts/run.sh --n 1000000`):

```
thread-pooling tactic (SWEN-755): primes in [1, 1000000], 100 chunks, pool of 10 worker threads
running single...
running pool...
running per-chunk...
available processors: 10
Strategy     | Wall time (ms) | Threads used  | Chunks per thread
-------------|--------------|---------------|------------------
single       |       36.131 | 1             | main=100
pool         |        7.834 | 10            | pool-worker-0=19, pool-worker-1=4, pool-worker-2=4, pool-worker-3=4, pool-worker-4=4, pool-worker-5=4, pool-worker-6=19, pool-worker-7=4, pool-worker-8=19, pool-worker-9=19
per-chunk    |        8.841 | 100           | 100 threads × 1 chunk
all three strategies found 78498 primes
known-pi check: OK, pi(1000000) = 78498
```

| N | single (ms) | pool (ms) | per-chunk (ms) |
| --- | --- | --- | --- |
| 50,000,000 | 8,184 to 8,499 | 1,881 to 1,919 | 1,865 to 1,897 |
| 1,000,000 | 36.1 to 36.3 | 7.8 to 8.0 | 8.6 to 8.8 |

### Evidence of thread reuse

The start and end of the chunk-to-worker map from `scripts/run.sh --mode pool` at the default N:

```
pool: 3001134 primes in 2002.100 ms on 10 thread(s)
chunk-0 -> pool-worker-0
chunk-1 -> pool-worker-8
chunk-2 -> pool-worker-9
chunk-3 -> pool-worker-7
chunk-4 -> pool-worker-6
chunk-5 -> pool-worker-5
chunk-6 -> pool-worker-4
chunk-7 -> pool-worker-3
chunk-8 -> pool-worker-2
chunk-9 -> pool-worker-1
chunk-10 -> pool-worker-0
chunk-11 -> pool-worker-8
...
chunk-98 -> pool-worker-3
chunk-99 -> pool-worker-0
known-pi check: OK, pi(50000000) = 3001134
```

The first ten chunks go to ten different workers. Chunk 10 then goes back to `pool-worker-0`, the
same thread that ran chunk 0, and `pool-worker-0` is still taking work at chunk 99. In this run
each of the 10 workers handled exactly 10 of the 100 chunks. No thread was created after the pool
started.

### What the numbers show

The pool is about 4.4 times faster than the single thread at the default N, and about 4.6 times
faster at N = 1,000,000. It is not 10 times faster with 10 workers, most likely because only 4 of
this machine's 10 cores are performance cores.

The pool does not beat thread-per-chunk on raw time. At the default N, thread-per-chunk was
slightly faster in all three runs, by between 0.3% and 1.9%, which is within the run-to-run
spread. Creating 100 threads costs very little next to chunks that each take around 80 ms of
work. At N = 1,000,000, where a chunk takes well under a millisecond, the pool was about 10%
faster in all three runs, because the cost of creating a thread per chunk is no longer hidden. On
a machine with more than 10 cores, thread-per-chunk could pull ahead, since it can use every core
and the pool is capped at 10.

So the pool's payoff here is not speed over the no-pooling alternative. It is that the thread
count is bounded at 10 however many chunks there are, and that no thread is created per task.
With 100 chunks, 100 threads are affordable. With 100,000 chunks they would not be, and the pool
would still use 10.

The comparison is also not a clean test of reuse alone. The two concurrent strategies differ in
two ways at once: the pool reuses its threads and has 10 of them alive, while thread-per-chunk
creates each thread once and has up to 100 alive. The timings mix those two effects and cannot
separate them.

## Trade-offs

A fixed pool of 10 is a deliberate limit, and it costs something on both sides. On a machine with
more than 10 cores the pool leaves cores idle, and thread-per-chunk can finish sooner because it
uses all of them. On a machine with fewer than 10 cores the extra workers only add context
switching. A pool sized from the core count would fit each machine better, but the assignment
fixes the size at 10.

The work queue is unbounded, so memory grows with the number of waiting tasks. That is harmless
here, where 100 small task objects are queued once, and it would be a real risk in a system that
accepts work faster than it can finish it.

Concurrency also costs simplicity. The single-thread strategy is a plain loop. The pool needs a
synchronized queue, `wait()`/`notify()` signalling, an orderly shutdown, and one result slot per
chunk so that workers never write to the same place, and all of that has to be tested for races
and lost wake-ups. That is the price paid for the shorter wall time.

## Design (UML)

The class diagram shows the three packages (`pool`, `primes`, `run`) and how they fit together. The sequence diagram walks through one pool run: workers created once, one task per chunk, each worker going back to the queue for the next task, and shutdown letting queued work finish. The narrative and course mapping are in [uml/pool/README.md](uml/pool/README.md).

- Class diagram: [thread-pooling-class.png](uml/pool/thread-pooling-class.png) ([source](uml/pool/thread-pooling-class.puml))
- Sequence diagram: [thread-pooling-sequence.png](uml/pool/thread-pooling-sequence.png) ([source](uml/pool/thread-pooling-sequence.puml), [text version](uml/pool/thread-pooling-sequence.utxt))

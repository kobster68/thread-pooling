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

<!-- TODO(slice 4: Mallikarjuna) -->

## How the tactic maps to the course

<!-- TODO(slice 4: Mallikarjuna) -->

## Quality attribute scenario

<!-- TODO(slice 4: Mallikarjuna) -->

## Results

The comparison runs the same 100 chunks serially on the calling thread, through a reusable
10-worker thread pool, and on 100 one-shot threads (one per chunk). After an untimed warm-up over
the first million integers, it measures each complete run and reports wall time, participating
threads, and chunks handled per thread; the chunk-to-worker map makes reuse by the pool visible.

## Trade-offs

<!-- TODO(slice 4: Mallikarjuna) -->

## Design (UML)

The class diagram shows the three packages (`pool`, `primes`, `run`) and how they fit together. The sequence diagram walks through one pool run: workers created once, one task per chunk, each worker going back to the queue for the next task, and shutdown letting queued work finish. The narrative and course mapping are in [uml/pool/README.md](uml/pool/README.md).

- Class diagram: [thread-pooling-class.png](uml/pool/thread-pooling-class.png) ([source](uml/pool/thread-pooling-class.puml))
- Sequence diagram: [thread-pooling-sequence.png](uml/pool/thread-pooling-sequence.png) ([source](uml/pool/thread-pooling-sequence.puml), [text version](uml/pool/thread-pooling-sequence.utxt))

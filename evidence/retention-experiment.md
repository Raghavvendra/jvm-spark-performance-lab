# Lab 1 Evidence — Object Retention

## Question
Why is object retention high, and how do we distinguish allocation churn from unbounded retention?

## Baseline
Short-lived objects were allocated and then released. Heap usage rose during work and dropped sharply after garbage collection. The retained-cache count remained zero.

## Failure scenario
A deliberately unbounded, executor-like cache retained every microbatch. Heap usage grew approximately monotonically until the process failed with:

```text
java.lang.OutOfMemoryError: Java heap space
```

With a 768 MB maximum heap, observed samples included approximately:

- batch 0: 31.4 MB
- batch 10: 349.5 MB
- batch 18: 603.2 MB
- batch 23: 764.1 MB

A GC cycle showed essentially no reduction in live heap for one critical sample (`573M -> 573M`).

## Evidence
The live heap histogram showed `byte[]` dominating the retained footprint, alongside the experiment's `EventRecord` population. The retained-batch count continued increasing because a long-lived cache kept the object graph reachable.

## Fix
The bounded scenario retained only the most recent three batches. Heap usage periodically fell after GC, while the retained-batch count stayed at three.

## Root cause
The primary defect was not insufficient heap capacity. The defect was unbounded object lifetime caused by a long-lived reference retaining every batch.

## Engineering lesson
When memory grows, ask:

1. Does it fall after GC?
2. What object types dominate the live heap?
3. What reference keeps those objects reachable?
4. Is the working set legitimately large, or is it unbounded?
5. Can the lifecycle be bounded before increasing infrastructure capacity?

# Lab 1 Results — Object Retention

## Hypothesis

If objects are retained through a long-lived reference, normal GC cannot reclaim them. The post-GC live set should grow with every batch, eventually causing heap exhaustion.

## Baseline observation

The short-lived scenario showed heap usage rising during allocation and then dropping sharply after ordinary G1 collection. The retained-cache count stayed at zero.

## Leak observation

With a 768 MB maximum heap:

- batch 0: ~31 MB live heap
- batch 10: ~350 MB
- batch 18: ~603 MB
- batch 23: ~764 MB
- next allocation: `OutOfMemoryError: Java heap space`

A JVM class histogram captured during the run showed:

- `[B`: ~590.8 MB retained in byte arrays
- `MemoryRetentionLab$EventRecord`: 36,000 instances
- `cache_batches`: 19 at the inspection point

GC log evidence showed collections that recovered almost nothing from the live set, e.g. a mixed collection around 573 MB -> 573 MB. That is the critical signature: **GC is functioning, but the objects are still reachable.**

## Bounded-cache observation

The bounded scenario kept only three batches. The cache-batch count stayed at 3, and G1 periodically reduced heap usage substantially (for example ~375 MB -> ~192 MB). It did not show the linear growth seen in the leak scenario.

## Root cause

The program deliberately stored every batch in a static, executor-like map. The static reference is effectively a GC root path:

`static RETAINED map -> BatchState -> List -> EventRecord -> byte[]`

Therefore those objects remained reachable and were not garbage.

## Production reasoning

In Spark/Databricks, do not jump from `ExecutorLostFailure` or high memory to "increase executor memory." First identify:

1. which process failed;
2. which memory pool/resource was exhausted;
3. whether the live set is stable, workload-proportional, or unbounded;
4. which object populations dominate;
5. which reference path retains them;
6. whether the execution plan/operator is generating the retention.

Databricks' current troubleshooting guidance starts with Spark UI, jobs/stages/tasks, driver/executor logs, and executor failure/memory evidence. Spark's current release index includes Spark 4.2.0; this lab's Spark code is therefore pinned to Spark 4.2.0 for reproducibility.

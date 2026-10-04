# Lab 1 notes — Object retention

The test was run in three modes.

### Baseline

Objects were released normally. Heap usage dropped after GC.

### Retention

Every batch was kept in a long-lived cache.

Heap grew from about 31 MB to about 764 MB and ended with `Java heap space`.

### Bounded

Only the latest three batches were kept.

Heap stopped growing continuously and dropped after GC.

### Result

The problem was the unbounded cache, not simply the heap size.

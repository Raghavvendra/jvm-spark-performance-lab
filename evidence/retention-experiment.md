# Lab 1 evidence — Object retention

| Mode | Result |
|---|---|
| Baseline | Heap rose during work and fell after GC |
| Retention | Heap kept growing and ended with `Java heap space` |
| Bounded | Heap stayed bounded with three retained batches |

A heap histogram from the retention run showed `byte[]` as the largest live object population.

The retention path was the long-lived cache holding each batch.

The fix was to bound the number of retained batches.

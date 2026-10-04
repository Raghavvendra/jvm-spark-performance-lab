# JVM + Spark Performance Engineering Lab

> A hands-on performance engineering lab that starts at the JVM and works upward into Spark internals and Databricks production diagnosis.

## Why this repository exists

Many Spark performance discussions stop at configuration knobs:

- increase executor memory
- increase shuffle partitions
- add more workers
- change a GC flag

That is not the approach used here.

The objective is to learn a repeatable diagnostic method:

```text
Symptom
  ↓
Evidence
  ↓
Hypothesis
  ↓
Experiment
  ↓
Root cause
  ↓
Smallest effective fix
  ↓
Measure again
```

The lab deliberately creates performance and memory pathologies so that we can inspect them rather than memorize their definitions.

## Current lab

### Lab 1 — Object retention and JVM heap failure

Production question:

> **Why is object retention high?**

We build three controlled scenarios:

| Scenario | Behavior | Purpose |
|---|---|---|
| `baseline` | Short-lived objects | Establish normal allocation/GC behavior |
| `leak` | Every batch is retained | Demonstrate unbounded live-set growth and heap OOM |
| `bounded` | Only recent batches retained | Demonstrate bounded memory lifetime |

The lab uses a synthetic IoT microbatch workload with fields such as `event_id`, `event_ts`, `device_id`, `site_id`, `region`, `temperature_c`, `pressure_kpa`, `status`, `customer_id`, and `payload`.

### What we proved

The retention experiment produced a continuously growing live set and ultimately:

```text
java.lang.OutOfMemoryError: Java heap space
```

The important diagnostic observation was that garbage collection could run without materially reducing the live heap because the application still had references to the retained objects.

The fix was **not** simply to increase heap size. We bounded the object lifetime by retaining only the most recent three batches.

Detailed evidence: [`evidence/retention-experiment.md`](evidence/retention-experiment.md)

## Diagnostic method

For each lab, we use progressively deeper evidence:

```text
Spark UI / application metrics
        ↓
stage and task behavior
        ↓
physical plan
        ↓
executor / driver logs
        ↓
JVM metrics and GC logs
        ↓
heap histogram
        ↓
heap dump / retained heap
        ↓
reference graph / GC roots
        ↓
OS and container metrics
```

The key discipline is to diagnose the failed subsystem before changing configuration.

For example:

```text
OutOfMemoryError
```

does not automatically mean:

```text
increase heap
```

We first determine whether the failure is due to allocation churn, a legitimately large working set, object retention, metaspace, native memory, thread limits, or a container-level memory limit.

## Repository structure

```text
.
├── README.md
├── MemoryRetentionLab.java
├── data/
│   └── generate_dataset.py
├── evidence/
│   └── retention-experiment.md
├── scripts/
│   ├── analyze.py
│   ├── compile.sh
│   ├── inspect.sh
│   └── run.sh
├── spark/
│   ├── pom.xml
│   ├── README.md
│   └── src/main/java/com/lab/
│       └── SparkExecutorRetentionLab.java
└── docs/
    └── LAB_NOTES.md
```

Generated datasets, compiled classes, heap dumps, PIDs, and raw runtime logs are intentionally excluded from Git. They are reproducible artifacts, not source code.

## Running the JVM lab

Requirements:

- JDK 17+ recommended
- `jcmd` available on `PATH`
- Bash-compatible shell

Generate the synthetic data:

```bash
python data/generate_dataset.py
```

Compile:

```bash
./scripts/compile.sh
```

Run the scenarios:

```bash
./scripts/run.sh baseline
./scripts/run.sh leak
./scripts/run.sh bounded
```

Inspect a live run:

```bash
./scripts/inspect.sh leak
```

The scripts capture JVM/GC evidence without requiring a production cluster.

## Spark implementation

The repository also contains the Spark version of the experiment under `spark/`.

The Spark implementation is deliberately kept separate from the JVM-only control experiment so we can answer a critical question:

> What changes when the same memory-lifetime problem is executed inside a real Spark executor workload?

That is the bridge from JVM internals to Spark performance engineering.

## Roadmap

This repository will grow as an evidence-driven performance series:

1. Object retention and heap analysis
2. Allocation churn and GC overhead
3. Java heap OOM: working set vs leak
4. Driver-side memory failures
5. Shuffle memory and spill
6. Partition sizing and task working sets
7. Data skew and straggler tasks
8. Join strategy and build-side memory
9. Aggregation state growth
10. Cache/storage-memory trade-offs
11. Serialization and object representation
12. UDF and language-boundary overhead
13. Window-function memory behavior
14. Native/off-heap/thread pressure
15. Metaspace and classloader retention
16. JVM + Spark query-plan diagnosis
17. Databricks-specific performance investigation
18. Full production-style incident simulation

The final lab will combine multiple deliberately interacting failure modes and require diagnosis from limited production-style evidence.

## Engineering standard

Every optimization in this repository should answer five questions:

1. **What was the symptom?**
2. **What evidence isolated the subsystem?**
3. **What mechanism caused the symptom?**
4. **Why does the fix work?**
5. **What changed in the measurements?**

That is the standard for the series: not "I know Spark tuning," but "I can diagnose why a distributed data workload is slow or unstable."

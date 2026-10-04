# Spark-specific Lab 1

This is the real Spark implementation of the same mechanism demonstrated in `src/MemoryRetentionLab.java`.

It reads the synthetic IoT microbatch dataset from `../data/iot_microbatches`, performs a normal aggregation, then optionally creates executor-local retention through a static map inside a `mapPartitions` operation.

## Why this is deliberately designed this way

The static map is not normal Spark programming. It is a diagnostic trap designed to mimic a long-lived executor-side reference that survives task completion. In a real production investigation you would *not* assume the cause; you would discover it through memory trends, histograms/heap dumps, executor logs, and code/plan correlation.

Run:

```bash
mvn -q package
spark-submit --class com.lab.SparkExecutorRetentionLab target/spark-performance-lab-1.0-SNAPSHOT.jar baseline ../data/iot_microbatches
spark-submit --class com.lab.SparkExecutorRetentionLab target/spark-performance-lab-1.0-SNAPSHOT.jar leak ../data/iot_microbatches
spark-submit --class com.lab.SparkExecutorRetentionLab target/spark-performance-lab-1.0-SNAPSHOT.jar bounded ../data/iot_microbatches
```

For Databricks, the same dataset can be placed in cloud storage and the application translated into notebook/job form. The investigation sequence remains: establish baseline, inspect Spark UI, isolate the stage, correlate executor/JVM memory behavior, inspect object populations, then fix the lifecycle/retention issue before changing cluster sizing.

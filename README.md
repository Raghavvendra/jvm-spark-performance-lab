# JVM + Spark Performance Lab

A small set of hands-on experiments for understanding JVM and Spark performance.

The idea is simple: reproduce a problem, measure it, find the cause, fix it, and measure again.

## Current

### Lab 1 — Object retention

Three cases:

- baseline
- unbounded retention
- bounded retention

The lab uses synthetic micro-batch data and JVM diagnostics to compare heap and GC behavior.

## Workflow

```text
reproduce → measure → isolate → fix → verify
```

## Structure

```text
MemoryRetentionLab.java   JVM experiment
spark/                    Spark version
scripts/                  run / inspect helpers
data/                     dataset generator
evidence/                 experiment notes
docs/                     project notes
```

## Run

JDK 17+ and `jcmd` are recommended.

```bash
python data/generate_dataset.py
./scripts/compile.sh
./scripts/run.sh baseline
./scripts/run.sh leak
./scripts/run.sh bounded
```

The Spark experiment is under `spark/`.

## Status

Lab 1 is in place. More JVM, Spark, and Databricks performance experiments will be added as the lab grows.
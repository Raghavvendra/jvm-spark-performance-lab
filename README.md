# JVM + Spark Performance Lab

Small experiments around JVM and Spark performance.

## Current

**Lab 1 — Object retention**

- baseline
- retained batches
- bounded batches

The experiment uses synthetic data and JVM diagnostics.

## Run

JDK 17+ and `jcmd`.

```bash
python data/generate_dataset.py
./scripts/compile.sh
./scripts/run.sh baseline
./scripts/run.sh leak
./scripts/run.sh bounded
```

Spark code is under `spark/`.

## Status

Lab 1 complete. More experiments will be added.

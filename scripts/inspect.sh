#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCENARIO="${1:-baseline}"
PID_FILE="$ROOT/results/$SCENARIO/pid.txt"
PID="$(cat "$PID_FILE")"
OUT="$ROOT/results/$SCENARIO/inspect.txt"
{
  echo "=== TIMESTAMP ==="; date -Is
  echo "=== JPS ==="; jps -l
  echo "=== VM INFO ==="; jcmd "$PID" VM.info
  echo "=== GC HEAP INFO ==="; jcmd "$PID" GC.heap_info
  echo "=== CLASS HISTOGRAM ==="; jcmd "$PID" GC.class_histogram
} > "$OUT" 2>&1 || true
cat "$OUT"

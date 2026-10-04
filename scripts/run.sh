#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCENARIO="${1:-baseline}"
HEAP="${HEAP:-768m}"
mkdir -p "$ROOT/results/$SCENARIO"
"$ROOT/scripts/compile.sh" >/dev/null
LOG="$ROOT/results/$SCENARIO/app.log"
GCLOG="$ROOT/results/$SCENARIO/gc.log"
nohup java \
  -Xms512m -Xmx"$HEAP" \
  -XX:+UseG1GC \
  -Xlog:gc*,safepoint:file="$GCLOG":time,uptime,level,tags \
  -cp "$ROOT/classes" MemoryRetentionLab "$SCENARIO" \
  >"$LOG" 2>&1 &
PID=$!
echo "$PID" > "$ROOT/results/$SCENARIO/pid.txt"
echo "Started $SCENARIO; PID=$PID"
echo "Log: $LOG"
echo "GC log: $GCLOG"

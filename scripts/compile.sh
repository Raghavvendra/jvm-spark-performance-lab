#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/classes"
javac -d "$ROOT/classes" "$ROOT/src/MemoryRetentionLab.java"
echo "Compiled to $ROOT/classes"

from pathlib import Path
import re, sys

if len(sys.argv) != 2:
    raise SystemExit('usage: analyze.py <scenario>')
root = Path(__file__).resolve().parents[1]
scenario = sys.argv[1]
log = root / 'results' / scenario / 'app.log'
if not log.exists():
    raise SystemExit(f'Missing {log}')
rows=[]
pat=re.compile(r'BATCH=(\d+) heap_used_mb=([0-9.]+).*cache_batches=(\d+)')
for line in log.read_text().splitlines():
    m=pat.search(line)
    if m:
        rows.append((int(m.group(1)), float(m.group(2)), int(m.group(3))))
if not rows:
    print('No batch metrics found')
    raise SystemExit(0)
print('batch,heap_used_mb,cache_batches')
for r in rows:
    print(','.join(map(str,r)))
print()
print(f'first_heap_mb={rows[0][1]:.1f}')
print(f'last_heap_mb={rows[-1][1]:.1f}')
print(f'growth_mb={rows[-1][1]-rows[0][1]:.1f}')
print(f'final_cache_batches={rows[-1][2]}')

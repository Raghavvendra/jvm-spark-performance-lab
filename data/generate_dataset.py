from pathlib import Path
from datetime import datetime, timezone, timedelta
import json, random

OUT = Path(__file__).resolve().parent / 'iot_microbatches'
OUT.mkdir(parents=True, exist_ok=True)
random.seed(20261004)

batches = 10
rows_per_batch = 50_000
start = datetime(2026, 10, 4, 8, 0, 0, tzinfo=timezone.utc)
regions = ['BLR', 'MYS', 'HYD', 'CHE', 'PUN']
statuses = ['OK', 'WARN', 'FAIL']

for b in range(batches):
    path = OUT / f'batch_{b:03d}.jsonl'
    with path.open('w', encoding='utf-8') as f:
        batch_start = start + timedelta(minutes=b)
        for i in range(rows_per_batch):
            event_id = b * rows_per_batch + i
            # Deliberate but realistic skew: one gateway generates many more events.
            device_id = 'gateway_hot' if i < rows_per_batch * 0.12 else f'device_{random.randint(1, 50_000):05d}'
            site_id = f'site_{random.randint(1, 500):04d}'
            ts = batch_start + timedelta(milliseconds=random.randint(0, 59_999))
            temp = round(random.gauss(27.0, 4.0), 2)
            pressure = round(random.gauss(101.3, 1.8), 2)
            status = random.choices(statuses, weights=[92, 6, 2], k=1)[0]
            customer_id = random.randint(1, 20_000)
            payload = f'firmware=7.{random.randint(0,9)};signal={random.randint(-95,-40)};seq={event_id}'
            rec = {
                'event_id': event_id,
                'event_ts': ts.isoformat(),
                'device_id': device_id,
                'site_id': site_id,
                'region': random.choice(regions),
                'temperature_c': temp,
                'pressure_kpa': pressure,
                'status': status,
                'customer_id': customer_id,
                'payload': payload,
            }
            f.write(json.dumps(rec, separators=(',', ':')) + '\n')

print(f'generated {batches} files x {rows_per_batch} rows = {batches*rows_per_batch:,} records in {OUT}')

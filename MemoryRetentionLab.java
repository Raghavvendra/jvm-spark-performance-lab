import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lab 1 JVM memory simulator.
 *
 * It mimics a Spark executor pattern:
 *   input microbatch -> per-record objects -> transformation -> aggregate -> lifecycle
 *
 * Scenarios:
 *   baseline : objects are short-lived and discarded
 *   leak     : objects are retained in an executor-like static cache
 *   bounded  : objects are retained briefly, then evicted (controlled reuse/cache)
 *   churn    : very high short-lived allocation rate to create GC pressure
 */
public class MemoryRetentionLab {
    static final Map<Long, BatchState> EXECUTOR_CACHE = new ConcurrentHashMap<>();

    static final int EVENTS_PER_BATCH = 20_000;
    static final int PAYLOAD_BYTES = 16 * 1024; // 16 KB per retained object
    static final int RETAINED_EVENTS_PER_BATCH = 2_000;
    static final int BATCHES = 30;
    static final long PAUSE_MS = 700;

    static class EventRecord {
        final long eventId;
        final int deviceId;
        final double value;
        final byte[] payload;

        EventRecord(long eventId, int deviceId, double value, byte[] payload) {
            this.eventId = eventId;
            this.deviceId = deviceId;
            this.value = value;
            this.payload = payload;
        }
    }

    static class BatchState {
        final long batchId;
        final List<EventRecord> records;

        BatchState(long batchId, List<EventRecord> records) {
            this.batchId = batchId;
            this.records = records;
        }
    }

    static long sink;

    public static void main(String[] args) throws Exception {
        String scenario = args.length == 0 ? "baseline" : args[0];
        System.out.println("PID=" + ProcessHandle.current().pid());
        System.out.println("SCENARIO=" + scenario);
        System.out.println("START=" + Instant.now());
        printHeap("START");

        for (long batch = 0; batch < BATCHES; batch++) {
            switch (scenario) {
                case "baseline" -> runBaselineBatch(batch);
                case "leak" -> runLeakBatch(batch);
                case "bounded" -> runBoundedBatch(batch);
                case "churn" -> runChurnBatch(batch);
                default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
            }

            // Deliberately no System.gc(): in a performance investigation, forcing GC would
            // contaminate the signal. We observe the JVM naturally.
            printHeap("BATCH=" + batch);
            Thread.sleep(PAUSE_MS);
        }

        printHeap("END");
        System.out.println("CACHE_BATCHES=" + EXECUTOR_CACHE.size());
        System.out.println("END=" + Instant.now());
        Thread.sleep(3_000); // keep JVM alive for final jcmd inspection
    }

    static void runBaselineBatch(long batch) {
        long local = 0;
        for (int i = 0; i < EVENTS_PER_BATCH; i++) {
            byte[] payload = new byte[1024];
            EventRecord e = new EventRecord(batch * EVENTS_PER_BATCH + i, i % 10_000, i * 0.01, payload);
            local += e.deviceId + e.payload[0];
        }
        sink ^= local;
    }

    static void runLeakBatch(long batch) {
        List<EventRecord> retained = new ArrayList<>(RETAINED_EVENTS_PER_BATCH);
        for (int i = 0; i < RETAINED_EVENTS_PER_BATCH; i++) {
            byte[] payload = new byte[PAYLOAD_BYTES];
            payload[0] = (byte) i;
            retained.add(new EventRecord(batch * EVENTS_PER_BATCH + i, i % 10_000, i * 0.01, payload));
        }
        EXECUTOR_CACHE.put(batch, new BatchState(batch, retained));
    }

    static void runBoundedBatch(long batch) {
        List<EventRecord> retained = new ArrayList<>(RETAINED_EVENTS_PER_BATCH);
        for (int i = 0; i < RETAINED_EVENTS_PER_BATCH; i++) {
            byte[] payload = new byte[PAYLOAD_BYTES];
            retained.add(new EventRecord(batch * EVENTS_PER_BATCH + i, i % 10_000, i * 0.01, payload));
        }
        EXECUTOR_CACHE.put(batch, new BatchState(batch, retained));
        // Keep only a bounded working set, analogous to an explicitly managed cache.
        long expireBefore = batch - 2;
        EXECUTOR_CACHE.keySet().removeIf(k -> k < expireBefore);
    }

    static void runChurnBatch(long batch) {
        long local = 0;
        for (int i = 0; i < EVENTS_PER_BATCH * 8; i++) {
            byte[] payload = new byte[4096];
            payload[0] = (byte) (i ^ batch);
            local += payload[0];
        }
        sink ^= local;
    }

    static void printHeap(String label) {
        MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = bean.getHeapMemoryUsage();
        MemoryUsage nonHeap = bean.getNonHeapMemoryUsage();
        System.out.printf(
            "%s heap_used_mb=%.1f heap_committed_mb=%.1f heap_max_mb=%.1f nonheap_used_mb=%.1f cache_batches=%d%n",
            label,
            heap.getUsed() / 1024.0 / 1024.0,
            heap.getCommitted() / 1024.0 / 1024.0,
            heap.getMax() / 1024.0 / 1024.0,
            nonHeap.getUsed() / 1024.0 / 1024.0,
            EXECUTOR_CACHE.size()
        );
    }
}

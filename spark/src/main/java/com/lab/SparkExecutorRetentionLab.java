package com.lab;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.storage.StorageLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.spark.sql.functions.*;

/**
 * Spark version of Lab 1.
 *
 * The static cache is intentionally executor-local state. In a real multi-executor
 * deployment, each executor JVM has its own copy. In local mode, the JVM/process model
 * is different, so this lab is for mechanism learning rather than cluster benchmarking.
 *
 * Usage:
 *   spark-submit --class com.lab.SparkExecutorRetentionLab ... baseline
 *   spark-submit --class com.lab.SparkExecutorRetentionLab ... leak
 *   spark-submit --class com.lab.SparkExecutorRetentionLab ... bounded
 */
public class SparkExecutorRetentionLab {
    static final Map<Integer, List<Row>> RETAINED = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        String scenario = args.length == 0 ? "baseline" : args[0];
        String input = args.length > 1 ? args[1] : "../data/iot_microbatches";

        SparkSession spark = SparkSession.builder()
                .appName("SparkExecutorRetentionLab-" + scenario)
                .master("local[4]")
                .config("spark.sql.shuffle.partitions", "16")
                .config("spark.ui.enabled", "true")
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        Dataset<Row> events = spark.read()
                .schema("event_id long, event_ts timestamp, device_id string, site_id string, " +
                        "region string, temperature_c double, pressure_kpa double, status string, " +
                        "customer_id long, payload string")
                .json(input);

        Dataset<Row> baseline = events
                .filter(col("status").notEqual("FAIL"))
                .groupBy(col("region"), col("device_id"))
                .agg(
                        count(lit(1)).alias("events"),
                        avg(col("temperature_c")).alias("avg_temp"),
                        max(col("pressure_kpa")).alias("max_pressure")
                );

        if (scenario.equals("baseline")) {
            System.out.println("BASELINE rows=" + baseline.count());
        } else {
            Dataset<Row> rows = events.repartition(16, col("region"));
            rows.mapPartitions((org.apache.spark.api.java.function.FlatMapFunction<java.util.Iterator<Row>, Row>) it -> {
                List<Row> current = new ArrayList<>();
                while (it.hasNext()) {
                    Row r = it.next();
                    // Keep only a controlled number of references per partition.
                    current.add(r);
                    if (current.size() >= 2000) break;
                }
                int key = System.identityHashCode(Thread.currentThread());
                if (scenario.equals("leak")) {
                    RETAINED.put(key, current);          // intentionally unbounded across task lifecycles
                } else {
                    RETAINED.put(key, current);
                    RETAINED.keySet().removeIf(k -> k != key && RETAINED.size() > 3);
                }
                return java.util.Collections.emptyIterator();
            }).count();
            System.out.println("SCENARIO=" + scenario + " RETAINED_BUCKETS=" + RETAINED.size());
            System.out.println("RESULT=" + baseline.count());
        }

        spark.stop();
    }
}

package hr.fer.ztel.rassus.dz2.thread;

import hr.fer.ztel.rassus.dz2.model.Measurement;
import hr.fer.ztel.rassus.dz2.timestamp.ScalarTimestamp;
import hr.fer.ztel.rassus.dz2.timestamp.VectorTimestamp;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@Getter
@ToString
public class Node {

    /** Interval between two sorts of measurements, in milliseconds. */
    private static final long SORT_INTERVAL_MILLIS = 5000;

    private final long startTime = System.currentTimeMillis();

    private final String name;
    private final int nodeIndex;
    private final int totalNodes;

    private final AtomicInteger eventCount = new AtomicInteger();
    private final List<Measurement> measurements = new CopyOnWriteArrayList<>();
    private final Map<ScalarTimestamp, Measurement> scalarTimestampMap = new ConcurrentHashMap<>();
    private final Map<VectorTimestamp, Measurement> vectorTimestampMap = new ConcurrentHashMap<>();

    private boolean started = false;
    private final ServerThread serverThread;
    private final ScheduledExecutorService executorService;

    public Node(String name, int port, double lossRate, int averageDelay, int nodeIndex, int totalNodes) {
        this.name = name;
        this.nodeIndex = nodeIndex;
        this.totalNodes = totalNodes;

        this.serverThread = new ServerThread(this, port, lossRate, averageDelay);
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void startNode() {
        if (started) {
            return;
        }

        started = true;
        serverThread.start();
        executorService.schedule(new SortingJob(), SORT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void recordEvent() {
        eventCount.incrementAndGet();
    }

    public synchronized void storeMeasurement(Measurement measurement, ScalarTimestamp scalar, VectorTimestamp vector) {
        measurements.add(measurement);
        scalarTimestampMap.put(scalar, measurement);
        vectorTimestampMap.put(vector, measurement);
    }

    private class SortingJob implements Runnable {

        @Override
        public void run() {
            // Calculate average CO value
            double averageCO = measurements.stream()
                    .map(Measurement::getCo)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);

            // Sort measurements by timestamps and clear all global measurements
            Map<ScalarTimestamp, Measurement> scalar = new TreeMap<>(scalarTimestampMap);
            Map<VectorTimestamp, Measurement> vector = new TreeMap<>(vectorTimestampMap);
            synchronized (Node.this) {
                scalarTimestampMap.clear();
                vectorTimestampMap.clear();
                measurements.clear();
            }

            // Log required information
            log.info("Average CO measurement: {}", averageCO);
            log.info("Measurements (scalar):  {}", scalar);
            log.info("Measurements (vector):  {}", vector);
        }
    }
}

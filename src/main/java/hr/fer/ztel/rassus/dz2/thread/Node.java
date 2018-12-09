package hr.fer.ztel.rassus.dz2.thread;

import hr.fer.ztel.rassus.dz2.model.Measurement;
import hr.fer.ztel.rassus.dz2.timestamp.ScalarTimestamp;
import hr.fer.ztel.rassus.dz2.timestamp.VectorTimestamp;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.net.SocketAddress;
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
public class Node {

    /** Interval between two sorts of measurements, in milliseconds. */
    private static final long SORT_INTERVAL_MILLIS = 5000;

    @Getter private long startTime;
    @Getter private boolean started = false;

    @Getter private final String name;
    @Getter private final int port;
    @Getter private final int nodeIndex;
    @Getter private final int totalNodes;

    @Getter private volatile VectorTimestamp lastVectorTimestamp;
    @Getter private volatile ScalarTimestamp lastScalarTimestamp;

    private final AtomicInteger eventCount = new AtomicInteger();
    private final List<Measurement> measurements = new CopyOnWriteArrayList<>();
    private final Map<ScalarTimestamp, Measurement> scalarTimestampMap = new ConcurrentHashMap<>();
    private final Map<VectorTimestamp, Measurement> vectorTimestampMap = new ConcurrentHashMap<>();

    private final ServerThread serverThread;
    private final ClientThread clientThread;
    private final ScheduledExecutorService executorService;

    public Node(String name, int port, double lossRate, int averageDelay, int nodeIndex, List<SocketAddress> neighbourNodes) {
        this.name = name;
        this.port = port;
        this.nodeIndex = nodeIndex;
        this.totalNodes = neighbourNodes.size() + 1;

        this.lastVectorTimestamp = new VectorTimestamp(new int[totalNodes]);
        this.lastScalarTimestamp = new ScalarTimestamp(0);

        this.serverThread = new ServerThread(this, port, lossRate, averageDelay);
        this.clientThread = new ClientThread(this, lossRate, averageDelay, neighbourNodes);
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void startNode() {
        if (started) {
            log.info("Node {} was already started. Cannot reuse resources.", name);
            return;
        }

        log.info("Starting node {}", name);
        started = true;
        startTime = System.currentTimeMillis();

        serverThread.setDaemon(true);
        serverThread.start();
        clientThread.start();
        executorService.scheduleWithFixedDelay(new SortJob(),
                SORT_INTERVAL_MILLIS, SORT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        if (!started) {
            return;
        }

        log.info("Shutting down node {}", name);

        clientThread.interrupt();
        serverThread.interrupt();
        executorService.shutdown();
    }

    public int getEventCount() {
        return eventCount.get();
    }

    public void storeMeasurement(Measurement measurement) {
        storeMeasurement(measurement, lastScalarTimestamp, lastVectorTimestamp);
    }

    public synchronized void storeMeasurement(Measurement measurement, ScalarTimestamp scalar, VectorTimestamp vector) {
        recordEvent();

        // Set last vector timestamp and always force local event count value for current node
        lastVectorTimestamp = VectorTimestamp.combine(lastVectorTimestamp, vector, nodeIndex, getEventCount());
        log.debug("Last vector timestamp: {}", lastVectorTimestamp);

        measurements.add(measurement);
        scalarTimestampMap.put(scalar, measurement);
        vectorTimestampMap.put(vector, measurement);
    }

    private void recordEvent() {
        eventCount.incrementAndGet();
    }

    private class SortJob implements Runnable {

        @Override
        public void run() {
            // Calculate average CO value
            double averageCO = measurements.stream()
                    .map(Measurement::getCo)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(Double.NaN);

            // Sort measurements by timestamps and clear all global measurements
            TreeMap<ScalarTimestamp, Measurement> scalar = new TreeMap<>(scalarTimestampMap);
            TreeMap<VectorTimestamp, Measurement> vector = new TreeMap<>(vectorTimestampMap);
            synchronized (Node.this) {
                scalarTimestampMap.clear();
                vectorTimestampMap.clear();
                measurements.clear();
            }

            // Log required information
            log.info("Average CO measurement: {}", averageCO);
            log.info("Measurements (scalar):  {}", scalar.descendingMap());
            log.info("Measurements (vector):  {}", vector.descendingMap());
        }
    }
}

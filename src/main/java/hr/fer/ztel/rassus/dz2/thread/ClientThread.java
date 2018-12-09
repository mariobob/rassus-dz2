package hr.fer.ztel.rassus.dz2.thread;

import com.google.gson.Gson;
import hr.fer.ztel.rassus.dz2.loader.Loaders;
import hr.fer.ztel.rassus.dz2.model.Measurement;
import hr.fer.ztel.rassus.dz2.model.MeasurementPacket;
import hr.fer.ztel.rassus.dz2.stupidudp.network.SimpleSimulatedDatagramSocket;
import hr.fer.ztel.rassus.dz2.timestamp.ScalarTimestamp;
import hr.fer.ztel.rassus.dz2.timestamp.VectorTimestamp;
import hr.fer.ztel.rassus.dz2.util.Utility;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static hr.fer.ztel.rassus.dz2.util.Utility.RECEIVE_CONFIRMATION;

@Log4j2
@ToString
public class ClientThread extends Thread {

    /** Interval between two automatic measurements, in milliseconds. */
    private static final long AUTO_MEASURE_SLEEP_MILLIS = 1000;
    private static final int BUFFER_SIZE = 256;
    /** Maximum number of attempts when retrying a send. */
    private static final int RETRY_LOGIC_ATTEMPTS = 3;

    private final ExecutorService executorService;

    private final Node node;
    @Getter private final double lossRate;
    @Getter private final int averageDelay;
    @Getter private final List<SocketAddress> neighbourNodes;

    public ClientThread(Node node, double lossRate, int averageDelay, List<SocketAddress> neighbourNodes) {
        super("ClientThread");
        this.node = node;
        this.lossRate = lossRate;
        this.averageDelay = averageDelay;
        this.neighbourNodes = neighbourNodes;
        this.executorService = Executors.newFixedThreadPool(3 * neighbourNodes.size());
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new SimpleSimulatedDatagramSocket(lossRate, averageDelay)) {
            while (!Thread.currentThread().isInterrupted()) {
                // Start a measurement and sleep until the new measurement cycle
                measure(socket);
                try { Thread.sleep(AUTO_MEASURE_SLEEP_MILLIS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
    }

    public void measure(DatagramSocket socket) {
        // Generate measurement
        int secondsActive = Math.toIntExact((System.currentTimeMillis() - node.getStartTime()) / 1000);
        Measurement measurement = Loaders.getMeasurementLoader().getMeasurement(secondsActive % 100);
        log.debug("Generated measurement: {}", measurement);

        // Store measurement locally
        node.storeMeasurement(measurement);
        ScalarTimestamp scalar = node.getLastScalarTimestamp();
        VectorTimestamp vector = node.getLastVectorTimestamp();

        // Send to all other nodes in network
        for (SocketAddress nodeAddress : neighbourNodes) {
            executorService.submit(new SendJob(node.getName(), socket, nodeAddress, measurement, scalar, vector));
        }
    }

    @RequiredArgsConstructor
    private static class SendJob implements Runnable {

        /** ID of the packet being sent. This prevents duplicates over UDP. */
        private static long packetId = 0L;

        private final String nodeName;
        private final DatagramSocket socket;
        private final SocketAddress nodeAddress;
        private final Measurement measurement;
        private final ScalarTimestamp scalar;
        private final VectorTimestamp vector;

        @Override
        public void run() {
            log.info("Sending packet to {}", nodeAddress);

            MeasurementPacket measurementPacket = new MeasurementPacket(
                    nodeName+"-"+packetId, measurement, scalar, vector);
            packetId++;

            // Create a JSON object and convert to bytes
            Gson gson = new Gson();
            String jsonText = gson.toJson(measurementPacket);
            byte[] bytes = jsonText.getBytes();

            // Send measurement to socket
            Utility.retry(RETRY_LOGIC_ATTEMPTS, () -> {
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, nodeAddress);
                socket.send(packet);
                return true;
            });

            // Await confirmation
            Utility.retry(RETRY_LOGIC_ATTEMPTS, () -> {
                DatagramPacket rcvPacket = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
                socket.receive(rcvPacket);

                String rcvString = new String(rcvPacket.getData(), rcvPacket.getOffset(), rcvPacket.getLength());
                return rcvString.equals(RECEIVE_CONFIRMATION);
            });
        }
    }
}

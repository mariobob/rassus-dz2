package hr.fer.ztel.rassus.dz2.thread;

import com.google.gson.Gson;
import hr.fer.ztel.rassus.dz2.model.MeasurementPacket;
import hr.fer.ztel.rassus.dz2.stupidudp.network.SimpleSimulatedDatagramSocket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static hr.fer.ztel.rassus.dz2.util.Utility.RECEIVE_CONFIRMATION;

@Log4j2
@ToString
public class ServerThread extends Thread {

    /** Socket timeout, in milliseconds. */
    private static final int DEFAULT_SO_TIMEOUT = 1000;
    private static final int BUFFER_SIZE = 256;

    @ToString.Exclude
    private final transient ExecutorService threadPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() - 1
    );

    /** IDs of packages already received and stored. */
    private final Set<String> receivedIds = new HashSet<>();

    private final Node node;
    @Getter private final int port;
    @Getter private final double lossRate;
    @Getter private final int averageDelay;

    public ServerThread(Node node, int port, double lossRate, int averageDelay) {
        super("ServerThread");
        this.node = node;
        this.port = port;
        this.lossRate = lossRate;
        this.averageDelay = averageDelay;
    }

    @Override
    public void run() {
        try (DatagramSocket serverSocket = new SimpleSimulatedDatagramSocket(port, lossRate, averageDelay)) {
            serverSocket.setSoTimeout(DEFAULT_SO_TIMEOUT);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    acceptClient(serverSocket);
                } catch (SocketTimeoutException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Accepts the client <b>blocking</b> the thread while waiting for a
     * client socket.
     *
     * @param serverSocket server socket that accepts clients
     * @throws SocketTimeoutException if the server socket has timed out
     * @throws IOException            if an I/O or other socket exception occurs
     */
    private void acceptClient(DatagramSocket serverSocket) throws IOException {
        byte[] rcvBuf = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(rcvBuf, rcvBuf.length);
        serverSocket.receive(packet);
        ClientWorker cw = new ClientWorker(serverSocket, packet);
        threadPool.submit(cw);
    }

    /**
     * Runnable object that processes datagram packets and sends confirmation to the client.
     *
     * @author Mario Bobic
     */
    @RequiredArgsConstructor
    private class ClientWorker implements Runnable {
        /** Datagram socket for communication with the client. */
        private final DatagramSocket socket;
        /** Packet received from the client. */
        private final DatagramPacket packet;

        @Override
        public void run() {
            log.info("Receiving packet from {}", packet.getSocketAddress());

            try {
                // Receive measurement in JSON format from client
                String rcvString = new String(packet.getData(), packet.getOffset(), packet.getLength());

                // Create a DatagramPacket and send confirmation of receipt
                byte[] bytes = RECEIVE_CONFIRMATION.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(
                        bytes, bytes.length, packet.getAddress(), packet.getPort());
                log.debug("Received packet. Sending confirmation...");
                socket.send(sendPacket);

                // Convert string to JSON and check package ID
                Gson gson = new Gson();
                MeasurementPacket measurementPacket = gson.fromJson(rcvString, MeasurementPacket.class);
                if (receivedIds.contains(measurementPacket.getId())) {
                    log.info("Received packet is a duplicate: {}", measurementPacket);
                    return;
                }

                storeMeasurement(measurementPacket);
                receivedIds.add(measurementPacket.getId());
                log.debug("Finished processing packet from {}", packet.getSocketAddress());
            } catch (IOException e) {
                log.error("An I/O exception occurred", e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void storeMeasurement(MeasurementPacket measurementPacket) {
            node.storeMeasurement(
                    measurementPacket.getMeasurement(),
                    measurementPacket.getScalarTimestamp(),
                    measurementPacket.getVectorTimestamp());
        }
    }
}

package hr.fer.ztel.rassus.dz2;

import hr.fer.ztel.rassus.dz2.thread.Node;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

@Log4j2
public class NodeConsole {

    private static final Path NETWORK_CONFIG_PATH = Paths.get("src/main/resources/network.config");
    private static final String NODE_HOSTNAME = "localhost";

    /**
     * Client program entry point.
     *
     * @param args node name
     */
    public static void main(String[] args) throws IOException {
        // Get node name and instantiate object with configuration
        String name = readNodeName(args);
        Node node = createNode(name);
        if (node == null) {
            log.error("Could not find node with name '{}'. Exiting...", name);
            return;
        }

        // Print out the welcome text
        System.out.println("Welcome to node management interface of " + name + ".");
        System.out.println("Enter 'START' to trigger node or 'EXIT' to shutdown node.");

        // Start the command prompt, listen for user input and loop through it
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            l:
            while (true) {
                System.out.print("> ");

                String command = reader.readLine();
                if (command == null) break;
                if (command.trim().isEmpty()) continue;

                switch (command.toUpperCase()) {
                    case "START":
                        node.startNode();
                        break;

                    case "EXIT":
                        node.shutdown();
                        break l;

                    default:
                        System.out.println("Unknown command: " + command);
                }
            }

            reader.close();
            System.out.println("Sensor client console has shut down. Goodbye!");
        } catch (Exception e) {
            System.out.println("A critical error occurred... shutting down node.");
            try { node.shutdown(); } catch (Exception ignorable) {}
            throw e;
        }
    }

    private static String readNodeName(String[] args) {
        if (args.length > 0) {
            log.info("Reading node name from command line...");
            return args[0];
        } else {
            System.out.print("Enter node name: ");
            Scanner sc = new Scanner(System.in);
            return sc.nextLine();
        }
    }

    private static Node createNode(String name) throws IOException {
        // Read all lines from config and store all nodes in a map;
        // Map: key=<NodeName>, value=<NodeLine>
        List<String> lines = Files.readAllLines(NETWORK_CONFIG_PATH, StandardCharsets.UTF_8);
        Map<String, String> allNodeLines = lines.stream()
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toMap(l -> l.split("\\s+")[0], l -> l));

        // Get node with specified name
        String line = allNodeLines.get(name);
        if (line == null) {
            return null;
        }

        // Remove specified node, leaving a map with only neighbouring nodes
        allNodeLines.remove(name);

        // Map neighbouring nodes to SocketAddress objects
        final InetAddress nodeAddress = InetAddress.getByName(NODE_HOSTNAME);
        List<SocketAddress> neighbourNodes = allNodeLines.values().stream()
                .map(l -> l.split("\\s+")[2])
                .map(Integer::parseInt)
                .map(port -> new InetSocketAddress(nodeAddress, port))
                .collect(Collectors.toList());

        // Finally create the node
        String[] tokens = line.split("\\s+");
        int index = Integer.parseInt(tokens[1]);
        int port = Integer.parseInt(tokens[2]);
        double lossRate = Double.parseDouble(tokens[3]);
        int averageDelay = Integer.parseInt(tokens[4]);
        return new Node(name, port, lossRate, averageDelay, index, neighbourNodes);
    }

}

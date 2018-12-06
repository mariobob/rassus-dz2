package hr.fer.ztel.rassus.dz2;

import hr.fer.ztel.rassus.dz2.thread.Node;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

@Log4j2
public class NodeConsole {

    private static final Path NETWORK_CONFIG_PATH = Paths.get("src/main/resources/network.config");

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
        // Read all lines from config
        List<String> lines = Files.readAllLines(NETWORK_CONFIG_PATH, StandardCharsets.UTF_8);

        // Iterate over lines to find a record with the specified name
        for (int i = 0, n = lines.size(); i < n; i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) continue;

            // Split line into tokens and check if name matches
            String[] tokens = line.split("\\s+");
            if (tokens[0].equals(name)) {
                int port = Integer.parseInt(tokens[1]);
                double lossRate = Double.parseDouble(tokens[2]);
                int averageDelay = Integer.parseInt(tokens[3]);
                return new Node(name, port, lossRate, averageDelay, i, n);
            }
        }

        return null;
    }

}

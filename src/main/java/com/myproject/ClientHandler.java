package com.myproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

/**
 * ClientHandler - Handles a single client connection in its own thread.
 * 
 * Phase 3: Per-client request handler
 * - Reads commands from client
 * - Parses and validates commands
 * - Executes queries via QueryProtocol
 * - Sends responses back to client
 * - Handles disconnection
 * 
 * @author Muhammad
 * @version 1.0 - Phase 3
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final DatabaseManager dbManager;
    private final int clientId;
    private final QueryServer server;
    private QueryProtocol protocol;

    /**
     * Creates a new ClientHandler.
     * 
     * @param clientSocket Client's socket connection
     * @param dbManager    Database manager for queries
     * @param clientId     Unique client identifier
     * @param server       Reference to server (for disconnect notification)
     */
    public ClientHandler(Socket clientSocket, DatabaseManager dbManager, int clientId, QueryServer server) {
        this.clientSocket = clientSocket;
        this.dbManager = dbManager;
        this.clientId = clientId;
        this.server = server;
        this.protocol = new QueryProtocol(dbManager);
    }

    /**
     * Main thread execution method.
     * Handles the client's entire session.
     */
    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            // Send welcome message
            out.println("========================================");
            out.println("📁 File Indexer Query Server");
            out.println("========================================");
            out.println("Client ID: #" + clientId);
            out.println("Type 'HELP' for commands, 'QUIT' to exit");
            out.println("========================================");

            // Command processing loop
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String command = inputLine.trim();

                // Skip empty lines
                if (command.isEmpty()) {
                    continue;
                }

                System.out.println("📥 Client #" + clientId + ": " + command);

                // Check for QUIT command
                if (command.equalsIgnoreCase("QUIT")) {
                    out.println("👋 Goodbye!");
                    break;
                }

                // Process command through protocol
                String response = protocol.processCommand(command);
                out.println(response);
            }

        } catch (IOException e) {
            System.err.println("❌ Client #" + clientId + " error: " + e.getMessage());
        } finally {
            // Clean up
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("⚠️  Error closing socket for client #" + clientId);
            }
            server.onClientDisconnected(clientId);
        }
    }
}

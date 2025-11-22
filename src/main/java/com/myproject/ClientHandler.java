package com.myproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final DatabaseManager dbManager;
    private final int clientId;
    private final QueryServer server;
    private QueryProtocol protocol;

    public ClientHandler(Socket clientSocket, DatabaseManager dbManager, int clientId, QueryServer server) {
        this.clientSocket = clientSocket;
        this.dbManager = dbManager;
        this.clientId = clientId;
        this.server = server;
        this.protocol = new QueryProtocol(dbManager);
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            out.println("========================================");
            out.println("📁 File Indexer Query Server");
            out.println("========================================");
            out.println("Client ID: #" + clientId);
            out.println("Type 'HELP' for commands, 'QUIT' to exit");
            out.println("========================================");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String command = inputLine.trim();

                if (command.isEmpty()) {
                    continue;
                }

                System.out.println("📥 Client #" + clientId + ": " + command);

                if (command.equalsIgnoreCase("QUIT")) {
                    out.println("👋 Goodbye!");
                    break;
                }

                String response = protocol.processCommand(command);
                out.println(response);
            }

        } catch (IOException e) {
            System.err.println("❌ Client #" + clientId + " error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("⚠️  Error closing socket for client #" + clientId);
            }
            server.onClientDisconnected(clientId);
        }
    }
}

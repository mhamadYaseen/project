package com.myproject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * QueryServer - TCP server that handles client queries for file searches.
 * 
 * Phase 3: Multi-client TCP server
 * - Listens on a port for client connections
 * - Creates a new thread for each client
 * - Supports multiple concurrent clients
 * - Graceful shutdown
 * 
 * @author Muhammad
 * @version 1.0 - Phase 3
 */
public class QueryServer {

    private final int port;
    private final DatabaseManager dbManager;
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private volatile boolean running = false;
    private final AtomicInteger activeClients = new AtomicInteger(0);

    /**
     * Creates a QueryServer on the specified port.
     * 
     * @param port      Port to listen on (e.g., 8080)
     * @param dbManager Database manager for queries
     */
    public QueryServer(int port, DatabaseManager dbManager) {
        this.port = port;
        this.dbManager = dbManager;
        this.clientThreadPool = Executors.newCachedThreadPool();
    }

    /**
     * Starts the server and begins accepting clients.
     * This method blocks until the server is stopped.
     * 
     * @throws IOException if server cannot start
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        System.out.println("\n========================================");
        System.out.println("🚀 Query Server Started");
        System.out.println("========================================");
        System.out.println("📡 Listening on port: " + port);
        System.out.println("🔗 Waiting for clients...");
        System.out.println("========================================\n");

        // Main accept loop
        while (running) {
            try {
                // Accept client connection (blocks here)
                Socket clientSocket = serverSocket.accept();

                int clientId = activeClients.incrementAndGet();
                System.out.println("✅ Client #" + clientId + " connected from " +
                        clientSocket.getInetAddress().getHostAddress());

                // Handle client in separate thread
                ClientHandler handler = new ClientHandler(clientSocket, dbManager, clientId, this);
                clientThreadPool.execute(handler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("❌ Error accepting client: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Stops the server gracefully.
     * Waits for active clients to disconnect.
     */
    public void stop() {
        System.out.println("\n🛑 Shutting down server...");
        running = false;

        try {
            // Close server socket (stops accepting new clients)
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            // Shutdown thread pool gracefully
            clientThreadPool.shutdown();
            if (!clientThreadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                clientThreadPool.shutdownNow();
            }

            System.out.println("✅ Server stopped successfully");
        } catch (IOException | InterruptedException e) {
            System.err.println("⚠️  Error during shutdown: " + e.getMessage());
            clientThreadPool.shutdownNow();
        }
    }

    /**
     * Called when a client disconnects.
     * 
     * @param clientId ID of the disconnected client
     */
    public void onClientDisconnected(int clientId) {
        activeClients.decrementAndGet();
        System.out.println("👋 Client #" + clientId + " disconnected. Active clients: " +
                activeClients.get());
    }

    /**
     * Returns the number of currently connected clients.
     * 
     * @return Number of active clients
     */
    public int getActiveClientCount() {
        return activeClients.get();
    }

    public boolean isRunning() {
        return running;
    }
}

package com.myproject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryServer {

    private final int port;
    private final DatabaseManager dbManager;
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private volatile boolean running = false;
    private final AtomicInteger activeClients = new AtomicInteger(0);

    public QueryServer(int port, DatabaseManager dbManager) {
        this.port = port;
        this.dbManager = dbManager;
        this.clientThreadPool = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        System.out.println("\n========================================");
        System.out.println("🚀 Query Server Started");
        System.out.println("========================================");
        System.out.println("📡 Listening on port: " + port);
        System.out.println("🔗 Waiting for clients...");
        System.out.println("========================================\n");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();

                int clientId = activeClients.incrementAndGet();
                System.out.println("✅ Client #" + clientId + " connected from " +
                        clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket, dbManager, clientId, this);
                clientThreadPool.execute(handler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("❌ Error accepting client: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        System.out.println("\n🛑 Shutting down server...");
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

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

    public void onClientDisconnected(int clientId) {
        activeClients.decrementAndGet();
        System.out.println("👋 Client #" + clientId + " disconnected. Active clients: " +
                activeClients.get());
    }

    public int getActiveClientCount() {
        return activeClients.get();
    }

    public boolean isRunning() {
        return running;
    }
}

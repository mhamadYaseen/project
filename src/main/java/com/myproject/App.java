package com.myproject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();

        switch (command) {
            case "test":
                handleTest();
                break;

            case "scan":
                handleScan(args);
                break;

            case "server":
                handleServer(args);
                break;

            case "help":
            case "--help":
            case "-h":
                printUsage();
                break;

            default:
                System.err.println("❌ Unknown command: " + command);
                printUsage();
        }
    }

    private static void handleTest() {
        System.out.println("========================================");
        System.out.println("📁 File Indexer - Phase 1 Testing");
        System.out.println("========================================\n");

        DatabaseManager dbManager = new DatabaseManager();

        System.out.println("🔌 Test 1: Database Connection");
        System.out.println("----------------------------------------");
        if (!dbManager.testConnection()) {
            System.err.println("❌ Cannot connect to database! Check if XAMPP MySQL is running.");
            return;
        }
        System.out.println();

        System.out.println("🗑️  Test 2: Clear Old Data");
        System.out.println("----------------------------------------");
        dbManager.clearDatabase();
        System.out.println();

        System.out.println("➕ Test 3: Adding Sample Files");
        System.out.println("----------------------------------------");

        dbManager.addFile("/Users/muhammad/Documents/project_report.pdf",
                1048576, System.currentTimeMillis(), "pdf");
        dbManager.addFile("/Users/muhammad/Code/App.java",
                5432, System.currentTimeMillis() - 86400000, "java");
        dbManager.addFile("/Users/muhammad/Documents/notes.txt",
                2048, System.currentTimeMillis() - 172800000, "txt");
        dbManager.addFile("/Users/muhammad/Pictures/vacation.jpg",
                2097152, System.currentTimeMillis(), "jpg");
        dbManager.addFile("/Users/muhammad/Code/DatabaseManager.java",
                8192, System.currentTimeMillis(), "java");
        dbManager.addFile("/Users/muhammad/Documents/budget_report.xlsx",
                15000, System.currentTimeMillis(), "xlsx");

        System.out.println();

        System.out.println("🔍 Test 4: Search by Name (keyword: 'report')");
        System.out.println("----------------------------------------");
        List<FileMetadata> nameResults = dbManager.searchByName("report");
        nameResults.forEach(f -> System.out.println(f.getPath() + " [" + f.getFormattedSize() + "]"));
        System.out.println();

        System.out.println("🔍 Test 5: Search by Extension (.java)");
        System.out.println("----------------------------------------");
        List<FileMetadata> extResults = dbManager.searchByExtension("java");
        extResults.forEach(f -> System.out.println(f.getPath() + " [" + f.getFormattedSize() + "]"));
        System.out.println();

        System.out.println("📊 Test 6: Database Statistics");
        System.out.println("----------------------------------------");
        Map<String, Object> stats = dbManager.getStats();

        System.out.println("Total Files: " + stats.get("totalFiles"));
        System.out.println("Total Size: " + formatBytes((Long) stats.get("totalSize")));
        System.out.println("\nFiles by Extension:");

        @SuppressWarnings("unchecked")
        Map<String, Integer> byExt = (Map<String, Integer>) stats.get("by_extension");
        byExt.forEach((ext, count) -> System.out.println("  ." + ext + ": " + count + " files"));

        System.out.println();
        System.out.println("========================================");
        System.out.println("✅ Phase 1 Testing Complete!");
        System.out.println("========================================");
        System.out.println("\n💡 Now check phpMyAdmin to see the data:");
        System.out.println("   http://localhost/phpmyadmin/");
        System.out.println("   Navigate to: file_indexer > files table\n");
    }

    private static void handleScan(String[] args) {
        if (args.length < 2) {
            System.err.println("❌ Error: Directory path required");
            System.err.println("Usage: java -jar FileIndexer.jar scan <directory> [--threads N]");
            System.err.println("Example: java -jar FileIndexer.jar scan /Users/muhammad/Documents");
            return;
        }

        String directoryPath = args[1];
        int threadCount = 4;

        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("--threads") && i + 1 < args.length) {
                try {
                    threadCount = Integer.parseInt(args[i + 1]);
                    if (threadCount < 1 || threadCount > 32) {
                        System.err.println("⚠️  Warning: Thread count should be between 1 and 32. Using default (4).");
                        threadCount = 4;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("⚠️  Warning: Invalid thread count. Using default (4).");
                }
                break;
            }
        }

        DatabaseManager dbManager = new DatabaseManager();

        if (!dbManager.testConnection()) {
            System.err.println("❌ Cannot connect to database!");
            System.err.println("Make sure XAMPP MySQL is running and database 'file_indexer' exists.");
            return;
        }

        FileScanner scanner = new FileScanner(dbManager, threadCount);

        try {
            FileScanner.ScanResult result = scanner.scan(directoryPath);

            result.printSummary();

            System.out.println("📊 Database Statistics:");
            System.out.println("----------------------------------------");
            Map<String, Object> stats = dbManager.getStats();
            System.out.println("Total Files in Database: " + stats.get("totalFiles"));
            System.out.println("Total Size: " + formatBytes((Long) stats.get("totalSize")));

            System.out.println("\nTop File Types:");
            @SuppressWarnings("unchecked")
            Map<String, Integer> byExt = (Map<String, Integer>) stats.get("by_extension");
            byExt.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(10)
                    .forEach(entry -> System.out.println("  ." + entry.getKey() + ": " + entry.getValue() + " files"));
            System.out.println();

        } catch (IOException e) {
            System.err.println("❌ Scan failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleServer(String[] args) {
        int port = 8080;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                    if (port < 1024 || port > 65535) {
                        System.err.println("❌ Port must be between 1024 and 65535");
                        return;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("❌ Invalid port number: " + args[i + 1]);
                    return;
                }
            }
        }

        DatabaseManager dbManager = new DatabaseManager();
        if (!dbManager.testConnection()) {
            System.err.println("❌ Cannot connect to database. Server not started.");
            return;
        }
        System.out.println("✅ Database connection successful!\n");

        QueryServer server = new QueryServer(port, dbManager);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚠️  Shutdown signal received...");
            server.stop();
        }));

        try {
            server.start();
        } catch (Exception e) {
            System.err.println("❌ Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("\n========================================");
        System.out.println("📁 File Indexer - Usage");
        System.out.println("========================================\n");
        System.out.println("COMMANDS:");
        System.out.println();
        System.out.println("  test");
        System.out.println("    Tests Phase 1 database operations with sample data");
        System.out.println("    Example: mvn exec:java -Dexec.args=\"test\"");
        System.out.println();
        System.out.println("  scan <directory> [--threads N]");
        System.out.println("    Scans directory recursively and indexes all files");
        System.out.println("    Options:");
        System.out.println("      --threads N : Use N threads (default: 4, max: 32)");
        System.out.println("    Example: mvn exec:java -Dexec.args=\"scan /Users/muhammad/Documents\"");
        System.out.println("    Example: mvn exec:java -Dexec.args=\"scan ~/Desktop --threads 8\"");
        System.out.println();
        System.out.println("  server [--port N]");
        System.out.println("    Starts TCP query server (Phase 3 - Coming Soon)");
        System.out.println("    Example: mvn exec:java -Dexec.args=\"server --port 9090\"");
        System.out.println();
        System.out.println("  help");
        System.out.println("    Shows this help message");
        System.out.println();
        System.out.println("RUNNING FROM MAVEN:");
        System.out.println("  mvn exec:java -Dexec.args=\"test\"");
        System.out.println("  mvn exec:java -Dexec.args=\"scan /path/to/directory\"");
        System.out.println();
        System.out.println("BUILDING JAR:");
        System.out.println("  mvn clean package");
        System.out.println("  java -jar target/FileIndexer-1.0-SNAPSHOT.jar scan /path/to/dir");
        System.out.println();
        System.out.println("========================================\n");
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

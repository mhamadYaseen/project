package com.myproject;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FileScanner {

    private final int threadCount;
    private static final int BATCH_SIZE = 100;
    private final DatabaseManager dbManager;
    private ExecutorService executorService;
    private final AtomicInteger filesFound = new AtomicInteger(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<FileMetadata> fileQueue;

    public FileScanner(DatabaseManager dbManager) {
        this(dbManager, 4);
    }

    public FileScanner(DatabaseManager dbManager, int threadCount) {
        this.dbManager = dbManager;
        this.threadCount = threadCount;
        this.fileQueue = new ConcurrentLinkedQueue<>();
    }

    public ScanResult scan(String directoryPath) throws IOException {
        Path startPath = Paths.get(directoryPath);

        if (!Files.exists(startPath)) {
            throw new IOException("Path does not exist: " + directoryPath);
        }

        if (!Files.isDirectory(startPath)) {
            throw new IOException("Path is not a directory: " + directoryPath);
        }

        if (!Files.isReadable(startPath)) {
            throw new IOException("Directory is not readable: " + directoryPath);
        }

        System.out.println("\n========================================");
        System.out.println("📁 Starting File Scan");
        System.out.println("========================================");
        System.out.println("📂 Directory: " + startPath.toAbsolutePath());
        System.out.println("🧵 Threads: " + threadCount);
        System.out.println("========================================\n");

        filesFound.set(0);
        totalBytes.set(0);
        errorCount.set(0);
        fileQueue.clear();

        long startTime = System.currentTimeMillis();

        try {
            executorService = Executors.newFixedThreadPool(threadCount);

            walkDirectoryTree(startPath);

            shutdownExecutorService();

            flushQueueToDatabase();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            return new ScanResult(
                    filesFound.get(),
                    totalBytes.get(),
                    errorCount.get(),
                    duration);

        } catch (Exception e) {
            System.err.println("❌ Scanning failed: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Scan failed", e);
        } finally {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        }
    }

    private void walkDirectoryTree(Path startPath) throws IOException {
        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    processFile(file, attrs);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("⚠️  Cannot access: " + file + " (" + exc.getMessage() + ")");
                errorCount.incrementAndGet();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!Files.isReadable(dir)) {
                    System.err.println("⚠️  Cannot read directory: " + dir);
                    errorCount.incrementAndGet();
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void processFile(Path filePath, BasicFileAttributes attrs) {
        try {
            long size = attrs.size();

            FileMetadata metadata = new FileMetadata(filePath, size, attrs.lastModifiedTime());

            fileQueue.offer(metadata);

            int count = filesFound.incrementAndGet();
            totalBytes.addAndGet(size);

            if (count % 100 == 0) {
                System.out.println("📊 Progress: " + count + " files found, " +
                        formatBytes(totalBytes.get()) + " scanned");
            }

            if (fileQueue.size() >= BATCH_SIZE) {
                flushQueueToDatabase();
            }

        } catch (Exception e) {
            System.err.println("⚠️  Error processing file: " + filePath + " - " + e.getMessage());
            errorCount.incrementAndGet();
        }
    }

    private synchronized void flushQueueToDatabase() {
        if (fileQueue.isEmpty()) {
            return;
        }

        List<FileMetadata> batch = new ArrayList<>();
        FileMetadata file;
        while ((file = fileQueue.poll()) != null) {
            batch.add(file);
        }

        if (!batch.isEmpty()) {
            dbManager.addFileBatch(batch);
        }
    }

    private void shutdownExecutorService() {
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();

                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("⚠️  Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static class ScanResult {
        private final int filesFound;
        private final long totalBytes;
        private final int errors;
        private final long durationMs;

        public ScanResult(int filesFound, long totalBytes, int errors, long durationMs) {
            this.filesFound = filesFound;
            this.totalBytes = totalBytes;
            this.errors = errors;
            this.durationMs = durationMs;
        }

        public int getFilesFound() {
            return filesFound;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public int getErrors() {
            return errors;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void printSummary() {
            System.out.println("\n========================================");
            System.out.println("✅ Scan Complete!");
            System.out.println("========================================");
            System.out.println("📄 Files Found: " + filesFound);
            System.out.println("💾 Total Size: " + formatBytes(totalBytes));
            System.out.println("⏱️  Duration: " + (durationMs / 1000.0) + " seconds");
            System.out.println("⚡ Speed: " + (filesFound / Math.max(1, durationMs / 1000.0)) + " files/sec");
            if (errors > 0) {
                System.out.println("⚠️  Errors: " + errors);
            }
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
}

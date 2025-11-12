package com.myproject;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FileScanner - Coordinates concurrent file scanning and indexing.
 * 
 * This class is the heart of Phase 2. It:
 * 1. Walks through directory trees recursively
 * 2. Distributes work across multiple threads
 * 3. Collects file metadata
 * 4. Batch inserts into database
 * 
 * Concurrency Strategy:
 * - Uses ExecutorService with fixed thread pool
 * - Thread-safe collections for coordination
 * - AtomicInteger for progress tracking
 * - Batch processing for database efficiency
 * 
 * @author Muhammad
 * @version 1.0 - Phase 2
 */
public class FileScanner {

    // ========== CONFIGURATION ==========

    /**
     * Number of threads in the thread pool.
     * Default: 4 threads (good balance for most systems)
     */
    private final int threadCount;

    /**
     * Batch size for database inserts.
     * Files are collected in batches and inserted together for performance.
     */
    private static final int BATCH_SIZE = 100;

    // ========== DEPENDENCIES ==========

    /**
     * Database manager for storing file metadata
     */
    private final DatabaseManager dbManager;

    /**
     * Thread pool for concurrent scanning
     */
    private ExecutorService executorService;

    // ========== STATISTICS (Thread-Safe Counters) ==========

    /**
     * Total files found during scanning
     * AtomicInteger ensures thread-safe increment
     */
    private final AtomicInteger filesFound = new AtomicInteger(0);

    /**
     * Total bytes scanned
     * AtomicLong for large numbers
     */
    private final AtomicLong totalBytes = new AtomicLong(0);

    /**
     * Number of errors encountered
     */
    private final AtomicInteger errorCount = new AtomicInteger(0);

    // ========== THREAD-SAFE COLLECTIONS ==========

    /**
     * Queue to collect file metadata from scanner threads
     * ConcurrentLinkedQueue is thread-safe without blocking
     */
    private final ConcurrentLinkedQueue<FileMetadata> fileQueue;

    // ========== CONSTRUCTOR ==========

    /**
     * Creates a new FileScanner with default thread count (4).
     * 
     * @param dbManager DatabaseManager for storing files
     */
    public FileScanner(DatabaseManager dbManager) {
        this(dbManager, 4);
    }

    /**
     * Creates a new FileScanner with custom thread count.
     * 
     * @param dbManager   DatabaseManager for storing files
     * @param threadCount Number of threads to use
     */
    public FileScanner(DatabaseManager dbManager, int threadCount) {
        this.dbManager = dbManager;
        this.threadCount = threadCount;
        this.fileQueue = new ConcurrentLinkedQueue<>();
    }

    // ========== MAIN SCANNING METHOD ==========

    /**
     * Scans a directory recursively and indexes all files.
     * 
     * This is the main public API method. It:
     * 1. Validates the input path
     * 2. Creates thread pool
     * 3. Walks directory tree
     * 4. Processes files concurrently
     * 5. Batch inserts to database
     * 6. Cleans up resources
     * 
     * @param directoryPath Path to scan
     * @return ScanResult containing statistics
     * @throws IOException if path doesn't exist or isn't readable
     */
    public ScanResult scan(String directoryPath) throws IOException {
        Path startPath = Paths.get(directoryPath);

        // Validate path
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

        // Reset statistics
        filesFound.set(0);
        totalBytes.set(0);
        errorCount.set(0);
        fileQueue.clear();

        long startTime = System.currentTimeMillis();

        try {
            // Create thread pool
            executorService = Executors.newFixedThreadPool(threadCount);

            // Walk directory tree and process files
            walkDirectoryTree(startPath);

            // Wait for all tasks to complete
            shutdownExecutorService();

            // Insert remaining files in queue
            flushQueueToDatabase();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Create and return result
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
            // Ensure executor is shut down
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        }
    }

    // ========== DIRECTORY TRAVERSAL ==========

    /**
     * Walks the directory tree and processes each file.
     * 
     * Uses Java NIO.2 Files.walkFileTree() for efficient traversal.
     * Handles errors gracefully without stopping the entire scan.
     * 
     * @param startPath Starting directory
     * @throws IOException if traversal fails
     */
    private void walkDirectoryTree(Path startPath) throws IOException {
        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // Process regular files only (not directories, symlinks, etc.)
                if (attrs.isRegularFile()) {
                    processFile(file, attrs);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Handle permission denied, etc.
                System.err.println("⚠️  Cannot access: " + file + " (" + exc.getMessage() + ")");
                errorCount.incrementAndGet();
                return FileVisitResult.CONTINUE; // Continue despite error
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Check if directory is accessible
                if (!Files.isReadable(dir)) {
                    System.err.println("⚠️  Cannot read directory: " + dir);
                    errorCount.incrementAndGet();
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ========== FILE PROCESSING ==========

    /**
     * Processes a single file: extracts metadata and adds to queue.
     * 
     * This method is called for each file found during traversal.
     * It's designed to be fast and non-blocking.
     * 
     * @param filePath Path to the file
     * @param attrs    File attributes (from Files.walkFileTree)
     */
    private void processFile(Path filePath, BasicFileAttributes attrs) {
        try {
            // Get file size
            long size = attrs.size();

            // Create metadata object (includes extraction of last modified time)
            FileMetadata metadata = new FileMetadata(filePath, size, attrs.lastModifiedTime());

            // Add to queue (thread-safe operation)
            fileQueue.offer(metadata);

            // Update statistics (thread-safe atomic operations)
            int count = filesFound.incrementAndGet();
            totalBytes.addAndGet(size);

            // Show progress every 100 files
            if (count % 100 == 0) {
                System.out.println("📊 Progress: " + count + " files found, " +
                        formatBytes(totalBytes.get()) + " scanned");
            }

            // Batch insert when queue reaches threshold
            if (fileQueue.size() >= BATCH_SIZE) {
                flushQueueToDatabase();
            }

        } catch (Exception e) {
            System.err.println("⚠️  Error processing file: " + filePath + " - " + e.getMessage());
            errorCount.incrementAndGet();
        }
    }

    // ========== DATABASE OPERATIONS ==========

    /**
     * Flushes the file queue to the database in a batch operation.
     * 
     * This method is synchronized to prevent multiple threads from
     * flushing simultaneously (which would cause race conditions).
     * 
     * Thread-safe: Only one thread can flush at a time
     */
    private synchronized void flushQueueToDatabase() {
        if (fileQueue.isEmpty()) {
            return;
        }

        // Drain queue into a list
        List<FileMetadata> batch = new ArrayList<>();
        FileMetadata file;
        while ((file = fileQueue.poll()) != null) {
            batch.add(file);
        }

        // Batch insert to database
        if (!batch.isEmpty()) {
            dbManager.addFileBatch(batch);
        }
    }

    // ========== EXECUTOR MANAGEMENT ==========

    /**
     * Shuts down the executor service gracefully.
     * 
     * This waits for all tasks to complete before shutting down.
     * Gives tasks up to 60 seconds to finish.
     */
    private void shutdownExecutorService() {
        executorService.shutdown();

        try {
            // Wait for all tasks to complete (max 60 seconds)
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                // Force shutdown if tasks don't finish
                executorService.shutdownNow();

                // Wait a bit more for forced shutdown
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("⚠️  Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ========== UTILITY METHODS ==========

    /**
     * Formats bytes into human-readable format.
     * 
     * @param bytes Number of bytes
     * @return Formatted string (e.g., "1.5 MB")
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ========== RESULT CLASS ==========

    /**
     * ScanResult - Contains statistics about a completed scan.
     * This is returned to the caller after scanning completes.
     */
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

        /**
         * Prints a formatted summary of the scan results.
         */
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

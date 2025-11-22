package com.myproject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * DatabaseManager - Handles all database operations for the File Indexer
 * project.
 * 
 * This class encapsulates all JDBC logic and provides a clean API for:
 * - Connecting to MySQL database
 * - Inserting file metadata (CREATE)
 * - Searching for files (READ)
 * - Getting statistics (READ)
 * - Deleting files (DELETE)
 * 
 * Design Pattern: This follows the "Data Access Object" (DAO) pattern,
 * separating database logic from business logic.
 * 
 * @author Your Name
 * @version 1.0
 */
public class DatabaseManager {

    // ========== DATABASE CONNECTION CONFIGURATION ==========

    /**
     * JDBC URL - The "address" of our MySQL database
     * Format: jdbc:mysql://[host]:[port]/[database_name]
     * 
     * Breakdown:
     * - jdbc:mysql:// -> Protocol (tells Java to use MySQL via JDBC)
     * - localhost -> Database server address (on this computer)
     * - 3306 -> MySQL default port
     * - file_indexer -> Our database name
     */
    private final String JDBC_URL = "jdbc:mysql://localhost:3306/file_indexer";

    /**
     * Database username - Default XAMPP MySQL username is "root"
     */
    private final String DB_USER = "root";

    /**
     * Database password - Default XAMPP MySQL has NO password (empty string)
     */
    private final String DB_PASSWORD = "";

    // ========== CONNECTION METHOD ==========

    /**
     * Creates and returns a connection to the MySQL database.
     * 
     * This method:
     * 1. Loads the MySQL JDBC driver (though it's auto-loaded in JDBC 4.0+)
     * 2. Establishes a connection to the database
     * 3. Returns the Connection object
     * 
     * @return Connection object to interact with the database
     * @throws SQLException if connection fails (wrong URL, server down, etc.)
     */
    private Connection connect() throws SQLException {
        try {
            // Step 1: Load the MySQL JDBC driver
            // This is optional in modern JDBC (4.0+) but good for compatibility
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (ClassNotFoundException e) {
            // This happens if mysql-connector-j is not in your classpath
            throw new SQLException("MySQL JDBC Driver not found! Check your pom.xml", e);
        }

        // Step 2: Establish and return the connection
        // DriverManager asks the loaded driver to create a connection
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    // ========== CREATE OPERATION ==========

    /**
     * Adds a single file record to the database.
     * 
     * This method demonstrates:
     * - Try-with-resources for automatic resource cleanup
     * - PreparedStatement for SQL injection prevention
     * - Setting parameters with proper data types
     * - executeUpdate() for INSERT operations
     * - Exception handling
     * 
     * @param path         File path (e.g., "/Users/muhammad/Documents/report.pdf")
     * @param size         File size in bytes (must use long for large files)
     * @param lastModified Last modified timestamp in milliseconds
     * @param ext          File extension (e.g., "pdf", "txt")
     * @return true if file was added successfully, false otherwise
     */
    public boolean addFile(String path, long size, long lastModified, String ext) {
        // SQL INSERT statement with placeholders (?)
        // The ? are filled in by PreparedStatement to prevent SQL injection
        String sql = "INSERT INTO files(path, size, last_modified, ext) VALUES(?, ?, ?, ?)";

        /*
         * Try-with-resources statement:
         * - Automatically closes Connection and PreparedStatement when done
         * - Even if an exception occurs, resources are closed
         * - Much safer than manual close() in finally block
         * 
         * Syntax: try (Resource1; Resource2; ...) { }
         */
        try (
                Connection conn = this.connect(); // Get database connection
                PreparedStatement pstmt = conn.prepareStatement(sql) // Prepare the SQL statement
        ) {
            // Fill in the ? placeholders (index starts at 1, not 0!)
            pstmt.setString(1, path); // First ? = path
            pstmt.setLong(2, size); // Second ? = size (use Long for BIGINT)
            pstmt.setLong(3, lastModified); // Third ? = last_modified
            pstmt.setString(4, ext); // Fourth ? = ext

            // Execute the INSERT statement
            // executeUpdate() returns the number of rows affected
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Successfully added file: " + path);
                return true;
            }

        } catch (SQLException e) {
            // Something went wrong (connection failed, SQL error, etc.)
            System.err.println("❌ Error adding file to database: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for debugging
        }

        return false;
    }

    // ========== READ OPERATIONS ==========

    /**
     * Searches for files by name (partial match).
     * Uses SQL LIKE operator for pattern matching.
     * 
     * Example: searchByName("report") will find:
     * - "/docs/report.pdf"
     * - "/files/annual_report_2024.xlsx"
     * - "/reports/summary.txt"
     * 
     * @param keyword Keyword to search for in file paths
     * @return List of matching FileMetadata objects (empty list if none found)
     */
    public List<FileMetadata> searchByName(String keyword) {
        List<FileMetadata> results = new ArrayList<>();

        // SQL LIKE operator: % means "any characters"
        // Example: WHERE path LIKE '%report%' finds any path containing "report"
        String sql = "SELECT path, size, last_modified, ext FROM files WHERE path LIKE ?";

        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Add % wildcards to search for keyword anywhere in the path
            pstmt.setString(1, "%" + keyword + "%");

            // executeQuery() returns a ResultSet (the query results)
            ResultSet rs = pstmt.executeQuery();

            /*
             * ResultSet is like a cursor pointing to rows:
             * - Initially points BEFORE the first row
             * - rs.next() moves to next row, returns false when no more rows
             * - Use rs.getString(), rs.getLong(), etc. to get column values
             */
            while (rs.next()) {
                String path = rs.getString("path");
                long size = rs.getLong("size");
                long modified = rs.getLong("last_modified");
                String ext = rs.getString("ext");

                // Create FileMetadata object
                FileMetadata metadata = new FileMetadata(path, size, modified, ext != null ? ext : "");
                results.add(metadata);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error searching by name: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Searches for files by extension (exact match).
     * 
     * Example: searchByExtension("pdf") finds all PDF files
     * 
     * @param extension File extension to search for (e.g., "pdf", "txt")
     * @return List of matching FileMetadata objects
     */
    public List<FileMetadata> searchByExtension(String extension) {
        List<FileMetadata> results = new ArrayList<>();

        // Exact match: WHERE ext = ?
        String sql = "SELECT path, size, last_modified, ext FROM files WHERE ext = ?";

        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, extension);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String path = rs.getString("path");
                long size = rs.getLong("size");
                long modified = rs.getLong("last_modified");
                String ext = rs.getString("ext");

                FileMetadata metadata = new FileMetadata(path, size, modified, ext != null ? ext : "");
                results.add(metadata);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error searching by extension: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Searches for files by size comparison.
     * 
     * @param size    Size threshold in bytes
     * @param greater true for >, false for <
     * @return List of matching FileMetadata objects
     */
    public List<FileMetadata> searchBySize(long size, boolean greater) {
        List<FileMetadata> results = new ArrayList<>();

        String sql = greater
                ? "SELECT path, size, last_modified, ext FROM files WHERE size > ?"
                : "SELECT path, size, last_modified, ext FROM files WHERE size < ?";

        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, size);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String path = rs.getString("path");
                long fileSize = rs.getLong("size");
                long modified = rs.getLong("last_modified");
                String ext = rs.getString("ext");

                FileMetadata metadata = new FileMetadata(path, fileSize, modified, ext != null ? ext : "");
                results.add(metadata);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error searching by size: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Searches for files within a size range.
     * 
     * Example: searchBySize(1000, 5000) finds files between 1KB and 5KB
     * 
     * @param minSize Minimum file size in bytes (inclusive)
     * @param maxSize Maximum file size in bytes (inclusive)
     * @return List of matching file paths
     */
    public List<String> searchBySizeRange(long minSize, long maxSize) {
        List<String> results = new ArrayList<>();

        // Range query: WHERE size BETWEEN ? AND ?
        String sql = "SELECT path, size, ext FROM files WHERE size BETWEEN ? AND ?";

        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, minSize);
            pstmt.setLong(2, maxSize);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String path = rs.getString("path");
                long size = rs.getLong("size");
                String ext = rs.getString("ext");

                String result = String.format("%s [%s, %d bytes]", path, ext, size);
                results.add(result);
            }

            System.out.println("🔍 Found " + results.size() +
                    " files between " + minSize + " and " + maxSize + " bytes");

        } catch (SQLException e) {
            System.err.println("❌ Error searching by size: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Gets database statistics.
     * 
     * Returns:
     * - Total number of files
     * - Total size of all files (in bytes)
     * - Average size
     * - Max size
     * - Number of unique extensions
     * 
     * @return Map containing statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        try (Connection conn = this.connect()) {

            // Query: Get comprehensive statistics
            String sql = "SELECT COUNT(*) as totalFiles, " +
                    "COALESCE(SUM(size), 0) as totalSize, " +
                    "COALESCE(AVG(size), 0) as avgSize, " +
                    "COALESCE(MAX(size), 0) as maxSize, " +
                    "COUNT(DISTINCT ext) as uniqueExtensions " +
                    "FROM files";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    stats.put("totalFiles", rs.getInt("totalFiles"));
                    stats.put("totalSize", rs.getLong("totalSize"));
                    stats.put("avgSize", rs.getLong("avgSize"));
                    stats.put("maxSize", rs.getLong("maxSize"));
                    stats.put("uniqueExtensions", rs.getInt("uniqueExtensions"));
                }
            }

            // Query: Get file count by extension
            String extSql = "SELECT ext, COUNT(*) as count FROM files GROUP BY ext ORDER BY count DESC";
            Map<String, Integer> byExtension = new HashMap<>();

            try (PreparedStatement pstmt = conn.prepareStatement(extSql)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String ext = rs.getString("ext");
                    int count = rs.getInt("count");
                    byExtension.put(ext != null ? ext : "no_extension", count);
                }
            }

            stats.put("by_extension", byExtension);

            System.out.println("📊 Statistics retrieved successfully");

        } catch (SQLException e) {
            System.err.println("❌ Error getting stats: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    // ========== DELETE OPERATIONS ==========

    /**
     * Deletes a file record by its ID.
     * 
     * @param id The file ID to delete
     * @return true if file was deleted, false otherwise
     */
    public boolean deleteFile(int id) {
        String sql = "DELETE FROM files WHERE id = ?";

        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("🗑️  Deleted file with ID: " + id);
                return true;
            } else {
                System.out.println("⚠️  No file found with ID: " + id);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error deleting file: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Clears all records from the files table (useful for testing).
     * ⚠️ WARNING: This deletes ALL data!
     * 
     * @return Number of rows deleted
     */
    public int clearDatabase() {
        String sql = "DELETE FROM files";

        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int rowsDeleted = pstmt.executeUpdate();
            System.out.println("🗑️  Cleared database: " + rowsDeleted + " rows deleted");
            return rowsDeleted;

        } catch (SQLException e) {
            System.err.println("❌ Error clearing database: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Clears all files from the database.
     * Uses TRUNCATE for better performance on large tables.
     * 
     * @return true if successful, false otherwise
     */
    public boolean clearAllFiles() {
        String sql = "TRUNCATE TABLE files";

        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error clearing database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ========== BATCH OPERATIONS (Phase 2) ==========

    /**
     * Adds multiple files to the database in a single batch operation.
     * This is much faster than calling addFile() multiple times.
     * 
     * Performance comparison:
     * - 1000 individual inserts: ~10-15 seconds
     * - 1 batch insert of 1000: ~0.5-1 second
     * 
     * This method demonstrates:
     * - Batch PreparedStatement operations
     * - Transaction management (commit/rollback)
     * - Thread-safe database access (synchronized)
     * 
     * Thread Safety: This method is synchronized to prevent race conditions
     * when multiple scanner threads try to insert simultaneously.
     * 
     * @param fileList List of FileMetadata objects to insert
     * @return Number of files successfully inserted
     */
    public synchronized int addFileBatch(List<FileMetadata> fileList) {
        if (fileList == null || fileList.isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO files(path, size, last_modified, ext) VALUES(?, ?, ?, ?)";

        Connection conn = null;
        int totalInserted = 0;

        try {
            conn = this.connect();

            // Disable auto-commit to use transactions
            // This allows us to rollback if something goes wrong
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // Add each file to the batch
                for (FileMetadata file : fileList) {
                    pstmt.setString(1, file.getPath());
                    pstmt.setLong(2, file.getSize());
                    pstmt.setLong(3, file.getLastModified());
                    pstmt.setString(4, file.getExtension());

                    // Add to batch (doesn't execute yet)
                    pstmt.addBatch();
                }

                // Execute all inserts at once
                int[] results = pstmt.executeBatch();

                // Count successful inserts
                for (int result : results) {
                    if (result > 0 || result == Statement.SUCCESS_NO_INFO) {
                        totalInserted++;
                    }
                }

                // Commit the transaction - save all changes
                conn.commit();

                System.out.println("✅ Batch inserted " + totalInserted + " files");

            } catch (SQLException e) {
                // Something went wrong - rollback all changes
                if (conn != null) {
                    try {
                        conn.rollback();
                        System.err.println("⚠️  Transaction rolled back due to error");
                    } catch (SQLException rollbackEx) {
                        System.err.println("❌ Rollback failed: " + rollbackEx.getMessage());
                    }
                }
                System.err.println("❌ Batch insert failed: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.err.println("❌ Connection error: " + e.getMessage());
        } finally {
            // Restore auto-commit and close connection
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("❌ Error closing connection: " + e.getMessage());
                }
            }
        }

        return totalInserted;
    }

    /**
     * Adds a single FileMetadata object to the database.
     * Convenience method that uses the FileMetadata class.
     * 
     * @param fileMetadata FileMetadata object to insert
     * @return true if successfully inserted
     */
    public boolean addFile(FileMetadata fileMetadata) {
        return addFile(
                fileMetadata.getPath(),
                fileMetadata.getSize(),
                fileMetadata.getLastModified(),
                fileMetadata.getExtension());
    }

    // ========== UTILITY METHODS ==========

    /**
     * Tests the database connection.
     * Useful for verifying that MySQL is running and accessible.
     * 
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection() {
        try (Connection conn = this.connect()) {
            System.out.println("✅ Database connection successful!");
            System.out.println("   Database: " + conn.getCatalog());
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed!");
            System.err.println("   " + e.getMessage());
            return false;
        }
    }
}

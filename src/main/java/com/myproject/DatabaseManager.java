package com.myproject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {

    private final String JDBC_URL = "jdbc:mysql://localhost:3306/file_indexer";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "";

    private Connection connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found! Check your pom.xml", e);
        }
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    public boolean addFile(String path, long size, long lastModified, String ext) {
        String sql = "INSERT INTO files(path, size, last_modified, ext) VALUES(?, ?, ?, ?)";

        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, path);
            pstmt.setLong(2, size);
            pstmt.setLong(3, lastModified);
            pstmt.setString(4, ext);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Successfully added file: " + path);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error adding file to database: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public List<FileMetadata> searchByName(String keyword) {
        List<FileMetadata> results = new ArrayList<>();

        String sql = "SELECT path, size, last_modified, ext FROM files WHERE path LIKE ?";

        try (
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");

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
            System.err.println("❌ Error searching by name: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    public List<FileMetadata> searchByExtension(String extension) {
        List<FileMetadata> results = new ArrayList<>();

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

    public List<String> searchBySizeRange(long minSize, long maxSize) {
        List<String> results = new ArrayList<>();

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

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        try (Connection conn = this.connect()) {

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

    public synchronized int addFileBatch(List<FileMetadata> fileList) {
        if (fileList == null || fileList.isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO files(path, size, last_modified, ext) VALUES(?, ?, ?, ?)";

        Connection conn = null;
        int totalInserted = 0;

        try {
            conn = this.connect();

            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                for (FileMetadata file : fileList) {
                    pstmt.setString(1, file.getPath());
                    pstmt.setLong(2, file.getSize());
                    pstmt.setLong(3, file.getLastModified());
                    pstmt.setString(4, file.getExtension());

                    pstmt.addBatch();
                }

                int[] results = pstmt.executeBatch();

                for (int result : results) {
                    if (result > 0 || result == Statement.SUCCESS_NO_INFO) {
                        totalInserted++;
                    }
                }

                conn.commit();

                System.out.println("✅ Batch inserted " + totalInserted + " files");

            } catch (SQLException e) {
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

    public boolean addFile(FileMetadata fileMetadata) {
        return addFile(
                fileMetadata.getPath(),
                fileMetadata.getSize(),
                fileMetadata.getLastModified(),
                fileMetadata.getExtension());
    }

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

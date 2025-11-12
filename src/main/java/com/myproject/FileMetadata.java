package com.myproject;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * FileMetadata - Data class representing file information.
 * 
 * This is a simple POJO (Plain Old Java Object) that holds metadata
 * about a single file. It's used to pass file information between
 * the scanner threads and the database writer.
 * 
 * Design Pattern: This is a "Value Object" or "Data Transfer Object" (DTO)
 * - Immutable (all fields final)
 * - No business logic, just data
 * - Easy to create
 * , pass around, and store
 * 
 * @author Muhammad
 * @version 1.0 - Phase 2
 */
public class FileMetadata {

    // ========== FIELDS ==========

    /**
     * Full absolute path to the file
     * Example: "/Users/muhammad/Documents/report.pdf"
     */
    private final String path;

    /**
     * File size in bytes
     * Uses long to support large files (up to 9.2 exabytes)
     */
    private final long size;

    /**
     * Last modified timestamp in milliseconds since Unix epoch (Jan 1, 1970)
     * Example: 1699747200000 (represents a specific date/time)
     */
    private final long lastModified;

    /**
     * File extension without the dot
     * Examples: "pdf", "java", "txt", "" (empty if no extension)
     */
    private final String extension;

    // ========== CONSTRUCTOR ==========

    /**
     * Creates a new FileMetadata object.
     * 
     * @param path         Full file path
     * @param size         File size in bytes
     * @param lastModified Last modified timestamp (milliseconds)
     * @param extension    File extension (without dot)
     */
    public FileMetadata(String path, long size, long lastModified, String extension) {
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
        this.extension = extension;
    }

    /**
     * Alternative constructor that takes a Path object.
     * This is convenient when working with Java NIO.2 API.
     * 
     * @param filePath     Path object from java.nio.file
     * @param size         File size in bytes
     * @param modifiedTime FileTime object from Files.getLastModifiedTime()
     */
    public FileMetadata(Path filePath, long size, FileTime modifiedTime) {
        this.path = filePath.toAbsolutePath().toString();
        this.size = size;
        this.lastModified = modifiedTime.toMillis();
        this.extension = extractExtension(filePath);
    }

    // ========== GETTERS ==========

    /**
     * Gets the full file path.
     * 
     * @return File path as String
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the file size in bytes.
     * 
     * @return File size
     */
    public long getSize() {
        return size;
    }

    /**
     * Gets the last modified timestamp.
     * 
     * @return Timestamp in milliseconds
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Gets the file extension.
     * 
     * @return Extension string (without dot)
     */
    public String getExtension() {
        return extension;
    }

    // ========== UTILITY METHODS ==========

    /**
     * Extracts the file extension from a Path object.
     * 
     * Examples:
     * - "report.pdf" → "pdf"
     * - "App.java" → "java"
     * - "README" → "" (no extension)
     * - ".gitignore" → "" (hidden file, no extension)
     * - "archive.tar.gz" → "gz" (last extension only)
     * 
     * @param filePath Path to extract extension from
     * @return Extension without dot, or empty string if no extension
     */
    private String extractExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();

        // Find the last dot in the filename
        int lastDotIndex = fileName.lastIndexOf('.');

        // Check if:
        // 1. Dot exists (lastDotIndex > -1)
        // 2. Dot is not the first character (not a hidden file)
        // 3. Dot is not the last character (file doesn't end with dot)
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            // Return everything after the last dot
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        // No valid extension found
        return "";
    }

    /**
     * Gets a human-readable file size.
     * Converts bytes to KB, MB, GB, etc.
     * 
     * Examples:
     * - 500 bytes → "500 B"
     * - 5000 bytes → "4.88 KB"
     * - 5000000 bytes → "4.77 MB"
     * 
     * @return Formatted size string
     */
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Returns a formatted date string.
     * 
     * @return Formatted date string
     */
    public String getFormattedDate() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(lastModified));
    }

    /**
     * Gets the filename (without directory path).
     * 
     * Example: "/Users/muhammad/report.pdf" → "report.pdf"
     * 
     * @return Just the filename
     */
    public String getFileName() {
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return path.substring(lastSeparator + 1);
    }

    // ========== OBJECT METHODS ==========

    /**
     * Returns a string representation of this file metadata.
     * Useful for debugging and logging.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return String.format("FileMetadata{path='%s', size=%s, ext='%s'}",
                getFileName(), getFormattedSize(), extension);
    }

    /**
     * Checks if two FileMetadata objects are equal.
     * Two files are considered equal if they have the same path.
     * 
     * @param obj Object to compare with
     * @return true if paths are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        FileMetadata other = (FileMetadata) obj;
        return path.equals(other.path);
    }

    /**
     * Returns a hash code based on the file path.
     * Required when overriding equals().
     * 
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return path.hashCode();
    }
}

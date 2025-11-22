package com.myproject;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class FileMetadata {

    private final String path;
    private final long size;
    private final long lastModified;
    private final String extension;

    public FileMetadata(String path, long size, long lastModified, String extension) {
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
        this.extension = extension;
    }

    public FileMetadata(Path filePath, long size, FileTime modifiedTime) {
        this.path = filePath.toAbsolutePath().toString();
        this.size = size;
        this.lastModified = modifiedTime.toMillis();
        this.extension = extractExtension(filePath);
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getExtension() {
        return extension;
    }

    private String extractExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();

        int lastDotIndex = fileName.lastIndexOf('.');

        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        return "";
    }

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

    public String getFormattedDate() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(lastModified));
    }

    public String getFileName() {
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return path.substring(lastSeparator + 1);
    }

    @Override
    public String toString() {
        return String.format("FileMetadata{path='%s', size=%s, ext='%s'}",
                getFileName(), getFormattedSize(), extension);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        FileMetadata other = (FileMetadata) obj;
        return path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}

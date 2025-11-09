package com.myproject;

import java.util.List;
import java.util.Map;

/**
 * Main application - Tests the DatabaseManager (Phase 1)
 * 
 * This program demonstrates all CRUD operations:
 * 1. Test database connection
 * 2. Add sample files
 * 3. Search by name, extension, and size
 * 4. Get statistics
 * 
 * @author Your Name
 * @version 1.0 - Phase 1: Database Connection
 */
public class App {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("📁 File Indexer - Phase 1 Testing");
        System.out.println("========================================\n");
        
        // Create DatabaseManager instance
        DatabaseManager dbManager = new DatabaseManager();
        
        // Test 1: Check database connection
        System.out.println("🔌 Test 1: Database Connection");
        System.out.println("----------------------------------------");
        if (!dbManager.testConnection()) {
            System.err.println("❌ Cannot connect to database! Check if XAMPP MySQL is running.");
            return; // Exit if connection fails
        }
        System.out.println();
        
        // Test 2: Clear old data (for clean testing)
        System.out.println("🗑️  Test 2: Clear Old Data");
        System.out.println("----------------------------------------");
        dbManager.clearDatabase();
        System.out.println();
        
        // Test 3: Add sample files
        System.out.println("➕ Test 3: Adding Sample Files");
        System.out.println("----------------------------------------");
        
        // Add various types of files
        dbManager.addFile("/Users/muhammad/Documents/project_report.pdf", 
                         1048576,  // 1 MB
                         System.currentTimeMillis(), 
                         "pdf");
        
        dbManager.addFile("/Users/muhammad/Code/App.java", 
                         5432,     // 5.4 KB
                         System.currentTimeMillis() - 86400000, // 1 day ago
                         "java");
        
        dbManager.addFile("/Users/muhammad/Documents/notes.txt", 
                         2048,     // 2 KB
                         System.currentTimeMillis() - 172800000, // 2 days ago
                         "txt");
        
        dbManager.addFile("/Users/muhammad/Pictures/vacation.jpg", 
                         2097152,  // 2 MB
                         System.currentTimeMillis(), 
                         "jpg");
        
        dbManager.addFile("/Users/muhammad/Code/DatabaseManager.java", 
                         8192,     // 8 KB
                         System.currentTimeMillis(), 
                         "java");
        
        dbManager.addFile("/Users/muhammad/Documents/budget_report.xlsx", 
                         15000,    // 15 KB
                         System.currentTimeMillis(), 
                         "xlsx");
        
        System.out.println();
        
        // Test 4: Search by name (contains "report")
        System.out.println("🔍 Test 4: Search by Name (keyword: 'report')");
        System.out.println("----------------------------------------");
        List<String> nameResults = dbManager.searchByName("report");
        nameResults.forEach(System.out::println);
        System.out.println();
        
        // Test 5: Search by extension (all .java files)
        System.out.println("🔍 Test 5: Search by Extension (.java)");
        System.out.println("----------------------------------------");
        List<String> extResults = dbManager.searchByExtension("java");
        extResults.forEach(System.out::println);
        System.out.println();
        
        // Test 6: Search by size (files between 5KB and 20KB)
        System.out.println("🔍 Test 6: Search by Size (5KB - 20KB)");
        System.out.println("----------------------------------------");
        List<String> sizeResults = dbManager.searchBySize(5000, 20000);
        sizeResults.forEach(System.out::println);
        System.out.println();
        
        // Test 7: Get database statistics
        System.out.println("📊 Test 7: Database Statistics");
        System.out.println("----------------------------------------");
        Map<String, Object> stats = dbManager.getStats();
        
        System.out.println("Total Files: " + stats.get("total_files"));
        System.out.println("Total Size: " + formatBytes((Long) stats.get("total_size")));
        System.out.println("\nFiles by Extension:");
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> byExt = (Map<String, Integer>) stats.get("by_extension");
        byExt.forEach((ext, count) -> 
            System.out.println("  ." + ext + ": " + count + " files")
        );
        
        System.out.println();
        System.out.println("========================================");
        System.out.println("✅ Phase 1 Testing Complete!");
        System.out.println("========================================");
        System.out.println("\n💡 Now check phpMyAdmin to see the data:");
        System.out.println("   http://localhost/phpmyadmin/");
        System.out.println("   Navigate to: file_indexer > files table");
    }
    
    /**
     * Helper method to format bytes in human-readable format
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

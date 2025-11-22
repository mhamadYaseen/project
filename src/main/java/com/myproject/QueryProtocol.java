package com.myproject;

import java.util.List;
import java.util.Map;

public class QueryProtocol {

    private final DatabaseManager dbManager;
    private static final int MAX_RESULTS = 100;

    public QueryProtocol(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public String processCommand(String command) {
        try {
            command = command.trim();
            String upperCommand = command.toUpperCase();

            if (upperCommand.equals("HELP")) {
                return getHelpMessage();
            } else if (upperCommand.equals("STATS")) {
                return handleStats();
            } else if (upperCommand.startsWith("FIND")) {
                return handleFind(command);
            } else {
                return "❌ Unknown command: " + command + "\nType 'HELP' for available commands.";
            }

        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }

    private String handleFind(String command) {
        String[] parts = command.split("\\s+", 4);

        if (parts.length < 4) {
            return "❌ Invalid FIND syntax.\nUsage: FIND <field> <operator> <value>\n" +
                    "Example: FIND name contains report";
        }

        String field = parts[1].toLowerCase();
        String operator = parts[2].toLowerCase();
        String value = parts[3];

        switch (field) {
            case "name":
                if (operator.equals("contains")) {
                    return searchByName(value);
                } else {
                    return "❌ Invalid operator for 'name'. Use 'contains'";
                }

            case "ext":
            case "extension":
                if (operator.equals("is")) {
                    return searchByExtension(value);
                } else {
                    return "❌ Invalid operator for 'ext'. Use 'is'";
                }

            case "size":
                if (operator.equals(">") || operator.equals("<")) {
                    return searchBySize(operator, value);
                } else {
                    return "❌ Invalid operator for 'size'. Use '>' or '<'";
                }

            default:
                return "❌ Unknown field: " + field + "\nSupported fields: name, ext, size";
        }
    }

    private String searchByName(String keyword) {
        List<FileMetadata> results = dbManager.searchByName(keyword);
        return formatResults(results, "Files containing '" + keyword + "' in name");
    }

    private String searchByExtension(String extension) {
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }

        List<FileMetadata> results = dbManager.searchByExtension(extension);
        return formatResults(results, "Files with extension '" + extension + "'");
    }

    private String searchBySize(String operator, String sizeStr) {
        try {
            long size = Long.parseLong(sizeStr);
            List<FileMetadata> results = dbManager.searchBySize(size, operator.equals(">"));

            String comparison = operator.equals(">") ? "larger than" : "smaller than";
            return formatResults(results, "Files " + comparison + " " + formatBytes(size));

        } catch (NumberFormatException e) {
            return "❌ Invalid size value: " + sizeStr + ". Must be a number.";
        }
    }

    private String handleStats() {
        Map<String, Object> stats = dbManager.getStats();

        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("📊 Database Statistics\n");
        sb.append("========================================\n");
        sb.append("📄 Total Files: ").append(stats.get("totalFiles")).append("\n");
        sb.append("💾 Total Size: ").append(formatBytes((Long) stats.get("totalSize"))).append("\n");
        sb.append("📈 Average Size: ").append(formatBytes((Long) stats.get("avgSize"))).append("\n");
        sb.append("📦 Largest File: ").append(formatBytes((Long) stats.get("maxSize"))).append("\n");
        sb.append("📋 Extensions: ").append(stats.get("uniqueExtensions")).append("\n");
        sb.append("========================================");

        return sb.toString();
    }

    private String formatResults(List<FileMetadata> results, String title) {
        if (results.isEmpty()) {
            return "📭 No files found matching your criteria.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("🔍 ").append(title).append("\n");
        sb.append("========================================\n");
        sb.append("Found ").append(results.size()).append(" file(s)");

        if (results.size() > MAX_RESULTS) {
            sb.append(" (showing first ").append(MAX_RESULTS).append(")");
        }

        sb.append("\n========================================\n");

        int count = 0;
        for (FileMetadata file : results) {
            if (count >= MAX_RESULTS) {
                break;
            }

            sb.append(String.format("\n%d. %s\n", count + 1, file.getPath()));
            sb.append(String.format("   Size: %s | Modified: %s\n",
                    file.getFormattedSize(),
                    file.getFormattedDate()));
            count++;
        }

        sb.append("========================================");
        return sb.toString();
    }

    private String getHelpMessage() {
        return "\n========================================\n" +
                "📖 Available Commands\n" +
                "========================================\n" +
                "FIND name contains <keyword>\n" +
                "  Example: FIND name contains report\n\n" +
                "FIND ext is <extension>\n" +
                "  Example: FIND ext is pdf\n\n" +
                "FIND size > <bytes>\n" +
                "  Example: FIND size > 1048576\n\n" +
                "FIND size < <bytes>\n" +
                "  Example: FIND size < 1024\n\n" +
                "STATS\n" +
                "  Show database statistics\n\n" +
                "HELP\n" +
                "  Show this help message\n\n" +
                "QUIT\n" +
                "  Disconnect from server\n" +
                "========================================";
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
}

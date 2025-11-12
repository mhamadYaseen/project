# 🔬 Phase 1: Deep Dive Into JDBC Concepts

## 📌 Concept 1: The JDBC Driver

### What is a JDBC Driver?

A JDBC driver is a **software component** that enables Java applications to interact with a database.

**The Four Types of JDBC Drivers:**

1. **Type 1: JDBC-ODBC Bridge** (Deprecated)

   - Uses ODBC driver
   - Not used anymore

2. **Type 2: Native-API Driver**

   - Uses database-specific native libraries
   - Platform-dependent

3. **Type 3: Network Protocol Driver**

   - Uses middleware server
   - Rarely used

4. **Type 4: Pure Java Driver** ⭐ **We use this!**
   - Written entirely in Java
   - Directly communicates with database
   - Platform-independent
   - MySQL Connector/J is a Type 4 driver

### How the Driver Works

```java
Class.forName("com.mysql.cj.jdbc.Driver");
```

This line **loads the driver class** into memory.

**Note:** In modern JDBC (4.0+), this is actually **optional** because drivers are auto-discovered using the Service Provider Interface (SPI). But it's good practice to include it for clarity and compatibility.

---

## 📌 Concept 2: Connection Pooling (Advanced - For Later)

### Why Connection Pooling?

Creating a database connection is **expensive**:

- Takes time (100-200ms)
- Uses system resources
- Network overhead

**Problem:**

```java
// This is inefficient for many operations
for (int i = 0; i < 1000; i++) {
    Connection conn = DriverManager.getConnection(...); // Slow!
    // Do something
    conn.close();
}
```

**Solution: Connection Pooling**

Instead of creating new connections each time, maintain a **pool** of reusable connections:

```
┌─────────────────────────────────┐
│     Connection Pool             │
├─────────────────────────────────┤
│  [Conn1] [Conn2] [Conn3] ...    │  Pool of ready connections
└─────────────────────────────────┘
         ↑            ↓
    Borrow         Return
         ↓            ↑
┌─────────────────────────────────┐
│     Your Application            │
└─────────────────────────────────┘
```

**For Phase 1:** We'll use simple connections
**For Phase 2+:** We'll implement a connection pool (needed for concurrent operations)

**Popular pooling libraries:**

- HikariCP (fastest)
- Apache DBCP
- C3P0

---

## 📌 Concept 3: SQL Injection Attacks

### What is SQL Injection?

SQL Injection is when an attacker **manipulates your SQL query** by inserting malicious code through user input.

### Example Attack

**Vulnerable Code:**

```java
String userInput = request.getParameter("fileName");
String sql = "SELECT * FROM files WHERE path = '" + userInput + "'";
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(sql);
```

**Normal Input:**

```
userInput = "report.pdf"
SQL becomes: SELECT * FROM files WHERE path = 'report.pdf'
✅ Works fine
```

**Malicious Input:**

```
userInput = "' OR '1'='1"
SQL becomes: SELECT * FROM files WHERE path = '' OR '1'='1'
❌ Returns ALL files! (Because '1'='1' is always true)
```

**Even Worse:**

```
userInput = "'; DROP TABLE files; --"
SQL becomes: SELECT * FROM files WHERE path = ''; DROP TABLE files; --'
💀 DELETES YOUR ENTIRE TABLE!
```

### The Solution: PreparedStatement

```java
String sql = "SELECT * FROM files WHERE path = ?";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setString(1, userInput); // Safely escaped!
ResultSet rs = pstmt.executeQuery();
```

**How it protects you:**

1. SQL query structure is **fixed** (the `?` placeholders are defined)
2. User input is treated as **data**, not **code**
3. Special characters are automatically escaped
4. Database knows what's query and what's data

**Golden Rule:** 🚨 **NEVER concatenate user input into SQL strings!**

---

## 📌 Concept 4: Transaction Management

### What is a Transaction?

A **transaction** is a group of SQL operations that must **all succeed or all fail together**.

**Example Scenario:**
You need to:

1. Delete old file record
2. Insert new file record
3. Update statistics table

**Problem:** What if #2 fails but #1 succeeded? You've lost data!

**Solution: Transactions**

```java
Connection conn = null;
try {
    conn = DriverManager.getConnection(...);

    conn.setAutoCommit(false); // Start transaction

    // Operation 1
    PreparedStatement pstmt1 = conn.prepareStatement("DELETE FROM files WHERE id = ?");
    pstmt1.setInt(1, oldId);
    pstmt1.executeUpdate();

    // Operation 2
    PreparedStatement pstmt2 = conn.prepareStatement("INSERT INTO files...");
    // ... set parameters
    pstmt2.executeUpdate();

    // Operation 3
    PreparedStatement pstmt3 = conn.prepareStatement("UPDATE stats...");
    // ... set parameters
    pstmt3.executeUpdate();

    conn.commit(); // ✅ All succeeded, save changes

} catch (SQLException e) {
    if (conn != null) {
        conn.rollback(); // ❌ Something failed, undo everything
    }
} finally {
    if (conn != null) {
        conn.setAutoCommit(true); // Restore default
        conn.close();
    }
}
```

**ACID Properties:**

- **A**tomicity: All or nothing
- **C**onsistency: Database remains valid
- **I**solation: Transactions don't interfere with each other
- **D**urability: Committed changes are permanent

**For Phase 1:** We don't need transactions yet
**For Phase 2:** We'll use transactions for batch inserts

---

## 📌 Concept 5: Batch Operations

### Why Batch Operations?

**Slow way:**

```java
for (int i = 0; i < 1000; i++) {
    String sql = "INSERT INTO files VALUES(?, ?, ?, ?)";
    PreparedStatement pstmt = conn.prepareStatement(sql);
    pstmt.setString(1, path);
    pstmt.setLong(2, size);
    // ...
    pstmt.executeUpdate(); // 1000 round trips to database!
}
```

**Fast way (batching):**

```java
String sql = "INSERT INTO files VALUES(?, ?, ?, ?)";
PreparedStatement pstmt = conn.prepareStatement(sql);

for (int i = 0; i < 1000; i++) {
    pstmt.setString(1, path);
    pstmt.setLong(2, size);
    // ...
    pstmt.addBatch(); // Add to batch, don't execute yet

    if (i % 100 == 0) {
        pstmt.executeBatch(); // Execute 100 at once
        pstmt.clearBatch();
    }
}
pstmt.executeBatch(); // Execute remaining
```

**Performance improvement:** 10-50x faster for bulk operations!

**For Phase 1:** Single inserts for testing
**For Phase 2:** Batch inserts for scanning thousands of files

---

## 📌 Concept 6: ResultSet Types and Cursors

### Default ResultSet (Forward-only)

```java
ResultSet rs = pstmt.executeQuery();
while (rs.next()) { // Can only move forward
    // Process row
}
// Can't go back to previous rows!
```

### Scrollable ResultSet

```java
PreparedStatement pstmt = conn.prepareStatement(
    sql,
    ResultSet.TYPE_SCROLL_INSENSITIVE,
    ResultSet.CONCUR_READ_ONLY
);
ResultSet rs = pstmt.executeQuery();

rs.next();      // Move forward
rs.previous();  // Move backward
rs.first();     // Jump to first row
rs.last();      // Jump to last row
rs.absolute(5); // Jump to row 5
```

**Types:**

- `TYPE_FORWARD_ONLY` - Default, fastest
- `TYPE_SCROLL_INSENSITIVE` - Can scroll, but doesn't see DB changes
- `TYPE_SCROLL_SENSITIVE` - Can scroll, sees DB changes (rarely supported)

**For this project:** We'll use default forward-only (sufficient for our needs)

---

## 📌 Concept 7: Metadata

### DatabaseMetaData - Information About the Database

```java
Connection conn = DriverManager.getConnection(...);
DatabaseMetaData metaData = conn.getMetaData();

System.out.println("Database: " + metaData.getDatabaseProductName());
System.out.println("Version: " + metaData.getDatabaseProductVersion());
System.out.println("Driver: " + metaData.getDriverName());

// Get list of all tables
ResultSet tables = metaData.getTables(null, null, "%", null);
while (tables.next()) {
    System.out.println("Table: " + tables.getString("TABLE_NAME"));
}
```

### ResultSetMetaData - Information About Query Results

```java
ResultSet rs = pstmt.executeQuery();
ResultSetMetaData rsMetaData = rs.getMetaData();

int columnCount = rsMetaData.getColumnCount();
for (int i = 1; i <= columnCount; i++) {
    System.out.println("Column " + i + ": " + rsMetaData.getColumnName(i));
    System.out.println("  Type: " + rsMetaData.getColumnTypeName(i));
}
```

**Use cases:**

- Dynamic query builders
- Generic database utilities
- Debugging
- Schema validation

---

## 📌 Concept 8: Connection URL Parameters

Our connection URL can have parameters for configuration:

```java
String url = "jdbc:mysql://localhost:3306/file_indexer"
    + "?useSSL=false"              // Disable SSL (for local dev)
    + "&serverTimezone=UTC"        // Set timezone
    + "&allowPublicKeyRetrieval=true"  // For authentication
    + "&useUnicode=true"           // Support Unicode
    + "&characterEncoding=UTF-8";  // Use UTF-8 encoding
```

**Common Parameters:**

- `useSSL` - Enable/disable SSL encryption
- `serverTimezone` - Handle timezone differences
- `autoReconnect` - Reconnect if connection drops
- `maxReconnects` - How many times to retry
- `connectTimeout` - How long to wait for connection (ms)

**For development:** We'll keep it simple with just the basic URL

---

## 📌 Concept 9: Handling NULL Values

SQL `NULL` is special - it means "no value" or "unknown".

```java
ResultSet rs = pstmt.executeQuery();
while (rs.next()) {
    String ext = rs.getString("ext");

    if (ext == null) {
        System.out.println("File has no extension");
    }

    // Or check with wasNull()
    int size = rs.getInt("size");
    if (rs.wasNull()) {
        System.out.println("Size is NULL");
    }
}
```

**Inserting NULL:**

```java
pstmt.setString(1, path);
pstmt.setNull(2, java.sql.Types.VARCHAR); // Explicitly set NULL
```

**Best Practice:** Design your schema to avoid NULLs when possible (use `NOT NULL` constraint)

---

## 📌 Concept 10: Error Codes and Vendor-Specific Exceptions

```java
try {
    // Database operation
} catch (SQLException e) {
    int errorCode = e.getErrorCode();
    String sqlState = e.getSQLState();

    // MySQL-specific error codes
    if (errorCode == 1062) {
        System.out.println("Duplicate key error");
    } else if (errorCode == 1146) {
        System.out.println("Table doesn't exist");
    }

    System.out.println("SQL State: " + sqlState);
    System.out.println("Error Code: " + errorCode);
    System.out.println("Message: " + e.getMessage());
}
```

**Common MySQL Error Codes:**

- 1062: Duplicate entry
- 1146: Table doesn't exist
- 1452: Foreign key constraint fails
- 1054: Unknown column

---

## 🎯 Summary

You now understand:

✅ How JDBC drivers work (Type 4 drivers)
✅ Why PreparedStatement prevents SQL injection
✅ What transactions are and when to use them
✅ How batch operations improve performance
✅ Different types of ResultSets
✅ How to get database and result metadata
✅ Connection URL parameters
✅ Handling NULL values properly
✅ Error codes and debugging SQLException

**Next:** Let's implement DatabaseManager.java with all these concepts!

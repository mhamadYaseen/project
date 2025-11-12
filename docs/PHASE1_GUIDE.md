# 📚 Phase 1: Database Connection - Learning Guide

## 🎯 What You'll Learn in This Phase

1. **JDBC (Java Database Connectivity)** - How Java talks to databases
2. **Connection Management** - Opening and closing database connections safely
3. **PreparedStatement** - Writing safe SQL queries (prevents SQL injection)
4. **Try-with-resources** - Automatic resource cleanup
5. **Exception Handling** - Dealing with database errors gracefully

---

## 📖 Core Concepts Explained

### 1. What is JDBC?

**JDBC** = Java Database Connectivity

Think of JDBC as a **translator** between your Java program and any database (MySQL, PostgreSQL, Oracle, etc.).

```
┌─────────────┐        ┌──────────┐        ┌──────────┐
│ Java Code   │ ─────> │   JDBC   │ ─────> │  MySQL   │
│ (Your App)  │ <───── │  Driver  │ <───── │ Database │
└─────────────┘        └──────────┘        └──────────┘
```

**How it works:**

1. You write Java code using JDBC API
2. JDBC Driver translates it to database-specific commands
3. Database executes the command
4. Result comes back through JDBC to your Java code

---

### 2. The JDBC URL (Connection String)

```java
jdbc:mysql://localhost:3306/file_indexer
```

Let's break this down:

- `jdbc:mysql://` - Protocol (tells Java we're using MySQL via JDBC)
- `localhost` - Server address (database is on your computer)
- `3306` - Port number (MySQL's default port)
- `file_indexer` - Database name (the one we created)


### 3. Connection - The Bridge to Database

```java
Connection conn = DriverManager.getConnection(url, username, password);
```

**What is a Connection?**

- A "phone line" between your Java app and MySQL
- Allows you to send SQL commands and receive results
- Must be **opened** before use and **closed** after use

**Important Rules:**

1. ✅ Always close connections when done (or use try-with-resources)
2. ❌ Don't create too many connections (expensive operation)
3. ✅ Reuse connections when possible (later we'll use connection pooling)

---

### 4. PreparedStatement - Safe SQL Execution

**Bad way (vulnerable to SQL injection):**

```java
String sql = "INSERT INTO files VALUES('" + path + "')"; // DANGEROUS!
Statement stmt = conn.createStatement();
stmt.execute(sql);
```

**Good way (safe):**

```java
String sql = "INSERT INTO files(path, size) VALUES(?, ?)";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setString(1, path);  // Replace first ? with path
pstmt.setLong(2, size);     // Replace second ? with size
pstmt.executeUpdate();
```

**Why PreparedStatement?**

1. **Security:** Prevents SQL injection attacks
2. **Performance:** Database can optimize the query
3. **Readability:** Clean separation of SQL and data

**The `?` placeholders:**

- Each `?` is a placeholder for a value
- You fill them in order using `setString()`, `setInt()`, `setLong()`, etc.
- Index starts at **1** (not 0!)

---

### 5. Try-with-Resources - Automatic Cleanup

**Old way (manual cleanup):**

```java
Connection conn = null;
PreparedStatement pstmt = null;
try {
    conn = DriverManager.getConnection(url, user, pass);
    pstmt = conn.prepareStatement(sql);
    pstmt.executeUpdate();
} catch (SQLException e) {
    e.printStackTrace();
} finally {
    if (pstmt != null) pstmt.close(); // Must manually close
    if (conn != null) conn.close();    // Must manually close
}
```

**New way (automatic):**

```java
try (
    Connection conn = DriverManager.getConnection(url, user, pass);
    PreparedStatement pstmt = conn.prepareStatement(sql)
) {
    pstmt.executeUpdate();
} catch (SQLException e) {
    e.printStackTrace();
}
// Connection and PreparedStatement automatically closed!
```

**How it works:**

- Resources declared in `try(...)` are **AutoCloseable**
- Java automatically calls `.close()` when the try block ends
- Even if an exception occurs, resources are still closed
- Much safer and cleaner!

---

### 6. SQL Data Types vs Java Data Types

When working with databases, you need to match types correctly:

| SQL Type | Java Type | JDBC Method                   | Example              |
| -------- | --------- | ----------------------------- | -------------------- |
| INT      | int       | `setInt()` / `getInt()`       | File ID              |
| BIGINT   | long      | `setLong()` / `getLong()`     | File size, timestamp |
| TEXT     | String    | `setString()` / `getString()` | File path            |
| VARCHAR  | String    | `setString()` / `getString()` | Extension            |

**Why BIGINT for size and last_modified?**

- File sizes can exceed 2GB (INT max = 2,147,483,647 bytes ≈ 2GB)
- Timestamps in Java are milliseconds since 1970 (very large numbers)
- BIGINT can hold values up to 9,223,372,036,854,775,807 (Java `long`)

---

### 7. CRUD Operations

**CRUD** = Create, Read, Update, Delete

Every database application needs these four operations:

1. **Create (INSERT)** - Add new records

   ```sql
   INSERT INTO files(path, size, last_modified, ext) VALUES(?, ?, ?, ?)
   ```

2. **Read (SELECT)** - Query/search records

   ```sql
   SELECT * FROM files WHERE ext = ?
   SELECT * FROM files WHERE path LIKE ?
   ```

3. **Update** - Modify existing records

   ```sql
   UPDATE files SET size = ? WHERE path = ?
   ```

4. **Delete** - Remove records
   ```sql
   DELETE FROM files WHERE id = ?
   ```

---

### 8. ResultSet - Reading Query Results

When you run a SELECT query, you get a **ResultSet**:

```java
String sql = "SELECT * FROM files WHERE ext = ?";
try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
    pstmt.setString(1, "pdf");

    ResultSet rs = pstmt.executeQuery(); // Get results

    while (rs.next()) { // Loop through each row
        int id = rs.getInt("id");
        String path = rs.getString("path");
        long size = rs.getLong("size");

        System.out.println("File: " + path + ", Size: " + size);
    }
}
```

**How ResultSet works:**

- It's like a **cursor** pointing to rows in the result
- `rs.next()` moves to the next row (returns `false` when no more rows)
- `rs.getString("column_name")` gets the value from that column
- You can also use column index: `rs.getString(1)` (starts at 1, not 0)

---

### 9. executeUpdate() vs executeQuery()

**Use `executeUpdate()` for:**

- INSERT
- UPDATE
- DELETE
- Returns: number of rows affected

```java
int rowsAffected = pstmt.executeUpdate();
```

**Use `executeQuery()` for:**

- SELECT
- Returns: ResultSet with the query results

```java
ResultSet rs = pstmt.executeQuery();
```

---

### 10. Exception Handling - SQLException

Database operations can fail for many reasons:

- Database server is down
- Invalid SQL syntax
- Connection timeout
- Table doesn't exist
- Duplicate key violation

**Always catch SQLException:**

```java
try {
    // Database operations
} catch (SQLException e) {
    System.err.println("Database error: " + e.getMessage());
    e.printStackTrace();
}
```

**Common SQLExceptions:**

- `Communications link failure` - Can't reach database server
- `Access denied` - Wrong username/password
- `Unknown database` - Database doesn't exist
- `Duplicate entry` - Trying to insert duplicate primary key

---

## 🏗️ DatabaseManager Architecture

We'll create a `DatabaseManager` class that:

1. **Encapsulates all database logic** - One place for all DB operations
2. **Manages connections** - Opens/closes connections safely
3. **Provides CRUD methods** - Clean API for the rest of the application
4. **Handles errors** - Catches and reports database errors

```
┌────────────────────────────────────────┐
│         DatabaseManager                │
├────────────────────────────────────────┤
│ - JDBC_URL                             │
│ - DB_USER                              │
│ - DB_PASSWORD                          │
├────────────────────────────────────────┤
│ + connect()                            │
│ + addFile(path, size, modified, ext)   │
│ + searchByName(keyword)                │
│ + searchByExtension(ext)               │
│ + searchBySize(minSize, maxSize)       │
│ + getStats()                           │
│ + deleteFile(id)                       │
│ + clearDatabase()                      │
└────────────────────────────────────────┘
```

---

## 🧪 Testing Strategy

We'll test the DatabaseManager with:

1. **Insert Test** - Add a few sample files
2. **Search Test** - Query by name, extension, size
3. **Stats Test** - Get total files, total size, file type breakdown
4. **Verify in phpMyAdmin** - Visual confirmation

---

## 🎓 Key Takeaways

After Phase 1, you'll understand:

✅ How Java connects to MySQL using JDBC
✅ How to write safe SQL queries with PreparedStatement
✅ How to manage resources with try-with-resources
✅ The difference between executeUpdate() and executeQuery()
✅ How to handle ResultSets when reading data
✅ Proper exception handling for database operations
✅ How to organize database code in a manager class

---

## 🚀 Ready to Code!

Now let's implement the `DatabaseManager` class!

Check out:

- `DatabaseManager.java` - The implementation with detailed comments
- `App.java` - Test program to verify everything works
- `PHASE1_CONCEPTS.md` - Deep dive into each concept (next file)

**Next:** Open `DatabaseManager.java` and read through the comments carefully!

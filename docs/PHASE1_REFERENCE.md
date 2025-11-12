# 📚 Learning Resources for Phase 1

## Quick Reference Card

### JDBC Connection

```java
Connection conn = DriverManager.getConnection(url, username, password);
```

### PreparedStatement (Safe)

```java
String sql = "INSERT INTO table VALUES(?, ?)";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setString(1, value);
pstmt.executeUpdate();
```

### Try-with-Resources

```java
try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
    // Use resources
} // Automatically closed
```

### ResultSet

```java
ResultSet rs = pstmt.executeQuery();
while (rs.next()) {
    String value = rs.getString("column_name");
}
```

---

## Common SQL Patterns

### INSERT

```sql
INSERT INTO files(path, size, last_modified, ext) VALUES(?, ?, ?, ?)
```

### SELECT with LIKE

```sql
SELECT * FROM files WHERE path LIKE ?
-- Use: pstmt.setString(1, "%" + keyword + "%");
```

### SELECT with exact match

```sql
SELECT * FROM files WHERE ext = ?
```

### SELECT with range

```sql
SELECT * FROM files WHERE size BETWEEN ? AND ?
```

### COUNT

```sql
SELECT COUNT(*) as total FROM files
```

### GROUP BY

```sql
SELECT ext, COUNT(*) as count FROM files GROUP BY ext
```

---

## Java-SQL Type Mapping

| Java Type | SQL Type     | Set Method     | Get Method     |
| --------- | ------------ | -------------- | -------------- |
| int       | INT          | setInt()       | getInt()       |
| long      | BIGINT       | setLong()      | getLong()      |
| String    | TEXT/VARCHAR | setString()    | getString()    |
| double    | DOUBLE       | setDouble()    | getDouble()    |
| boolean   | BOOLEAN      | setBoolean()   | getBoolean()   |
| Date      | DATE         | setDate()      | getDate()      |
| Timestamp | TIMESTAMP    | setTimestamp() | getTimestamp() |

---

## Exception Handling Pattern

```java
try (Connection conn = connect()) {
    // Database operations
} catch (SQLException e) {
    System.err.println("Error: " + e.getMessage());
    e.printStackTrace();
    // Handle error appropriately
}
```

---

## Best Practices Checklist

- [ ] Always use PreparedStatement (never concatenate SQL)
- [ ] Always use try-with-resources for auto-closing
- [ ] Use BIGINT for large numbers (file sizes, timestamps)
- [ ] Handle SQLException appropriately
- [ ] Close resources in reverse order of creation
- [ ] Use meaningful variable names
- [ ] Add comments explaining complex logic
- [ ] Test with both valid and invalid data
- [ ] Check for null values in ResultSet
- [ ] Use connection pooling for production (not yet, but later)

---

## Debugging Tips

### Check Connection

```java
System.out.println("Connected to: " + conn.getCatalog());
```

### Print SQL Query

```java
System.out.println("Executing: " + pstmt.toString());
```

### Check Rows Affected

```java
int rows = pstmt.executeUpdate();
System.out.println("Rows affected: " + rows);
```

### Inspect SQLException

```java
catch (SQLException e) {
    System.err.println("SQL State: " + e.getSQLState());
    System.err.println("Error Code: " + e.getErrorCode());
    System.err.println("Message: " + e.getMessage());
}
```

---

## Further Reading

**Official Oracle JDBC Tutorial:**
https://docs.oracle.com/javase/tutorial/jdbc/

**MySQL Connector/J Documentation:**
https://dev.mysql.com/doc/connector-j/en/

**PreparedStatement JavaDoc:**
https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/PreparedStatement.html

**SQL Injection Prevention:**
https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html

---

## Key Vocabulary

- **JDBC**: Java Database Connectivity - Java API for database access
- **Driver**: Software that translates JDBC calls to database-specific calls
- **Connection**: Active link between Java and database
- **PreparedStatement**: Precompiled SQL with placeholders
- **ResultSet**: Cursor over query results
- **SQLException**: Exception thrown for database errors
- **CRUD**: Create, Read, Update, Delete operations
- **SQL Injection**: Security vulnerability when SQL is concatenated
- **Try-with-resources**: Java feature for automatic resource cleanup
- **Transaction**: Group of operations that succeed or fail together

---

## Phase 1 Project Structure

```
project/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── myproject/
│   │               ├── App.java                  ← Test program
│   │               └── DatabaseManager.java      ← Database operations
│   └── test/
│       └── java/
│           └── com/
│               └── myproject/
│                   └── AppTest.java
├── docs/
│   ├── PHASE1_GUIDE.md          ← Start here!
│   ├── PHASE1_CONCEPTS.md       ← Deep concepts
│   ├── PHASE1_TESTING.md        ← Testing checklist
│   └── PHASE1_REFERENCE.md      ← This file
├── pom.xml                      ← Maven configuration
└── README.md
```

---

## Command Reference

```bash
# Compile project
mvn compile

# Run tests
mvn test

# Run main program
mvn exec:java -Dexec.mainClass="com.myproject.App"

# Clean and rebuild
mvn clean compile

# Download dependencies
mvn clean install

# Package as JAR
mvn package
```

---

## MySQL Commands (in phpMyAdmin SQL tab)

```sql
-- View all files
SELECT * FROM files;

-- Count files
SELECT COUNT(*) FROM files;

-- Total size
SELECT SUM(size) FROM files;

-- Files by extension
SELECT ext, COUNT(*) FROM files GROUP BY ext;

-- Clear all files
DELETE FROM files;

-- Drop and recreate table
DROP TABLE IF EXISTS files;
CREATE TABLE files (
  id INT PRIMARY KEY AUTO_INCREMENT,
  path TEXT NOT NULL,
  size BIGINT NOT NULL,
  last_modified BIGINT NOT NULL,
  ext VARCHAR(25)
);
```

---

Happy Coding! 🚀

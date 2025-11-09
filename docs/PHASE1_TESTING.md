# ✅ Phase 1: Testing Checklist

## Before Running the Code

### 1. Make Sure XAMPP is Running
- [ ] Open XAMPP Control Panel
- [ ] Start **Apache** module
- [ ] Start **MySQL** module
- [ ] Both should show "Running" status

### 2. Verify Database Setup
- [ ] Open browser and go to: `http://localhost/phpmyadmin/`
- [ ] Check that `file_indexer` database exists in the left sidebar
- [ ] Click on `file_indexer` database
- [ ] Click on the `files` table
- [ ] Verify the table structure has these columns:
  - `id` (INT, PRIMARY KEY, AUTO_INCREMENT)
  - `path` (TEXT)
  - `size` (BIGINT)
  - `last_modified` (BIGINT)
  - `ext` (VARCHAR 25)

---

## Running the Test Program

### Option 1: Run from VS Code (Recommended)

1. **Open** `src/main/java/com/myproject/App.java`
2. **Right-click** anywhere in the file
3. Select **"Run Java"** or **"Debug Java"**
4. Watch the output in the **Terminal** panel at the bottom

### Option 2: Run from Terminal

```bash
# Make sure you're in the project directory
cd "/Users/muhammad/fifth semester/ACP/java/project"

# Compile (if not already done)
mvn compile

# Run the main class
mvn exec:java -Dexec.mainClass="com.myproject.App"
```

---

## Expected Output

You should see something like this:

```
========================================
📁 File Indexer - Phase 1 Testing
========================================

🔌 Test 1: Database Connection
----------------------------------------
✅ Database connection successful!
   Database: file_indexer

🗑️  Test 2: Clear Old Data
----------------------------------------
🗑️  Cleared database: 0 rows deleted

➕ Test 3: Adding Sample Files
----------------------------------------
✅ Successfully added file: /Users/muhammad/Documents/project_report.pdf
✅ Successfully added file: /Users/muhammad/Code/App.java
✅ Successfully added file: /Users/muhammad/Documents/notes.txt
✅ Successfully added file: /Users/muhammad/Pictures/vacation.jpg
✅ Successfully added file: /Users/muhammad/Code/DatabaseManager.java
✅ Successfully added file: /Users/muhammad/Documents/budget_report.xlsx

🔍 Test 4: Search by Name (keyword: 'report')
----------------------------------------
🔍 Found 2 files matching 'report'
/Users/muhammad/Documents/project_report.pdf [pdf, 1048576 bytes, modified: ...]
/Users/muhammad/Documents/budget_report.xlsx [xlsx, 15000 bytes, modified: ...]

🔍 Test 5: Search by Extension (.java)
----------------------------------------
🔍 Found 2 .java files
/Users/muhammad/Code/App.java [5432 bytes, modified: ...]
/Users/muhammad/Code/DatabaseManager.java [8192 bytes, modified: ...]

🔍 Test 6: Search by Size (5KB - 20KB)
----------------------------------------
🔍 Found 3 files between 5000 and 20000 bytes
/Users/muhammad/Code/App.java [java, 5432 bytes]
/Users/muhammad/Code/DatabaseManager.java [java, 8192 bytes]
/Users/muhammad/Documents/budget_report.xlsx [xlsx, 15000 bytes]

📊 Test 7: Database Statistics
----------------------------------------
📊 Statistics retrieved successfully
Total Files: 6
Total Size: 3.13 MB

Files by Extension:
  .java: 2 files
  .pdf: 1 files
  .txt: 1 files
  .jpg: 1 files
  .xlsx: 1 files

========================================
✅ Phase 1 Testing Complete!
========================================

💡 Now check phpMyAdmin to see the data:
   http://localhost/phpmyadmin/
   Navigate to: file_indexer > files table
```

---

## Verify in phpMyAdmin

1. Go to `http://localhost/phpmyadmin/`
2. Click on `file_indexer` database (left sidebar)
3. Click on `files` table
4. You should see **6 rows** of data
5. Each row should have:
   - An auto-generated `id`
   - File `path`
   - File `size` (in bytes)
   - `last_modified` timestamp
   - File `ext` (extension)

---

## Common Issues & Solutions

### ❌ "Cannot connect to database"

**Possible Causes:**
1. MySQL is not running in XAMPP
2. Database `file_indexer` doesn't exist
3. Wrong password (should be empty for XAMPP default)

**Solution:**
- Start MySQL in XAMPP
- Create the database in phpMyAdmin
- Check DatabaseManager.java for correct credentials

### ❌ "Table 'file_indexer.files' doesn't exist"

**Solution:**
Run this SQL in phpMyAdmin:
```sql
CREATE TABLE files (
  id INT PRIMARY KEY AUTO_INCREMENT,
  path TEXT NOT NULL,
  size BIGINT NOT NULL,
  last_modified BIGINT NOT NULL,
  ext VARCHAR(25)
);
```

### ❌ "MySQL JDBC Driver not found"

**Solution:**
```bash
mvn clean install
```
This downloads the MySQL connector dependency.

### ❌ "Could not find or load main class"

**Solution:**
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.myproject.App"
```

---

## Understanding the Code

### Read These Files in Order:

1. **`docs/PHASE1_GUIDE.md`** 
   - Start here! High-level overview of all concepts
   - Explains JDBC, connections, PreparedStatement, etc.

2. **`docs/PHASE1_CONCEPTS.md`**
   - Deep dive into advanced concepts
   - SQL injection, transactions, batch operations, etc.

3. **`src/main/java/com/myproject/DatabaseManager.java`**
   - Implementation with extensive comments
   - Read each method carefully
   - Understand the try-with-resources pattern

4. **`src/main/java/com/myproject/App.java`**
   - Test program that demonstrates all operations
   - Shows how to use DatabaseManager

---

## What You've Learned

After completing Phase 1, you now understand:

✅ **JDBC Basics**
   - How to connect to MySQL from Java
   - JDBC URL format and connection parameters
   - Loading JDBC drivers

✅ **Connection Management**
   - Creating and closing connections
   - Try-with-resources for automatic cleanup
   - Why connections are expensive

✅ **PreparedStatement**
   - Why it's safer than Statement
   - How to use placeholders (?)
   - Preventing SQL injection attacks

✅ **CRUD Operations**
   - **C**reate: INSERT INTO
   - **R**ead: SELECT with various conditions
   - **D**elete: DELETE FROM
   - (Update will come later)

✅ **ResultSet**
   - Iterating through query results
   - Getting values by column name
   - Understanding the cursor model

✅ **SQL Queries**
   - LIKE operator for pattern matching
   - BETWEEN for range queries
   - COUNT and SUM for aggregates
   - GROUP BY for categorization

✅ **Exception Handling**
   - Catching SQLException
   - Proper error messages
   - Graceful failure

---

## Experiments to Try

Want to learn more? Try these exercises:

### Exercise 1: Add More Files
Modify `App.java` to add 10 different files of your choice.

### Exercise 2: Update Operation
Add an `updateFileSize()` method to DatabaseManager:
```java
public boolean updateFileSize(int id, long newSize) {
    // Your implementation here
}
```

### Exercise 3: Complex Queries
Add a method to find files modified in the last N days:
```java
public List<String> searchRecentFiles(int days) {
    // Hint: Compare last_modified with System.currentTimeMillis() - (days * 86400000)
}
```

### Exercise 4: Count by Extension
Add a method to count how many files have a specific extension:
```java
public int countByExtension(String ext) {
    // Use SELECT COUNT(*) WHERE ext = ?
}
```

### Exercise 5: Delete by Extension
Add a method to delete all files with a certain extension:
```java
public int deleteByExtension(String ext) {
    // Returns number of files deleted
}
```

---

## Next Steps: Phase 2

Once you're comfortable with Phase 1, you're ready for **Phase 2: File Scanner**

Phase 2 will cover:
- Recursive directory traversal
- Java File I/O (`java.nio.file`)
- Multithreading with ExecutorService
- Thread-safe database operations
- Batch inserts for performance

**Estimated Time:** Phase 2 will take 3-4 days to implement and understand.

---

## 🎓 Congratulations!

You've completed Phase 1! You now have a solid foundation in JDBC and database operations.

**Phase 1 Completion Criteria:**
- ✅ Can connect to MySQL from Java
- ✅ Can insert file records
- ✅ Can search files by name, extension, and size
- ✅ Can get database statistics
- ✅ Understand PreparedStatement and SQL injection prevention
- ✅ Understand try-with-resources and exception handling

**Ready for Phase 2?** Let me know! 🚀

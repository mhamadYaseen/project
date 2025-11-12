# 🚀 Phase 2: File Scanner with Multithreading - Overview

## 📋 Assignment Requirements Recap

From your assignment, Phase 2 must include:

✅ **Concurrent file scanning and indexing**

- Scan directories recursively
- Use multiple threads for performance
- Store file metadata in database

✅ **File Metadata to Store**

- Path (full file path)
- Size (in bytes)
- Last modified (timestamp)
- Extension (file type)

✅ **Command-Line Interface Only**

- No GUI/UI required
- All interaction through terminal
- Command-line arguments for input

---

## 🎯 Phase 2 Goals

By the end of Phase 2, you'll have:

1. **FileScanner class** - Scans directories recursively
2. **FileScannerTask class** - Concurrent scanning task
3. **FileMetadata class** - Data object for file information
4. **Enhanced DatabaseManager** - Batch insert capability
5. **Command-line interface** - Scan directories via terminal

---

## 🏗️ Architecture Design

```
User runs command:
java -jar FileIndexer.jar scan /path/to/directory

              ↓

┌─────────────────────────────────┐
│       FileScanner               │
│  - Coordinates scanning         │
│  - Creates thread pool          │
│  - Manages tasks                │
└─────────────────────────────────┘
              ↓
    Creates multiple threads
              ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│FileScanTask 1│  │FileScanTask 2│  │FileScanTask 3│
│ Scans Dir A  │  │ Scans Dir B  │  │ Scans Dir C  │
└──────────────┘  └──────────────┘  └──────────────┘
              ↓
    Collect FileMetadata objects
              ↓
┌─────────────────────────────────┐
│    DatabaseManager              │
│  - Batch insert files           │
│  - Thread-safe operations       │
└─────────────────────────────────┘
              ↓
         MySQL Database
```

---

## 📁 New Classes to Implement

### 1. **FileMetadata.java**

**Purpose:** Data class to hold file information

```java
class FileMetadata {
    - String path
    - long size
    - long lastModified
    - String extension
}
```

**Why?** Clean way to pass file data between scanner and database

---

### 2. **FileScannerTask.java**

**Purpose:** Runnable task that scans one directory

```java
class FileScannerTask implements Runnable {
    - Scans assigned directory
    - Recursively processes subdirectories
    - Extracts file metadata
    - Adds to shared queue
}
```

**Why?** Each thread runs one of these tasks

---

### 3. **FileScanner.java**

**Purpose:** Main scanning coordinator

```java
class FileScanner {
    - Creates ExecutorService (thread pool)
    - Walks directory tree
    - Submits tasks to threads
    - Collects results
    - Batch inserts to database
}
```

**Why?** Orchestrates the entire scanning process

---

### 4. **Enhanced DatabaseManager**

**Purpose:** Add batch insert capability

```java
New methods:
- addFileBatch(List<FileMetadata> files)
- synchronized methods for thread safety
```

**Why?** Inserting 1000 files one-by-one is slow; batch insert is 10-50x faster

---

## 🧵 Concurrency Strategy

### Thread Pool Design

```
ExecutorService pool = Executors.newFixedThreadPool(4);

Why 4 threads?
- Good balance for most systems
- CPU cores * 2 is typical
- Can be configurable
```

### Producer-Consumer Pattern

```
Scanner Threads (Producers)
    ↓
BlockingQueue<FileMetadata>
    ↓
Database Writer (Consumer)
```

**Benefits:**

- Scanner threads don't wait for database
- Batch inserts improve performance
- Decouples scanning from storage

---

## 🔒 Thread Safety Considerations

### Issues to Handle:

1. **Multiple threads accessing database**

   - Solution: Synchronized methods or connection pool

2. **Shared data structures**

   - Solution: Thread-safe collections (ConcurrentLinkedQueue)

3. **Race conditions**

   - Solution: Proper synchronization

4. **Deadlocks**
   - Solution: Careful lock ordering

---

## 📊 Performance Optimizations

### 1. **Batch Inserts**

Instead of:

```java
for (file : files) {
    insertOne(file);  // 1000 database calls!
}
```

Do this:

```java
insertBatch(files);  // 1 database call!
```

### 2. **Parallel Scanning**

Single thread: 10,000 files = 10 seconds
4 threads: 10,000 files = 3 seconds

### 3. **Buffered Collection**

Collect 100 files, then batch insert
Repeat until done

---

## 🎮 Command-Line Interface Design

### Scan Command:

```bash
# Scan a directory
java -cp target/FileIndexer-1.0-SNAPSHOT.jar com.myproject.App scan /path/to/scan

# Scan with custom thread count
java -cp target/FileIndexer-1.0-SNAPSHOT.jar com.myproject.App scan /path/to/scan --threads 8
```

### Query Commands (Phase 3, but plan now):

```bash
# Will be for Phase 3 TCP server
java -cp target/FileIndexer-1.0-SNAPSHOT.jar com.myproject.App server
```

### App.java Main Method:

```java
public static void main(String[] args) {
    if (args.length == 0) {
        printUsage();
        return;
    }

    String command = args[0];
    switch (command) {
        case "scan":
            // Phase 2: File scanning
            handleScan(args);
            break;
        case "server":
            // Phase 3: TCP server
            handleServer(args);
            break;
        default:
            printUsage();
    }
}
```

---

## 🔍 Java NIO.2 (File I/O)

### Key Classes We'll Use:

1. **Path** - Represents file/directory path

   ```java
   Path path = Paths.get("/Users/muhammad/Documents");
   ```

2. **Files** - File operations

   ```java
   long size = Files.size(path);
   FileTime modified = Files.getLastModifiedTime(path);
   boolean isDirectory = Files.isDirectory(path);
   ```

3. **Files.walk()** - Recursive directory traversal

   ```java
   Files.walk(startPath)
        .filter(Files::isRegularFile)
        .forEach(file -> processFile(file));
   ```

4. **DirectoryStream** - Iterate directory contents
   ```java
   try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
       for (Path entry : stream) {
           // Process entry
       }
   }
   ```

---

## 📝 File Extension Extraction

```java
String getExtension(Path path) {
    String fileName = path.getFileName().toString();
    int dotIndex = fileName.lastIndexOf('.');

    if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
        return fileName.substring(dotIndex + 1);
    }

    return "";  // No extension
}
```

**Examples:**

- `report.pdf` → `"pdf"`
- `App.java` → `"java"`
- `README` → `""`
- `.gitignore` → `""`

---

## 🚨 Error Handling

### Common Issues:

1. **Permission Denied**

   - Some directories/files not accessible
   - Solution: Catch SecurityException, log, continue

2. **Symbolic Links**

   - Can cause infinite loops
   - Solution: Check Files.isSymbolicLink()

3. **File Deleted During Scan**

   - File exists when found, deleted before reading
   - Solution: Catch NoSuchFileException

4. **Large Files**
   - Files larger than Long.MAX_VALUE
   - Solution: Handle gracefully (unlikely in practice)

---

## 📈 Progress Tracking

```
Scanning: /Users/muhammad/Documents
Found: 150 files
Scanned: 1.2 MB
Processing...
[████████████████████████░░░░] 80% (120/150)
```

**Implementation:**

- AtomicInteger for thread-safe counting
- Periodic progress updates
- Final summary

---

## 🎯 Phase 2 Timeline (4 Days)

### Day 1: File I/O & Metadata

- [ ] Create FileMetadata class
- [ ] Learn Java NIO.2 basics
- [ ] Implement single-threaded scanner
- [ ] Test with small directory

### Day 2: Multithreading

- [ ] Create FileScannerTask
- [ ] Implement ExecutorService
- [ ] Create FileScanner coordinator
- [ ] Test with multiple threads

### Day 3: Database Integration

- [ ] Add batch insert to DatabaseManager
- [ ] Implement thread-safe operations
- [ ] Integrate scanner with database
- [ ] Test end-to-end

### Day 4: Command-Line & Polish

- [ ] Implement CLI argument parsing
- [ ] Add progress tracking
- [ ] Error handling
- [ ] Performance testing
- [ ] Documentation

---

## 🧪 Testing Strategy

### Test 1: Small Directory (10 files)

- Verify all files found
- Check metadata accuracy
- Confirm database insertion

### Test 2: Medium Directory (1000 files)

- Performance measurement
- Thread coordination
- No missing files

### Test 3: Large Directory (10,000+ files)

- Stress test
- Memory usage
- Batch insert efficiency

### Test 4: Error Conditions

- Permission denied directories
- Symbolic links
- Empty directories
- Non-existent paths

---

## 📚 Key Concepts You'll Learn

### 1. **Java NIO.2**

- Modern file I/O API
- Path and Files classes
- Directory walking
- File attributes

### 2. **ExecutorService**

- Thread pool management
- Task submission
- Shutdown procedures
- Future and Callable

### 3. **Concurrent Collections**

- ConcurrentLinkedQueue
- BlockingQueue
- Thread-safe operations

### 4. **Synchronization**

- synchronized keyword
- Thread safety
- Race condition prevention

### 5. **Batch Operations**

- Performance optimization
- Transaction management
- Bulk inserts

---

## 🔧 Tools & Dependencies

### Already Have:

- ✅ Java 21
- ✅ Maven
- ✅ MySQL JDBC Driver
- ✅ DatabaseManager

### Will Use (built-in Java):

- ✅ java.nio.file package
- ✅ java.util.concurrent package
- ✅ java.util.concurrent.atomic package

**No new dependencies needed!** Everything is in Java standard library.

---

## 💡 Best Practices for Phase 2

### DO:

✅ Use try-with-resources for file streams
✅ Handle exceptions gracefully
✅ Use thread-safe collections
✅ Batch database operations
✅ Shutdown ExecutorService properly
✅ Log progress and errors
✅ Test with real directories

### DON'T:

❌ Create unlimited threads
❌ Ignore exceptions
❌ Use non-thread-safe collections
❌ Insert files one-by-one
❌ Follow symbolic links blindly
❌ Forget to close resources
❌ Test only with small directories

---

## 🎓 Learning Resources

**Java NIO.2 Tutorial:**
https://docs.oracle.com/javase/tutorial/essential/io/fileio.html

**Java Concurrency Tutorial:**
https://docs.oracle.com/javase/tutorial/essential/concurrency/

**ExecutorService Guide:**
https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html

---

## ✅ Phase 2 Completion Criteria

You'll know Phase 2 is complete when:

- [ ] Can scan any directory recursively
- [ ] Uses multiple threads efficiently
- [ ] Stores all files in database
- [ ] Handles errors gracefully
- [ ] Works via command line
- [ ] Shows progress during scanning
- [ ] Batch inserts for performance
- [ ] No race conditions or deadlocks
- [ ] Clean, documented code
- [ ] Tested with 1000+ files

---

## 🚀 Ready to Start?

**Next Steps:**

1. Read this overview completely
2. Study the implementation plan
3. Start with FileMetadata (simplest)
4. Move to single-threaded scanner
5. Add multithreading
6. Integrate with database

**Let's begin!** 🎉

---

**Phase 2 Status:** 🟡 Ready to Start
**Estimated Time:** 4 days
**Difficulty:** ⭐⭐⭐ Moderate (introduces concurrency)

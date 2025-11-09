# 🚀 Complete Setup Guide - File Indexer Project

## ✅ Step-by-Step Setup Checklist

### **STEP 1: Install VS Code Java Extensions**

#### Option A: Install from VS Code (RECOMMENDED)

1. Open **VS Code**
2. Press `Cmd+Shift+X` to open Extensions
3. Search for "**Extension Pack for Java**"
4. Click **Install** on the one by **Microsoft**
5. Wait for all extensions to install (it includes 6+ extensions)

#### Option B: Install the .vsix file you downloaded

1. Open **VS Code**
2. Press `Cmd+Shift+X` to open Extensions
3. Click the `...` (three dots) at the top right
4. Select "**Install from VSIX...**"
5. Navigate to your downloaded `Microsoft.VisualStudio.Services.VSIXPackage` file
6. Select it and click Install

**What gets installed:**

- Language Support for Java by Red Hat
- Debugger for Java
- Test Runner for Java
- Maven for Java
- Project Manager for Java
- IntelliCode

---

### **STEP 2: Verify Java Installation**

Open Terminal in VS Code (Ctrl+` or Cmd+`) and run:

```bash
java --version
```

✅ **Expected Output:**

```
java 21.0.9 2025-10-21 LTS
Java(TM) SE Runtime Environment (build 21.0.9+7-LTS-338)
Java HotSpot(TM) 64-Bit Server VM (build 21.0.9+7-LTS-338, mixed mode, sharing)
```

✅ **You have Java 21** - Project is now configured for this version!

---

### **STEP 3: Verify Maven Installation**

In the same terminal, run:

```bash
mvn --version
```

✅ **Expected Output:** Something like:

```
Apache Maven 3.x.x
Maven home: /usr/local/...
Java version: 21.0.9
```

❌ **If Maven is NOT installed:**

```bash
# Install Maven using Homebrew (easiest on Mac)
brew install maven
```

---

### **STEP 4: Set Up XAMPP and MySQL Database**

1. **Start XAMPP:**

   - Open XAMPP Control Panel
   - Click **Start** on both **Apache** and **MySQL**
   - Wait until both show "Running" status

2. **Open phpMyAdmin:**

   - Open your browser
   - Go to: `http://localhost/phpmyadmin/`

3. **Create Database:**

   - Click "**New**" in the left sidebar (or "Databases" tab)
   - Enter database name: `file_indexer`
   - Click "**Create**"

4. **Create Table:**
   - Click on `file_indexer` database in the left sidebar
   - Click the "**SQL**" tab
   - Copy and paste this SQL:

```sql
CREATE TABLE files (
  id INT PRIMARY KEY AUTO_INCREMENT,
  path TEXT NOT NULL,
  size BIGINT NOT NULL,
  last_modified BIGINT NOT NULL,
  ext VARCHAR(25)
);

-- Add indexes for faster queries
CREATE INDEX idx_ext ON files(ext);
CREATE INDEX idx_size ON files(size);
```

- Click "**Go**"
- You should see "Query OK" message

---

### **STEP 5: Update the Project Package Structure**

Currently, your project uses `com.example` package. We need to change it to `com.myproject`.

**Manual Steps:**

1. In VS Code Explorer, navigate to:

   - `src/main/java/com/example/`

2. Right-click on the `example` folder → **Rename** → change to `myproject`

3. Open `App.java` and change the first line from:

   ```java
   package com.example;
   ```

   to:

   ```java
   package com.myproject;
   ```

4. Do the same for `AppTest.java` in `src/test/java/com/example/`

---

### **STEP 6: Download Maven Dependencies**

In VS Code terminal, run:

```bash
mvn clean install
```

This will:

- Download the MySQL JDBC driver
- Download JUnit libraries
- Compile your project
- Run tests

✅ **Expected Output:**

```
BUILD SUCCESS
```

You might see some warnings - that's okay for now.

---

### **STEP 7: Test Your Setup**

Let's verify everything works:

1. **Check if MySQL driver was downloaded:**

   ```bash
   ls ~/.m2/repository/com/mysql/mysql-connector-j/
   ```

   You should see version folders.

2. **Try compiling the project:**
   ```bash
   mvn compile
   ```
   Should say "BUILD SUCCESS"

---

## 🎯 What's Been Done So Far

✅ Maven project created
✅ Updated to Java 21
✅ Added MySQL JDBC driver dependency
✅ Project renamed to `FileIndexer`
✅ GroupId changed to `com.myproject`
✅ README updated

---

## 📝 Next Steps (After Setup)

Once all the above steps are complete, you're ready to start coding!

**Phase 1: Database Connection (Start Here)**

- Create `DatabaseManager.java`
- Test connection to MySQL
- Implement basic CRUD operations

**Phase 2: File Scanner**

- Create `FileScanner.java`
- Implement recursive directory scanning
- Add multithreading

**Phase 3: TCP Server**

- Create `QueryServer.java`
- Implement query protocol
- Handle multiple clients

---

## 🆘 Common Issues & Solutions

### Issue: "mvn command not found"

**Solution:** Install Maven with `brew install maven`

### Issue: "Cannot connect to MySQL"

**Solution:**

- Make sure XAMPP MySQL is running
- Check if port 3306 is open
- Verify database name is `file_indexer`

### Issue: VS Code doesn't recognize Java

**Solution:**

- Install "Extension Pack for Java"
- Reload VS Code window (Cmd+Shift+P → "Reload Window")
- Check Java path in VS Code settings

### Issue: "Package does not exist" errors

**Solution:**

- Run `mvn clean install` to download dependencies
- Reload VS Code window

---

## 📞 Ready to Code?

Once you complete all 7 steps above, run this command to make sure everything works:

```bash
mvn clean compile
```

If you see **BUILD SUCCESS**, you're all set! 🎉

Let me know when you're ready to start Phase 1 (Database Connection)!

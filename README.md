# Maven Project

A simple Maven project template.

## Project Structure

```
project/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── example/
│   │               └── App.java
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── AppTest.java
└── README.md
```

## Build and Run

### Compile the project
```bash
mvn compile
```

### Run tests
```bash
mvn test
```

### Package the application
```bash
mvn package
```

### Run the application
```bash
mvn exec:java -Dexec.mainClass="com.example.App"
```

Or run the packaged JAR:
```bash
java -cp target/project-1.0-SNAPSHOT.jar com.example.App
```

### Clean the project
```bash
mvn clean
```

## Requirements

- Java 21
- Maven 3.6 or higher
- MySQL (via XAMPP)
- VS Code with Extension Pack for Java

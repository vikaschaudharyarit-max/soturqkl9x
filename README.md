# SmartDBF

A modern Java library for **reading and processing DBF (dBASE) files**. SmartDBF provides a simple, type-safe API with support for streaming, schema introspection, and mapping records to Java objects via annotations.

---

## Features

- **Simple API** — Open a DBF file with a single call: `Dbf.open(path)`
- **Schema introspection** — Access field names, types, and metadata without reading all data
- **Streaming support** — Process large files with `stream()` without loading everything into memory
- **POJO mapping** — Map records to Java classes using `@DbfColumn` for flexible column binding
- **Type conversion** — Automatic handling of Character (C), Numeric (N), Logical (L), and Date (D) fields
- **Java 17+** — Built for modern Java with `Stream` and clean APIs
- **Minimal dependencies** — Only SLF4J for logging; no heavy runtime dependencies

---

## Requirements

- **Java 17** or higher

---

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.smartdbf</groupId>
    <artifactId>smart-dbf</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle (Groovy)

```groovy
implementation 'io.smartdbf:smart-dbf:1.0.0'
```

### Gradle (Kotlin DSL)

```kotlin
implementation("io.smartdbf:smart-dbf:1.0.0")
```

---

## Quick Start

```java
import io.smartdbf.core.Dbf;
import io.smartdbf.core.DbfReader;

DbfReader reader = Dbf.open("/path/to/file.dbf");

// Inspect schema
System.out.println(reader.getSchema());

// Read records one by one
while (reader.hasNext()) {
    Object[] record = reader.nextRecord();
    // record[i] corresponds to schema.getFields().get(i)
}

reader.close();
```

---

## Usage

### 1. Opening a DBF file

```java
import io.smartdbf.core.Dbf;
import io.smartdbf.core.DbfReader;

DbfReader reader = Dbf.open("data.dbf");
try {
    // use reader
} finally {
    reader.close();
}
```

`Dbf.open(String path)` opens the file and parses the header and schema. If the file cannot be opened or parsed, a `DbfException` is thrown.

---

### 2. Reading records as `Object[]`

Each record is returned as an array of objects aligned with the schema field order. Types are converted automatically:

| DBF Type | Java type / value |
|----------|-------------------|
| C (Character) | `String` |
| N (Numeric)   | `Double` or `null` |
| L (Logical)  | `Boolean` (Y/T → true) |
| D (Date)     | `String` (raw) |

```java
DbfReader reader = Dbf.open("data.dbf");
DbfSchema schema = reader.getSchema();

while (reader.hasNext()) {
    Object[] row = reader.nextRecord();
    for (int i = 0; i < row.length; i++) {
        String fieldName = schema.getFields().get(i).getName();
        Object value = row[i];
        System.out.println(fieldName + " = " + value);
    }
}
reader.close();
```

---

### 3. Streaming records

For large files, use `stream()` to process records without loading all into memory:

```java
DbfReader reader = Dbf.open("large.dbf");

reader.stream()
    .filter(row -> row[2] != null)
    .limit(1000)
    .forEach(row -> System.out.println(Arrays.toString(row)));

reader.close();
```

The stream is configured with `onClose(reader::close)`, so when the stream is consumed or closed, the underlying reader is closed. Always close the reader when you are done (or consume the stream fully).

---

### 4. Mapping to Java objects (POJOs)

Define a class with fields that match DBF column names (case-insensitive). Use `@DbfColumn("DBF_COLUMN_NAME")` when the Java field name differs from the column name.

**Example entity:**

```java
import io.smartdbf.annotation.DbfColumn;

public class Transaction {
    @DbfColumn("FOLIO_NO")
    private String folioNo;

    @DbfColumn("SCHEME")
    private String scheme;

    @DbfColumn("AMOUNT")
    private Double amount;

    @DbfColumn("TRADDATE")
    private String tradeDate;

    // Default constructor required for mapping
    public Transaction() {}

    // Getters and setters
    public String getFolioNo() { return folioNo; }
    public void setFolioNo(String folioNo) { this.folioNo = folioNo; }
    public String getScheme() { return scheme; }
    public void setScheme(String scheme) { this.scheme = scheme; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getTradeDate() { return tradeDate; }
    public void setTradeDate(String tradeDate) { this.tradeDate = tradeDate; }
}
```

**Read all into a list:**

```java
DbfReader reader = Dbf.open("transactions.dbf");
List<Transaction> list = reader.read(Transaction.class);
reader.close();
```

**Stream as mapped objects:**

```java
DbfReader reader = Dbf.open("transactions.dbf");
reader.stream(Transaction.class)
    .filter(t -> t.getAmount() != null && t.getAmount() > 1000)
    .forEach(t -> System.out.println(t.getFolioNo() + " " + t.getAmount()));
reader.close();
```

Mapping rules:

- **Column name:** If a field has `@DbfColumn("NAME")`, the mapper uses that name; otherwise it uses the Java field name.
- **Matching:** DBF column names are matched case-insensitively to the name (from annotation or field).
- **No-arg constructor:** The target class must have a public no-argument constructor.
- **Unmatched columns:** DBF columns with no matching field are ignored. Fields with no matching column are left at default values.

---

### 5. Schema and field metadata

```java
DbfReader reader = Dbf.open("data.dbf");
DbfSchema schema = reader.getSchema();

for (DbfField field : schema.getFields()) {
    String name = field.getName();
    char type = field.getType();   // C, N, L, D, etc.
    int length = field.getLength();
    int decimals = field.getDecimalCount();
    System.out.println(name + " " + type + " " + length);
}
```

---

## API overview

| Class / interface | Description |
|-------------------|-------------|
| `Dbf` | Entry point; `Dbf.open(String path)` returns a `DbfReader`. |
| `DbfReader` | Reads records, provides `getSchema()`, `hasNext()`, `nextRecord()`, `stream()`, `read(Class)`, `stream(Class)`, `close()`. |
| `DbfSchema` | List of `DbfField`; describes the structure of the DBF file. |
| `DbfField` | Field name, type (char), length, decimal count. |
| `DbfMapper<T>` | Used internally; maps `Object[]` rows to instances of `T` using `@DbfColumn`. |
| `@DbfColumn("NAME")` | Annotation on a field to bind it to the DBF column `NAME`. |
| `DbfException` | RuntimeException thrown on open/parse errors. |

---

## Building from source

```bash
git clone https://github.com/your-username/smart-dbf.git
cd smart-dbf
mvn clean install
```

To run tests:

```bash
mvn test
```

To skip tests (e.g. if tests use local file paths):

```bash
mvn clean install -DskipTests
```

---

## Publishing to Maven Central

This project is set up for publishing to Maven Central. See **[MAVEN_CENTRAL_PUBLISHING.md](MAVEN_CENTRAL_PUBLISHING.md)** for:

- Sonatype Central account and namespace
- GPG signing
- Token configuration and `mvn deploy`

---

## License

This project is licensed under the **Apache License, Version 2.0**. See the [LICENSE](LICENSE) file or [https://www.apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0) for details.

---

## Contributing

Contributions are welcome. Please open an issue or submit a pull request on the project repository.

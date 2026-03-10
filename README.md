# SmartDBF

A modern Java library for **reading and processing DBF (dBASE) files**. SmartDBF provides a simple, type-safe API with support for streaming, schema introspection, charset configuration, and mapping records directly to Java objects via annotations.

---

## How to use in your project

**1. Add the dependency** (Maven):

```xml
<dependency>
    <groupId>io.github.vikaschaudharyarit-max</groupId>
    <artifactId>smart-dbf</artifactId>
    <version>1.1.0</version>
</dependency>
```

**Gradle (Groovy):** `implementation 'io.github.vikaschaudharyarit-max:smart-dbf:1.1.0'`

**Gradle (Kotlin DSL):** `implementation("io.github.vikaschaudharyarit-max:smart-dbf:1.1.0")`

**2. In your Java code:**

```java
import io.github.vikaschaudharyarit_max.smartdbf.core.Dbf;
import io.github.vikaschaudharyarit_max.smartdbf.core.DbfReader;

// try-with-resources ensures the file is always closed
try (DbfReader reader = Dbf.open("/path/to/your/file.dbf")) {
    while (reader.hasNext()) {
        Object[] record = reader.nextRecord();
        // process record...
    }
}
```

That's it. For **streaming**, **schema inspection**, **charset configuration**, or **mapping to POJOs** with `@DbfColumn`, see the sections below.

---

## Features

- **Simple API** — Open any DBF file with a single call: `Dbf.open(path)`
- **try-with-resources** — `DbfReader` implements `AutoCloseable`; no manual `close()` needed
- **Schema introspection** — Inspect field names, types, and lengths without reading records: `Dbf.schema(path)`
- **Streaming** — Process large files lazily with `reader.stream()` without loading all records into memory
- **POJO mapping** — Map records to Java classes using `@DbfColumn` for flexible, annotation-driven binding
- **Automatic type conversion** — DBF fields converted to proper Java types:
  - `C` (Character) → `String`
  - `N`/`F` (Numeric/Float) → `BigDecimal` (preserves decimal precision)
  - `D` (Date) → `java.time.LocalDate`
  - `L` (Logical) → `Boolean`
- **Smart type coercion in POJO mapper** — Automatically converts `BigDecimal` to `Double`, `Long`, `Integer`, etc. to match your field types
- **Charset support** — Specify encoding for legacy DBF files: `Dbf.open(path, Charset.forName("CP1252"))`
- **InputStream support** — Read from AWS S3, HTTP, or any stream with `Dbf.open(InputStream)`
- **Configurable buffer size** — Tune I/O performance via `DbfConfig`
- **SLF4J logging** — Debug-level logging throughout; bring your own implementation (Logback, Log4j2, etc.)
- **Minimal dependencies** — Only `slf4j-api`; no Spring, no heavy transitive dependencies
- **Java 17+** — Uses modern Java idioms (`Stream`, `LocalDate`, records, pattern matching)

---

## Requirements

- **Java 17** or higher
- No other required runtime dependencies

---

## Usage

### 1. Opening a DBF file (local path)

```java
try (DbfReader reader = Dbf.open("data.dbf")) {
    while (reader.hasNext()) {
        Object[] row = reader.nextRecord();
        // row[i] corresponds to schema.getFields().get(i)
    }
}
```

If the file cannot be opened or the header cannot be parsed, a `DbfException` (unchecked) is thrown.

---

### 2. Inspecting the schema

Inspect file structure without reading any records:

```java
// From a path — no records are read
DbfSchema schema = Dbf.schema("data.dbf");

System.out.printf("%-15s %-6s %-8s %-10s%n", "Field", "Type", "Length", "Decimals");
for (DbfField field : schema.getFields()) {
    System.out.printf("%-15s %-6s %-8d %-10d%n",
            field.getName(), field.getType(), field.getLength(), field.getDecimalCount());
}
```

Example output:

```
Field           Type   Length   Decimals
AMC_CODE        C      3        0
FOLIO_NO        C      20       0
AMOUNT          N      19       2
TRADDATE        D      8        0
ACTIVE          L      1        0
```

You can also get the schema from an open reader: `reader.getSchema()`.

---

### 3. Reading records as `Object[]`

Field values are returned in schema order. Types are converted automatically:


| DBF Type                | Java Type              |
| ----------------------- | ---------------------- |
| C (Character)           | `String`               |
| N / F (Numeric / Float) | `java.math.BigDecimal` |
| D (Date, yyyyMMdd)      | `java.time.LocalDate`  |
| L (Logical)             | `Boolean`              |
| Any other               | `String`               |
| Empty / blank           | `null`                 |


```java
try (DbfReader reader = Dbf.open("data.dbf")) {
    DbfSchema schema = reader.getSchema();
    while (reader.hasNext()) {
        Object[] row = reader.nextRecord();
        for (int i = 0; i < row.length; i++) {
            System.out.println(schema.getFields().get(i).getName() + " = " + row[i]);
        }
    }
}
```

Deleted records (marked with `*` internally) are skipped automatically.

---

### 4. Streaming records

For large files, `stream()` processes records lazily without loading everything into memory:

```java
try (DbfReader reader = Dbf.open("large.dbf")) {
    reader.stream()
          .filter(row -> row[2] != null)
          .limit(1000)
          .forEach(row -> System.out.println(Arrays.toString(row)));
}
```

The stream closes the reader automatically when the terminal operation completes.

---

### 5. Mapping records to POJOs

Define a POJO with `@DbfColumn` annotations to bind fields to DBF columns by name (case-insensitive):

```java
import io.github.vikaschaudharyarit_max.smartdbf.annotation.DbfColumn;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {

    @DbfColumn("FOLIO_NO")  private String    folioNo;
    @DbfColumn("SCHEME")    private String    scheme;
    @DbfColumn("AMOUNT")    private BigDecimal amount;
    @DbfColumn("TRADDATE")  private LocalDate tradeDate;
    @DbfColumn("UNITS")     private BigDecimal units;

    public Transaction() {} // required no-arg constructor

    // getters...
}
```

**Read all into a list:**

```java
try (DbfReader reader = Dbf.open("transactions.dbf")) {
    List<Transaction> txns = reader.read(Transaction.class);
}
```

**Stream as typed objects:**

```java
try (DbfReader reader = Dbf.open("transactions.dbf")) {
    reader.stream(Transaction.class)
          .filter(t -> t.getAmount() != null && t.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0)
          .forEach(t -> System.out.println(t.getFolioNo() + " " + t.getAmount()));
}
```

**Mapping rules:**

- If a Java field has `@DbfColumn("NAME")`, that column name is used for lookup; otherwise the Java field name is used.
- DBF column names are matched **case-insensitively**.
- The target class must have a **public no-arg constructor** (works with Lombok `@NoArgsConstructor`).
- Unmatched columns are ignored; unmatched Java fields keep their default values.
- **Automatic type coercion:** `BigDecimal` → `Double`/`Long`/`Integer`/`Float`; `LocalDate` → `java.sql.Date`/`java.util.Date`; etc.

---

### 6. Opening from an InputStream (e.g. AWS S3)

Use `Dbf.open(InputStream)` when the DBF content comes from a remote source. SmartDBF does not depend on AWS — it only needs a standard `InputStream`.

```java
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

String bucket = "my-bucket";
String key    = "data/transactions.dbf";

try (var s3Stream = s3Client.getObject(
        GetObjectRequest.builder().bucket(bucket).key(key).build());
     DbfReader reader = Dbf.open(s3Stream)) {

    reader.stream(Transaction.class)
          .forEach(System.out::println);
}
```

---

### 7. Charset / encoding support

Many legacy DBF files produced on Windows use `CP1252` (Western European) or other regional encodings. Specify the charset explicitly:

```java
import java.nio.charset.Charset;

// CP1252 (common for Windows-generated DBF files)
try (DbfReader reader = Dbf.open("legacy.dbf", Charset.forName("CP1252"))) {
    reader.stream().forEach(row -> System.out.println(Arrays.toString(row)));
}

// ISO-8859-1
try (DbfReader reader = Dbf.open(s3Stream, Charset.forName("ISO-8859-1"))) {
    List<MyRecord> records = reader.read(MyRecord.class);
}
```

Default encoding is **UTF-8**.

---

### 8. Custom buffer size (performance tuning)

For very large files on slow storage (NFS, S3-backed mounts), increase the read buffer:

```java
import io.github.vikaschaudharyarit_max.smartdbf.config.DbfConfig;
import java.nio.charset.StandardCharsets;

DbfConfig config = new DbfConfig.Builder()
        .bufferSize(524288) // 512 KB
        .build();

try (DbfReader reader = Dbf.open("huge.dbf", StandardCharsets.UTF_8, config)) {
    reader.stream().forEach(row -> process(row));
}
```

Default buffer size is **64 KB**.

---

## API Reference


| Class                | Description                                                                                                                                                                                       |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Dbf`                | Entry point. Static factory methods: `open(path)`, `open(path, charset)`, `open(path, charset, config)`, `open(InputStream)`, `open(InputStream, charset)`, `schema(path)`, `schema(InputStream)` |
| `DbfReader`          | Reads records. `hasNext()`, `nextRecord()`, `stream()`, `read(Class)`, `stream(Class)`, `getSchema()`, `close()`. Implements `AutoCloseable`.                                                     |
| `DbfSchema`          | The list of `DbfField`s describing the file structure.                                                                                                                                            |
| `DbfField`           | One column: `getName()`, `getType()` (char: C/N/D/L/…), `getLength()`, `getDecimalCount()`                                                                                                        |
| `DbfConfig`          | I/O tuning. Built via `new DbfConfig.Builder().bufferSize(n).build()`.                                                                                                                            |
| `@DbfColumn("NAME")` | Annotation on a POJO field. Binds it to the DBF column `NAME`.                                                                                                                                    |
| `DbfException`       | Unchecked exception thrown on open / parse / read failures.                                                                                                                                       |


---

## Building from source

```bash
git clone https://github.com/vikaschaudharyarit-max/smart-dbf.git
cd smart-dbf
mvn clean install
```

Run tests only:

```bash
mvn test
```

Tests are fully self-contained — no external files or machine-specific paths are needed.


## License

Apache License, Version 2.0 — see [https://www.apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

## Contributing

Contributions are welcome. Please open an issue or submit a pull request on the project repository.
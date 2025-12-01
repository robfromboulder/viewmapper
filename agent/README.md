# ViewMapper Agent and Trino SQL Parser

This module contains the core agent and SQL parser for the ViewMapper project, using Trino's native SQL parser to accurately extract table and view dependencies.

## Dependencies

- **Trino-Parser** - SQL parsing and AST
- **Jackson** - JSON processing
- **LangChain4j** - LLM agent framework
- **Picocli** - CLI framework
- **JUnit 5** - Testing framework
- **AssertJ** - Fluent assertions

## Building

```bash
cd agent
mvn clean package
```

This produces:
- `target/original-viewmapper-478.jar` - Regular JAR
- `target/viewmapper-478.jar` - Fat JAR with all dependencies (for CLI usage)

## Running Tests

```bash
mvn test
```

## Why Trino Parser Over Regex?

The Trino parser correctly handles complex SQL features that regex cannot:

| Feature | Regex Result | Trino Parser Result |
|---------|--------------|---------------------|
| CTEs (WITH clauses) | Incorrectly includes CTE names as dependencies | ✅ Excludes CTEs, only includes actual tables |
| String literals | Matches table-like patterns in strings | ✅ Ignores string literal content |
| Subqueries | Misses nested dependencies | ✅ Correctly traverses all subqueries |
| Quoted identifiers | Fails on special characters | ✅ Handles all valid identifiers |
| Comments | Matches table names in comments | ✅ Ignores comments |

Let's take a specific example:
```sql
WITH temp AS (
  SELECT * FROM schema1.table1
  WHERE description LIKE '%schema2.fake_table%'
)
SELECT * FROM temp
JOIN schema3.table2 ON temp.id = table2.id
```

- **Regex result:** `[schema1.table1, schema2.fake_table, temp, schema3.table2]` ❌
- **Trino parser result:** `[schema1.table1, schema3.table2]` ✅

## Implementation Details

### TrinoSqlParser

Main entry point for parsing SQL and extracting dependencies.

```java
TrinoSqlParser parser = new TrinoSqlParser();
Set<TableReference> deps = parser.extractDependencies(sql);
```

### DependencyExtractor

AST visitor that traverses the parsed SQL tree and collects table references while:
- Tracking CTE names to exclude them from dependencies
- Handling all query types (SELECT, INSERT, CREATE VIEW, MERGE, etc.)
- Traversing subqueries, joins, and set operations
- Ignoring string literals and comments

### TableReference

Model class representing a table/view reference with:
- Catalog name (optional)
- Schema name (optional)
- Table name (required)
- Fully qualified name

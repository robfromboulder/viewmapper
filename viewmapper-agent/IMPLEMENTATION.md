# viewmapper-agent: Implementation Notes

This document contains technical implementation details for developers working on the ViewMapper codebase.

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

## About Core Components

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

## Creating Predefined Test Datasets

To create your own test data file:

1. **Define your schema:**
   ```json
   {
     "description": "Optional description of your test scenario",
     "views": []
   }
   ```

2. **Add views with dependencies:**
   ```json
   {
     "name": "base_table",
     "sql": "SELECT * FROM raw.source"
   },
   {
     "name": "derived_view",
     "sql": "SELECT * FROM base_table WHERE active = true"
   }
   ```

3. **Follow naming conventions:**
    - Use lowercase or quoted identifiers (Trino normalizes unquoted to lowercase)
    - Use qualified names for clarity: `schema.table` or `catalog.schema.table`
    - Follow domain patterns: `dim_*`, `fact_*`, `stg_*`, `raw_*`

4. **Place the file in:** `src/main/resources/datasets/`

5. **Test your file:**
   ```bash
   java -jar target/viewmapper-478.jar run --connection "test://your_file" "Analyze this schema"
   ```

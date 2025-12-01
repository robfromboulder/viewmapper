// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import io.trino.sql.parser.SqlParser;
import io.trino.sql.tree.Statement;

import java.util.Set;

/**
 * Wraps the Trino SQL parser to extract table and view dependencies from SQL statements.
 * <p>
 * This parser uses Trino's native AST parser rather than regex to handle complex SQL features:
 * - CTEs (WITH clauses)
 * - Subqueries
 * - UNNEST operations
 * - Quoted identifiers
 * - String literals containing table-like patterns
 */
public class TrinoSqlParser {

    private final SqlParser parser;

    /**
     * Creates a new TrinoSqlParser with default parsing options.
     */
    public TrinoSqlParser() {
        this.parser = new SqlParser();
    }

    /**
     * Extracts all table and view dependencies from a SQL statement.
     * <p>
     * Example:
     * <pre>
     * WITH temp AS (
     *   SELECT * FROM schema1.table1
     *   WHERE description LIKE '%schema2.fake_table%'
     * )
     * SELECT * FROM temp
     * JOIN schema3.table2 ON temp.id = table2.id
     * </pre>
     * <p>
     * Returns: [schema1.table1, schema3.table2]
     * (Note: temp is a CTE, not a dependency; schema2.fake_table is in a string literal)
     *
     * @param sql The SQL statement to parse
     * @return Set of qualified table/view names that are actual dependencies
     * @throws io.trino.sql.parser.ParsingException if the SQL is invalid
     */
    public Set<TableReference> extractDependencies(String sql) {
        Statement statement = parser.createStatement(sql);
        DependencyExtractor visitor = new DependencyExtractor();
        visitor.process(statement);
        return visitor.getDependencies();
    }

}
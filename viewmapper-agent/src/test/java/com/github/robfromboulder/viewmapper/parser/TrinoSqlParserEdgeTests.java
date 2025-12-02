// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import io.trino.sql.parser.ParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for edge cases in SQL parsing.
 * <p>
 * These tests verify that the parser correctly handles:
 * - String literals containing table-like patterns
 * - Comments containing table names
 * - UNNEST operations
 * - VALUES clauses
 * - Case sensitivity
 * - Special characters and quoted identifiers
 * - Invalid SQL
 */
class TrinoSqlParserEdgeTests {

    private TrinoSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new TrinoSqlParser();
    }

    @Test
    void testStringLiteralShouldNotBeExtracted() {
        String sql = """
                SELECT *
                FROM users
                WHERE description LIKE '%schema.fake_table%'
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("users");
    }

    @Test
    void testMultipleStringLiteralsWithTableNames() {
        String sql = """
                SELECT *
                FROM actual_table
                WHERE
                    col1 = 'SELECT * FROM fake_table1'
                    AND col2 LIKE '%fake_table2%'
                    AND col3 IN ('fake_table3', 'fake_table4')
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("actual_table");
    }

    @Test
    void testCommentsShouldNotBeExtracted() {
        String sql = """
                -- This query uses fake_table
                /* Also references another_fake_table */
                SELECT *
                FROM real_table
                -- JOIN fake_table ON real_table.id = fake_table.id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("real_table");
    }

    @Test
    void testUnnestOperation() {
        String sql = """
                SELECT *
                FROM users
                CROSS JOIN UNNEST(tags) AS t(tag)
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("users");
    }

    @Test
    void testUnnestWithArray() {
        String sql = """
                SELECT *
                FROM UNNEST(ARRAY[1, 2, 3]) AS numbers(n)
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).isEmpty();
    }

    @Test
    void testValuesClause() {
        String sql = """
                SELECT * FROM (
                    VALUES
                        (1, 'Alice'),
                        (2, 'Bob'),
                        (3, 'Charlie')
                ) AS users(id, name)
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).isEmpty();
    }

    @Test
    void testCaseSensitivityOfCTEs() {
        String sql = """
                WITH TempData AS (
                    SELECT * FROM users
                )
                SELECT * FROM tempdata
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("users");
    }

    @Test
    void testQuotedIdentifiersWithSpecialCharacters() {
        String sql = """
                SELECT *
                FROM "schema-with-dashes"."table@with#special$chars"
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        TableReference ref = dependencies.iterator().next();
        assertThat(ref.getSchema()).isEqualTo("schema-with-dashes");
        assertThat(ref.getTable()).isEqualTo("table@with#special$chars");
    }

    @Test
    void testQuotedIdentifiersWithSpaces() {
        String sql = """
                SELECT *
                FROM "my schema"."my table"
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        TableReference ref = dependencies.iterator().next();
        assertThat(ref.getSchema()).isEqualTo("my schema");
        assertThat(ref.getTable()).isEqualTo("my table");
    }

    @Test
    void testMixedQuotedAndUnquotedIdentifiers() {
        String sql = """
                SELECT *
                FROM normal_schema."quoted-table"
                JOIN "quoted-schema".normal_table ON 1=1
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        // Trino normalizes quoted identifiers by removing quotes in output
        assertThat(getFullyQualifiedNames(dependencies))
                .containsExactlyInAnyOrder(
                        "normal_schema.quoted-table",
                        "quoted-schema.normal_table"
                );
    }

    @Test
    void testTableFunctionCall() {
        String sql = """
                SELECT *
                FROM TABLE(sequence(1, 100))
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).isEmpty();
    }

    @Test
    void testLateralJoin() {
        String sql = """
                SELECT *
                FROM users
                CROSS JOIN LATERAL (
                    SELECT * FROM orders WHERE orders.user_id = users.id
                ) user_orders
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders");
    }

    @Test
    void testIntersectAndExcept() {
        String sql = """
                SELECT id FROM users
                INTERSECT
                SELECT user_id FROM orders
                EXCEPT
                SELECT user_id FROM blocked_users
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(3);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders", "blocked_users");
    }

    @Test
    void testWindowFunctionWithoutTableDependency() {
        String sql = """
                SELECT
                    name,
                    salary,
                    AVG(salary) OVER (PARTITION BY department) as avg_dept_salary
                FROM employees
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("employees");
    }

    @Test
    void testComplexNestedSubqueriesWithStringLiterals() {
        String sql = """
                WITH base AS (
                    SELECT * FROM table1
                    WHERE description NOT LIKE '%table2%'
                )
                SELECT *
                FROM base
                WHERE id IN (
                    SELECT id FROM table3
                    WHERE name = 'FROM table4'
                )
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("table1", "table3");
    }

    @Test
    void testEmptyQuery() {
        assertThatThrownBy(() -> parser.extractDependencies("")).isInstanceOf(ParsingException.class);
    }

    @Test
    void testInvalidSQL() {
        String invalidSql = "SELECT * FORM users";
        assertThatThrownBy(() -> parser.extractDependencies(invalidSql)).isInstanceOf(ParsingException.class);
    }

    @Test
    void testInvalidSyntaxWithMultipleErrors() {
        String invalidSql = "SELCT * FRM usrs WHRE id = 1";
        assertThatThrownBy(() -> parser.extractDependencies(invalidSql)).isInstanceOf(ParsingException.class);
    }

    @Test
    void testSQLInjectionPattern() {
        // Ensure that SQL injection patterns in strings are not extracted
        String sql = """
                SELECT *
                FROM users
                WHERE username = 'admin'' OR ''1''=''1'
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("users");
    }

    @Test
    void testVeryLongTableName() {
        String longTableName = "a".repeat(200);
        String sql = String.format("SELECT * FROM %s", longTableName);

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly(longTableName);
    }

    @Test
    void testDuplicateTableReferences() {
        String sql = """
                SELECT *
                FROM users u1
                JOIN users u2 ON u1.manager_id = u2.id
                JOIN users u3 ON u2.manager_id = u3.id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("users");
    }

    @Test
    void testCTENameSameAsActualTable() {
        String sql = """
                WITH users AS (
                    SELECT * FROM customers WHERE active = true
                )
                SELECT * FROM users
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("customers");
    }

    @Test
    void testMultipleStatementsNotSupported() {
        String sql = """
                SELECT * FROM table1;
                SELECT * FROM table2;
                """;

        assertThatThrownBy(() -> parser.extractDependencies(sql)).isInstanceOf(ParsingException.class);
    }

    @Test
    void testCreateViewStatement() {
        String sql = """
                CREATE VIEW my_view AS
                SELECT u.*, o.total
                FROM users u
                JOIN orders o ON u.id = o.user_id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders");
    }

    @Test
    void testInsertIntoSelect() {
        String sql = """
                INSERT INTO target_table
                SELECT * FROM source_table WHERE active = true
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).contains("source_table");
    }

    @Test
    void testDeleteWithSubquery() {
        String sql = """
                DELETE FROM orders
                WHERE user_id IN (SELECT id FROM inactive_users)
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("orders", "inactive_users");
    }

    @Test
    void testMergeStatement() {
        String sql = """
                MERGE INTO target_table t
                USING source_table s
                ON t.id = s.id
                WHEN MATCHED THEN UPDATE SET value = s.value
                WHEN NOT MATCHED THEN INSERT (id, value) VALUES (s.id, s.value)
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("target_table", "source_table");
    }

    // Helper methods

    private Set<String> getTableNames(Set<TableReference> references) {
        return references.stream()
                .map(TableReference::getTable)
                .collect(Collectors.toSet());
    }

    private Set<String> getFullyQualifiedNames(Set<TableReference> references) {
        return references.stream()
                .map(TableReference::getFullyQualifiedName)
                .collect(Collectors.toSet());
    }
}

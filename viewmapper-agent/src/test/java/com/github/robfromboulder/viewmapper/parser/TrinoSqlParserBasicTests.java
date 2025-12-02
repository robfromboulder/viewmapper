// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for basic SQL parsing cases.
 */
class TrinoSqlParserBasicTests {

    private TrinoSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new TrinoSqlParser();
    }

    @Test
    void testSimpleSelect() {
        String sql = "SELECT * FROM users";

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("users");
    }

    @Test
    void testSimpleCaseSensitivity() {
        String sql = "SELECT * FROM TempData";

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("tempdata");
    }

    @Test
    void testSchemaQualifiedTable() {
        String sql = "SELECT * FROM myschema.users";

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        TableReference ref = dependencies.iterator().next();
        assertThat(ref.getSchema()).isEqualTo("myschema");
        assertThat(ref.getTable()).isEqualTo("users");
        assertThat(ref.getFullyQualifiedName()).isEqualTo("myschema.users");
    }

    @Test
    void testCatalogSchemaQualifiedTable() {
        String sql = "SELECT * FROM mycatalog.myschema.users";

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        TableReference ref = dependencies.iterator().next();
        assertThat(ref.getCatalog()).isEqualTo("mycatalog");
        assertThat(ref.getSchema()).isEqualTo("myschema");
        assertThat(ref.getTable()).isEqualTo("users");
        assertThat(ref.getFullyQualifiedName()).isEqualTo("mycatalog.myschema.users");
    }

    @Test
    void testSimpleJoin() {
        String sql = """
                SELECT u.name, o.total
                FROM users u
                JOIN orders o ON u.id = o.user_id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders");
    }

    @Test
    void testMultipleJoins() {
        String sql = """
                SELECT *
                FROM users u
                JOIN orders o ON u.id = o.user_id
                JOIN products p ON o.product_id = p.id
                JOIN categories c ON p.category_id = c.id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(4);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders", "products", "categories");
    }

    @Test
    void testLeftJoin() {
        String sql = """
                SELECT *
                FROM users u
                LEFT JOIN orders o ON u.id = o.user_id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders");
    }

    @Test
    void testSubqueryInFrom() {
        String sql = """
                SELECT *
                FROM (
                    SELECT * FROM users WHERE active = true
                ) active_users
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("users");
    }

    @Test
    void testSubqueryInWhere() {
        String sql = """
                SELECT *
                FROM orders
                WHERE user_id IN (SELECT id FROM users WHERE active = true)
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("orders", "users");
    }

    @Test
    void testSubqueryInSelect() {
        String sql = """
                SELECT
                    name,
                    (SELECT COUNT(*) FROM orders WHERE orders.user_id = users.id) as order_count
                FROM users
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders");
    }

    @Test
    void testUnionQuery() {
        String sql = """
                SELECT name FROM users
                UNION
                SELECT name FROM customers
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "customers");
    }

    @Test
    void testMultipleTablesInFrom() {
        String sql = """
                SELECT *
                FROM users, orders
                WHERE users.id = orders.user_id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders");
    }

    @Test
    void testMixedSchemaQualification() {
        String sql = """
                SELECT *
                FROM schema1.table1
                JOIN schema2.table2 ON schema1.table1.id = schema2.table2.id
                JOIN table3 ON schema2.table2.id = table3.id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(3);
        assertThat(getFullyQualifiedNames(dependencies)).containsExactlyInAnyOrder("schema1.table1", "schema2.table2", "table3");
    }

    @Test
    void testQuotedIdentifiers() {
        String sql = """
                SELECT *
                FROM "my-schema"."my-table"
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        TableReference ref = dependencies.iterator().next();
        assertThat(ref.getSchema()).isEqualTo("my-schema");
        assertThat(ref.getTable()).isEqualTo("my-table");
    }

    @Test
    void testExistsSubquery() {
        String sql = """
                SELECT *
                FROM users
                WHERE EXISTS (
                    SELECT 1 FROM orders WHERE orders.user_id = users.id
                )
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders");
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
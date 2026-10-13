// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for tables passed to table functions as TABLE(...) arguments.
 */
class TrinoSqlParserTableFunctionTests {

    private TrinoSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new TrinoSqlParser();
    }

    @Test
    void testExcludeColumnsTableArgument() {
        String sql = """
                SELECT *
                FROM TABLE(exclude_columns(
                    input => TABLE(orders),
                    columns => DESCRIPTOR(comment)))
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(getTableNames(dependencies)).containsExactly("orders");
    }

    @Test
    void testQualifiedTableArgument() {
        String sql = """
                SELECT *
                FROM TABLE(exclude_columns(
                    input => TABLE(sales.orders),
                    columns => DESCRIPTOR(comment)))
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(getFullyQualifiedNames(dependencies)).containsExactly("sales.orders");
    }

    @Test
    void testAliasedTableArgument() {
        String sql = """
                SELECT *
                FROM TABLE(exclude_columns(
                    input => TABLE(orders) AS o,
                    columns => DESCRIPTOR(comment)))
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(getTableNames(dependencies)).containsExactly("orders");
    }

    @Test
    void testSubqueryTableArgument() {
        String sql = """
                SELECT *
                FROM TABLE(exclude_columns(
                    input => TABLE(
                        SELECT o.*, c.name
                        FROM orders o
                        JOIN customers c ON o.customer_id = c.id),
                    columns => DESCRIPTOR(comment)))
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("orders", "customers");
    }

    @Test
    void testPartitionedTableArgumentWithSubqueryInOrderBy() {
        String sql = """
                SELECT *
                FROM TABLE(exclude_columns(
                    input => TABLE(orders)
                        PARTITION BY region
                        ORDER BY (SELECT max(rank) FROM priorities),
                    columns => DESCRIPTOR(comment)))
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("orders", "priorities");
    }

    @Test
    void testCteAsTableArgumentIsNotDependency() {
        String sql = """
                WITH recent AS (SELECT * FROM orders WHERE order_date > DATE '2026-01-01')
                SELECT *
                FROM TABLE(exclude_columns(
                    input => TABLE(recent),
                    columns => DESCRIPTOR(comment)))
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(getTableNames(dependencies)).containsExactly("orders");
    }

    @Test
    void testQualifiedTableArgumentMatchingCteName() {
        String sql = """
                WITH orders AS (SELECT * FROM staging.orders WHERE status = 'open')
                SELECT *
                FROM TABLE(exclude_columns(
                    input => TABLE(sales.orders),
                    columns => DESCRIPTOR(comment)))
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(getFullyQualifiedNames(dependencies)).containsExactlyInAnyOrder("staging.orders", "sales.orders");
    }

    @Test
    void testTableFunctionJoinedWithTable() {
        String sql = """
                SELECT *
                FROM TABLE(exclude_columns(
                    input => TABLE(orders),
                    columns => DESCRIPTOR(comment))) t
                JOIN customers c ON t.customer_id = c.id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("orders", "customers");
    }

    @Test
    void testTableFunctionWithoutTableArgument() {
        String sql = """
                SELECT *
                FROM TABLE(sequence(start => 1, stop => 10))
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).isEmpty();
    }

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

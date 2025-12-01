// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for complex SQL parsing with WITH clauses (CTEs).
 * <p>
 * CTEs should NOT appear in dependencies - only the actual tables/views they reference.
 */
class TestTrinoSqlParserWithClauses {

    private TrinoSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new TrinoSqlParser();
    }

    @Test
    void testSimpleCTE() {
        String sql = """
                WITH active_users AS (
                    SELECT * FROM users WHERE active = true
                )
                SELECT * FROM active_users
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("users");
    }

    @Test
    void testMultipleCTEs() {
        String sql = """
                WITH
                    active_users AS (
                        SELECT * FROM users WHERE active = true
                    ),
                    recent_orders AS (
                        SELECT * FROM orders WHERE created_at > CURRENT_DATE - INTERVAL '30' DAY
                    )
                SELECT u.name, o.total
                FROM active_users u
                JOIN recent_orders o ON u.id = o.user_id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders");
    }

    @Test
    void testNestedCTEs() {
        String sql = """
                WITH
                    base_users AS (
                        SELECT * FROM users WHERE active = true
                    ),
                    user_orders AS (
                        SELECT u.*, o.total
                        FROM base_users u
                        JOIN orders o ON u.id = o.user_id
                    ),
                    user_products AS (
                        SELECT uo.*, p.name as product_name
                        FROM user_orders uo
                        JOIN order_items oi ON uo.id = oi.order_id
                        JOIN products p ON oi.product_id = p.id
                    )
                SELECT * FROM user_products
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(4);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "orders", "order_items", "products");
    }

    @Test
    void testCTEWithSubquery() {
        String sql = """
                WITH user_stats AS (
                    SELECT
                        user_id,
                        COUNT(*) as order_count,
                        (SELECT name FROM users WHERE id = orders.user_id) as user_name
                    FROM orders
                    GROUP BY user_id
                )
                SELECT * FROM user_stats WHERE order_count > 5
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("orders", "users");
    }

    @Test
    void testCTEWithJoins() {
        String sql = """
                WITH user_data AS (
                    SELECT u.*, a.address
                    FROM users u
                    LEFT JOIN addresses a ON u.id = a.user_id
                )
                SELECT ud.*, o.total
                FROM user_data ud
                JOIN orders o ON ud.id = o.user_id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(3);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("users", "addresses", "orders");
    }

    @Test
    void testRecursiveCTE() {
        String sql = """
                WITH RECURSIVE employee_hierarchy AS (
                    -- Base case
                    SELECT id, name, manager_id, 1 as level
                    FROM employees
                    WHERE manager_id IS NULL
                
                    UNION ALL
                
                    -- Recursive case
                    SELECT e.id, e.name, e.manager_id, eh.level + 1
                    FROM employees e
                    JOIN employee_hierarchy eh ON e.manager_id = eh.id
                )
                SELECT * FROM employee_hierarchy
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getTableNames(dependencies)).containsExactly("employees");
    }

    @Test
    void testCTEWithUnion() {
        String sql = """
                WITH all_contacts AS (
                    SELECT name, email FROM customers
                    UNION
                    SELECT name, email FROM suppliers
                )
                SELECT * FROM all_contacts ORDER BY name
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("customers", "suppliers");
    }

    @Test
    void testMultipleCTEsWithComplexDependencies() {
        String sql = """
                WITH
                    product_sales AS (
                        SELECT
                            p.id,
                            p.name,
                            SUM(oi.quantity) as total_sold
                        FROM products p
                        JOIN order_items oi ON p.id = oi.product_id
                        GROUP BY p.id, p.name
                    ),
                    category_performance AS (
                        SELECT
                            c.id,
                            c.name,
                            COUNT(DISTINCT ps.id) as product_count
                        FROM categories c
                        JOIN products p ON c.id = p.category_id
                        JOIN product_sales ps ON p.id = ps.id
                        GROUP BY c.id, c.name
                    ),
                    top_categories AS (
                        SELECT * FROM category_performance
                        WHERE product_count > 10
                    )
                SELECT * FROM top_categories
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(3);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("products", "order_items", "categories");
    }

    @Test
    void testCTEWithSchemaQualifiedTables() {
        String sql = """
                WITH customer_orders AS (
                    SELECT c.*, o.total
                    FROM sales.customers c
                    JOIN sales.orders o ON c.id = o.customer_id
                )
                SELECT * FROM customer_orders
                JOIN analytics.customer_segments cs ON customer_orders.id = cs.customer_id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(3);
        assertThat(getFullyQualifiedNames(dependencies))
                .containsExactlyInAnyOrder(
                        "sales.customers",
                        "sales.orders",
                        "analytics.customer_segments"
                );
    }

    @Test
    void testCTEReferencingAnotherCTE() {
        String sql = """
                WITH
                    step1 AS (
                        SELECT * FROM table1
                    ),
                    step2 AS (
                        SELECT * FROM step1
                        JOIN table2 ON step1.id = table2.id
                    ),
                    step3 AS (
                        SELECT * FROM step2
                        JOIN table3 ON step2.id = table3.id
                    )
                SELECT * FROM step3
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(3);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("table1", "table2", "table3");
    }

    @Test
    void testComplexRealWorldExample() {
        // This is the example from ARCHITECTURE.md
        String sql = """
                WITH temp AS (
                    SELECT * FROM schema1.table1
                    WHERE description LIKE '%schema2.fake_table%'
                )
                SELECT * FROM temp
                JOIN schema3.table2 ON temp.id = table2.id
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getFullyQualifiedNames(dependencies)).containsExactlyInAnyOrder("schema1.table1", "schema3.table2");
    }

    @Test
    void testCTEInCreateView() {
        String sql = """
                CREATE VIEW prod.user_summary AS
                WITH active_users AS (
                    SELECT * FROM staging.users WHERE active = true
                )
                SELECT id, name FROM active_users
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(1);
        assertThat(getFullyQualifiedNames(dependencies)).containsExactly("staging.users");
    }

    @Test
    void testMultipleLevelsOfCTENesting() {
        String sql = """
                WITH
                    level1 AS (
                        SELECT * FROM base_table
                    ),
                    level2 AS (
                        WITH inner_cte AS (
                            SELECT * FROM level1
                        )
                        SELECT * FROM inner_cte
                        JOIN another_table ON inner_cte.id = another_table.id
                    )
                SELECT * FROM level2
                """;

        Set<TableReference> dependencies = parser.extractDependencies(sql);
        assertThat(dependencies).hasSize(2);
        assertThat(getTableNames(dependencies)).containsExactlyInAnyOrder("base_table", "another_table");
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

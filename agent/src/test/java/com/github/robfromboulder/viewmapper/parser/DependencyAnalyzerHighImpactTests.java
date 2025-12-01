// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for "high-impact views" algorithm (by out-degree).
 * <p>
 * High-impact views are those with the most dependents - views that many other views depend on, making them foundational or core to the schema.
 */
class DependencyAnalyzerHighImpactTests {

    private DependencyAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DependencyAnalyzer();
    }

    @Test
    void testSimpleLinearChain() {
        // a -> b -> c -> d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");

        Map<String, Integer> highImpact = analyzer.findHighImpactViews(5);

        // a is most impactful (3 dependents transitively, but we count direct edges)
        // a has 1 dependent (b)
        // b has 1 dependent (c)
        // c has 1 dependent (d)
        // d has 0 dependents
        assertThat(highImpact).containsEntry("a", 1);
        assertThat(highImpact).containsEntry("b", 1);
        assertThat(highImpact).containsEntry("c", 1);
        assertThat(highImpact).containsEntry("d", 0);
    }

    @Test
    void testDiamondPattern() {
        //     a
        //    / \
        //   b   c
        //    \ /
        //     d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM a");
        analyzer.addView("d", "SELECT * FROM b JOIN C ON B.id = C.id");

        Map<String, Integer> highImpact = analyzer.findHighImpactViews(5);

        // a has 2 dependents (B, C)
        // b has 1 dependent (D)
        // c has 1 dependent (D)
        // d has 0 dependents
        assertThat(highImpact.get("a")).isEqualTo(2);
        assertThat(highImpact.get("b")).isEqualTo(1);
        assertThat(highImpact.get("c")).isEqualTo(1);
        assertThat(highImpact.get("d")).isEqualTo(0);

        // a should be first (highest impact)
        assertThat(highImpact.keySet().iterator().next()).isEqualTo("a");
    }

    @Test
    void testStarPattern() {
        //       core
        //      / | \
        //     v1 v2 v3
        analyzer.addView("view1", "SELECT * FROM core");
        analyzer.addView("view2", "SELECT * FROM core");
        analyzer.addView("view3", "SELECT * FROM core");

        Map<String, Integer> highImpact = analyzer.findHighImpactViews(1);

        // core has 3 dependents
        assertThat(highImpact).containsEntry("core", 3);
        assertThat(highImpact).hasSize(1);
    }

    @Test
    void testMultiLevelImpact() {
        //       base
        //      /    \
        //     l1a   l1b
        //    / \     |
        //  l2a l2b  l2c
        analyzer.addView("level1a", "SELECT * FROM base");
        analyzer.addView("level1b", "SELECT * FROM base");
        analyzer.addView("level2a", "SELECT * FROM level1a");
        analyzer.addView("level2b", "SELECT * FROM level1a");
        analyzer.addView("level2c", "SELECT * FROM level1b");

        Map<String, Integer> highImpact = analyzer.findHighImpactViews(10);

        // base: 2 dependents
        // level1a: 2 dependents
        // level1b: 1 dependent
        // level2a, level2b, level2c: 0 dependents each
        assertThat(highImpact.get("base")).isEqualTo(2);
        assertThat(highImpact.get("level1a")).isEqualTo(2);
        assertThat(highImpact.get("level1b")).isEqualTo(1);
        assertThat(highImpact.get("level2a")).isEqualTo(0);

        // top 3 should be base, level1a (tied at 2), then level1b (1)
        Map.Entry<String, Integer>[] entries = highImpact.entrySet().toArray(new Map.Entry[0]);
        assertThat(entries[0].getValue()).isEqualTo(2);
        assertThat(entries[1].getValue()).isEqualTo(2);
        assertThat(entries[2].getValue()).isEqualTo(1);
    }

    @Test
    void testLimitResults() {
        analyzer.addView("v1", "SELECT * FROM base");
        analyzer.addView("v2", "SELECT * FROM base");
        analyzer.addView("v3", "SELECT * FROM base");
        analyzer.addView("v4", "SELECT * FROM v1");
        analyzer.addView("v5", "SELECT * FROM v1");

        Map<String, Integer> top2 = analyzer.findHighImpactViews(2);

        // should return only top 2
        assertThat(top2).hasSize(2);

        // base has 3 dependents, v1 has 2 dependents
        assertThat(top2).containsEntry("base", 3);
        assertThat(top2).containsEntry("v1", 2);
    }

    @Test
    void testEmptyGraph() {
        Map<String, Integer> highImpact = analyzer.findHighImpactViews(5);
        assertThat(highImpact).isEmpty();
    }

    @Test
    void testSingleView() {
        analyzer.addView("lonely", "SELECT * FROM base_table");

        Map<String, Integer> highImpact = analyzer.findHighImpactViews(5);

        // lonely: 0 dependents
        // base_table: 1 dependent
        assertThat(highImpact.get("lonely")).isEqualTo(0);
        assertThat(highImpact.get("base_table")).isEqualTo(1);
    }

    @Test
    void testMultipleSourceDependencies() {
        analyzer.addView("combined", "SELECT * FROM source1 JOIN source2 ON source1.id = source2.id");

        Map<String, Integer> highImpact = analyzer.findHighImpactViews(5);

        // both sources have 1 dependent
        assertThat(highImpact.get("source1")).isEqualTo(1);
        assertThat(highImpact.get("source2")).isEqualTo(1);
        assertThat(highImpact.get("combined")).isEqualTo(0);
    }

    @Test
    void testRealWorldScenario() {
        analyzer.addView("customer_360", "SELECT * FROM customers JOIN profiles ON customers.id = profiles.customer_id");
        analyzer.addView("customer_orders", "SELECT * FROM customer_360 JOIN orders ON customer_360.id = orders.customer_id");
        analyzer.addView("customer_revenue", "SELECT * FROM customer_360 JOIN transactions ON customer_360.id = transactions.customer_id");
        analyzer.addView("customer_segments", "SELECT * FROM customer_360 WHERE segment IS NOT NULL");
        analyzer.addView("marketing_report", "SELECT * FROM customer_segments");
        analyzer.addView("executive_dashboard", "SELECT * FROM customer_revenue JOIN customer_orders ON customer_revenue.id = customer_orders.id");

        Map<String, Integer> highImpact = analyzer.findHighImpactViews(3);

        // customer_360 should be #1 with 3 direct dependents
        assertThat(highImpact.get("customer_360")).isEqualTo(3);

        // should be in top 3
        assertThat(highImpact).containsKey("customer_360");
    }

    @Test
    void testQualifiedNames() {
        analyzer.addView("schema1.view1", "SELECT * FROM catalog1.schema1.base");
        analyzer.addView("schema1.view2", "SELECT * FROM catalog1.schema1.base");

        Map<String, Integer> highImpact = analyzer.findHighImpactViews(5);

        assertThat(highImpact.get("catalog1.schema1.base")).isEqualTo(2);
    }

}

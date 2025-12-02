// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for "leaf views" algorithm (zero out-degree).
 * <p>
 * Leaf views have no dependents - nothing depends on them. These are typically final outputs, reports, or end-user facing views.
 */
class DependencyAnalyzerLeafViewTests {

    private DependencyAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DependencyAnalyzer();
    }

    @Test
    void testSimpleChainHasOneLeaf() {
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");

        List<String> leaves = analyzer.findLeafViews();

        // only d is a leaf
        assertThat(leaves).containsExactly("d");
    }

    @Test
    void testDiamondPatternHasOneLeaf() {
        //     A
        //    / \
        //   B   C
        //    \ /
        //     D
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM a");
        analyzer.addView("d", "SELECT * FROM b JOIN C ON B.id = C.id");

        List<String> leaves = analyzer.findLeafViews();

        // only d is a leaf
        assertThat(leaves).containsExactly("d");
    }

    @Test
    void testMultipleLeaves() {
        //       base
        //      /    \
        //     l1a   l1b
        //    / \
        //  l2a l2b
        analyzer.addView("level1a", "SELECT * FROM base");
        analyzer.addView("level1b", "SELECT * FROM base");
        analyzer.addView("level2a", "SELECT * FROM level1a");
        analyzer.addView("level2b", "SELECT * FROM level1a");

        List<String> leaves = analyzer.findLeafViews();

        assertThat(leaves).containsExactlyInAnyOrder("level1b", "level2a", "level2b");
    }

    @Test
    void testStarPatternAllLeaves() {
        //       core
        //      / | \
        //     v1 v2 v3
        analyzer.addView("view1", "SELECT * FROM core");
        analyzer.addView("view2", "SELECT * FROM core");
        analyzer.addView("view3", "SELECT * FROM core");

        List<String> leaves = analyzer.findLeafViews();

        assertThat(leaves).containsExactlyInAnyOrder("view1", "view2", "view3");
    }

    @Test
    void testEmptyGraph() {
        List<String> leaves = analyzer.findLeafViews();
        assertThat(leaves).isEmpty();
    }

    @Test
    void testSingleViewIsLeaf() {
        analyzer.addView("lonely", "SELECT * FROM base_table");

        List<String> leaves = analyzer.findLeafViews();

        // lonely is a leaf (no dependents)
        // base_table is NOT a leaf (lonely depends on it)
        assertThat(leaves).containsExactly("lonely");
    }

    @Test
    void testAllViewsAreLeaves() {
        analyzer.addView("report1", "SELECT * FROM base");
        analyzer.addView("report2", "SELECT * FROM base");
        analyzer.addView("report3", "SELECT * FROM base");

        List<String> leaves = analyzer.findLeafViews();

        assertThat(leaves).containsExactlyInAnyOrder("report1", "report2", "report3");
    }

    @Test
    void testRealWorldScenarioLeafReports() {
        // simulate a schema where leaf views are final reports
        analyzer.addView("customer_360", "SELECT * FROM customers JOIN profiles ON customers.id = profiles.customer_id");
        analyzer.addView("customer_orders", "SELECT * FROM customer_360 JOIN orders ON customer_360.id = orders.customer_id");
        analyzer.addView("customer_revenue", "SELECT * FROM customer_360 JOIN transactions ON customer_360.id = transactions.customer_id");
        analyzer.addView("customer_segments", "SELECT * FROM customer_360 WHERE segment IS NOT NULL");

        // these are final reports (leaves)
        analyzer.addView("marketing_report", "SELECT * FROM customer_segments");
        analyzer.addView("executive_dashboard", "SELECT * FROM customer_revenue JOIN customer_orders ON customer_revenue.id = customer_orders.id");
        analyzer.addView("monthly_summary", "SELECT * FROM customer_revenue");

        List<String> leaves = analyzer.findLeafViews();

        assertThat(leaves).containsExactlyInAnyOrder("marketing_report", "executive_dashboard", "monthly_summary");
    }

    @Test
    void testLeavesSortedAlphabetically() {
        analyzer.addView("zebra_report", "SELECT * FROM base");
        analyzer.addView("alpha_report", "SELECT * FROM base");
        analyzer.addView("middle_report", "SELECT * FROM base");

        List<String> leaves = analyzer.findLeafViews();

        // verify that results are sorted
        assertThat(leaves).containsExactly("alpha_report", "middle_report", "zebra_report");
    }

    @Test
    void testQualifiedNamesAsLeaves() {
        analyzer.addView("schema1.report", "SELECT * FROM catalog1.schema1.base");
        analyzer.addView("schema2.dashboard", "SELECT * FROM catalog1.schema1.base");

        List<String> leaves = analyzer.findLeafViews();

        assertThat(leaves).containsExactlyInAnyOrder("schema1.report", "schema2.dashboard");
    }

    @Test
    void testComplexNetworkIdentifiesLeaves() {
        //        t1    t2
        //         \   /
        //          v1
        //         / \
        //       v2   v3
        //       |     |
        //      v4    v5
        analyzer.addView("v1", "SELECT * FROM t1 JOIN t2 ON t1.id = t2.id");
        analyzer.addView("v2", "SELECT * FROM v1");
        analyzer.addView("v3", "SELECT * FROM v1");
        analyzer.addView("v4", "SELECT * FROM v2");
        analyzer.addView("v5", "SELECT * FROM v3");

        List<String> leaves = analyzer.findLeafViews();

        assertThat(leaves).containsExactlyInAnyOrder("v4", "v5");
    }

    @Test
    void testMixedTableAndViewLeaves() {
        analyzer.addView("view1", "SELECT * FROM table1");
        analyzer.addView("view2", "SELECT * FROM view1");

        List<String> leaves = analyzer.findLeafViews();

        // view2 is a leaf (no views depend on it)
        // view1 is not a leaf (view2 depends on it)
        // table1 is not a leaf (view1 depends on it)
        assertThat(leaves).containsExactly("view2");
    }

    @Test
    void testParallelBranches() {
        //   base1    base2
        //     |        |
        //    v1       v2
        //     |        |
        //   leaf1    leaf2
        analyzer.addView("v1", "SELECT * FROM base1");
        analyzer.addView("leaf1", "SELECT * FROM v1");
        analyzer.addView("v2", "SELECT * FROM base2");
        analyzer.addView("leaf2", "SELECT * FROM v2");

        List<String> leaves = analyzer.findLeafViews();

        assertThat(leaves).containsExactlyInAnyOrder("leaf1", "leaf2");
    }

}

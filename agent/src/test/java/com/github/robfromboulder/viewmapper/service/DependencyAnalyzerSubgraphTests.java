// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for subgraph extraction with BFS.
 * <p>
 * Subgraph extraction allows focusing on a specific view and its surrounding context by traversing upstream (dependencies) and downstream (dependents).
 */
class DependencyAnalyzerSubgraphTests {

    private DependencyAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DependencyAnalyzer();
    }

    @Test
    void testExtractFocusViewOnly() {
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");

        Set<String> subgraph = analyzer.findSubgraph("b", 0, 0, 0);

        // Only the focus view itself
        assertThat(subgraph).containsExactly("b");
    }

    @Test
    void testExtractUpstreamDepth1() {
        // a -> b -> c -> d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");

        Set<String> subgraph = analyzer.findSubgraph("c", 1, 0, 0);

        // c and its immediate dependency b
        assertThat(subgraph).containsExactlyInAnyOrder("c", "b");
    }

    @Test
    void testExtractUpstreamDepth2() {
        // a -> b -> c -> d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");

        Set<String> subgraph = analyzer.findSubgraph("c", 2, 0, 0);

        // c and dependencies up to depth 2: b and a
        assertThat(subgraph).containsExactlyInAnyOrder("c", "b", "a");
    }

    @Test
    void testExtractDownstreamDepth1() {
        // a -> b -> c -> d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");

        Set<String> subgraph = analyzer.findSubgraph("b", 0, 1, 0);

        // b and its immediate dependent c
        assertThat(subgraph).containsExactlyInAnyOrder("b", "c");
    }

    @Test
    void testExtractDownstreamDepth2() {
        // a -> b -> c -> d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");

        Set<String> subgraph = analyzer.findSubgraph("b", 0, 2, 0);

        // b and dependents up to depth 2: c and d
        assertThat(subgraph).containsExactlyInAnyOrder("b", "c", "d");
    }

    @Test
    void testExtractBidirectional() {
        // a -> b -> c -> d -> e
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");
        analyzer.addView("e", "SELECT * FROM d");

        Set<String> subgraph = analyzer.findSubgraph("c", 1, 1, 0);

        // c, 1 upstream (B), 1 downstream (D)
        assertThat(subgraph).containsExactlyInAnyOrder("c", "b", "d");
    }

    @Test
    void testExtractDiamondPattern() {
        //     a
        //    / \
        //   b   c
        //    \ /
        //     d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM a");
        analyzer.addView("d", "SELECT * FROM b JOIN C ON B.id = C.id");

        Set<String> subgraph = analyzer.findSubgraph("d", 1, 0, 0);

        // d and its immediate dependencies: b and c
        assertThat(subgraph).containsExactlyInAnyOrder("d", "b", "c");
    }

    @Test
    void testExtractDiamondFullDepth() {
        //     a
        //    / \
        //   b   c
        //    \ /
        //     d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM a");
        analyzer.addView("d", "SELECT * FROM b JOIN C ON B.id = C.id");

        Set<String> subgraph = analyzer.findSubgraph("d", 2, 0, 0);

        // d and all upstream: b, c, a
        assertThat(subgraph).containsExactlyInAnyOrder("d", "b", "c", "a");
    }

    @Test
    void testExtractComplexNetwork() {
        //   s1   s2   s3
        //    \   |   /
        //      hub
        //     / | \
        //   d1 d2 d3
        analyzer.addView("hub", "SELECT * FROM s1 JOIN s2 ON s1.id = s2.id JOIN s3 ON s2.id = s3.id");
        analyzer.addView("d1", "SELECT * FROM hub");
        analyzer.addView("d2", "SELECT * FROM hub");
        analyzer.addView("d3", "SELECT * FROM hub");

        Set<String> subgraph = analyzer.findSubgraph("hub", 1, 1, 0);

        // hub, 3 upstream, 3 downstream
        assertThat(subgraph).containsExactlyInAnyOrder("hub", "s1", "s2", "s3", "d1", "d2", "d3");
    }

    @Test
    void testMaxNodesLimit() {
        // a -> b -> c -> d -> e
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");
        analyzer.addView("e", "SELECT * FROM d");

        Set<String> subgraph = analyzer.findSubgraph("c", 2, 2, 3);

        // would normally return 5 nodes (a, b, c, d, e) but limited to 3
        // should keep C (focus) + highest degree nodes
        assertThat(subgraph).hasSize(3);
        assertThat(subgraph).contains("c");
    }

    @Test
    void testMaxNodesUnlimited() {
        // a -> b -> c -> d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");

        Set<String> subgraph = analyzer.findSubgraph("b", 5, 5, 0);

        // all reachable nodes (maxNodes = 0 means unlimited)
        assertThat(subgraph).containsExactlyInAnyOrder("a", "b", "c", "d");
    }

    @Test
    void testNonExistentView() {
        analyzer.addView("b", "SELECT * FROM a");

        Set<String> subgraph = analyzer.findSubgraph("nonexistent", 1, 1, 0);

        assertThat(subgraph).isEmpty();
    }

    @Test
    void testIsolatedView() {
        analyzer.addView("isolated", "SELECT * FROM base");

        Set<String> subgraph = analyzer.findSubgraph("isolated", 1, 1, 0);

        // isolated + base (dependency)
        assertThat(subgraph).containsExactlyInAnyOrder("isolated", "base");
    }

    @Test
    void testRealWorldCustomer360Subgraph() {
        // simulate exploring customer_360 view
        analyzer.addView("customer_360", "SELECT * FROM customers JOIN profiles ON customers.id = profiles.customer_id");
        analyzer.addView("customer_orders", "SELECT * FROM customer_360 JOIN orders ON customer_360.id = orders.customer_id");
        analyzer.addView("customer_revenue", "SELECT * FROM customer_360 JOIN transactions ON customer_360.id = transactions.customer_id");
        analyzer.addView("customer_segments", "SELECT * FROM customer_360 WHERE segment IS NOT NULL");
        analyzer.addView("marketing_report", "SELECT * FROM customer_segments");

        // extract customer_360 with 1 level upstream, 2 levels downstream
        Set<String> subgraph = analyzer.findSubgraph("customer_360", 1, 2, 0);

        // should include:
        // - customer_360 (focus)
        // - customers, profiles (1 upstream)
        // - customer_orders, customer_revenue, customer_segments (1 downstream)
        // - marketing_report (2 downstream via customer_segments)
        assertThat(subgraph).contains(
                "customer_360",
                "customers",
                "profiles",
                "customer_orders",
                "customer_revenue",
                "customer_segments",
                "marketing_report"
        );
    }

    @Test
    void testDepthLimitPreventsFullGraphTraversal() {
        analyzer.addView("v1", "SELECT * FROM base");
        analyzer.addView("v2", "SELECT * FROM v1");
        analyzer.addView("v3", "SELECT * FROM v2");
        analyzer.addView("v4", "SELECT * FROM v3");
        analyzer.addView("v5", "SELECT * FROM v4");

        // extract from v3 with depth limit 1
        Set<String> subgraph = analyzer.findSubgraph("v3", 1, 1, 0);

        // should not include base (2 levels up) or v5 (2 levels down)
        assertThat(subgraph).containsExactlyInAnyOrder("v3", "v2", "v4");
        assertThat(subgraph).doesNotContain("base", "v5");
    }

    @Test
    void testMultipleBranchesUpstream() {
        //   t1   t2   t3
        //    \   |   /
        //      view
        analyzer.addView("view", "SELECT * FROM t1 JOIN t2 ON t1.id = t2.id JOIN t3 ON t2.id = t3.id");

        Set<String> subgraph = analyzer.findSubgraph("view", 1, 0, 0);

        assertThat(subgraph).containsExactlyInAnyOrder("view", "t1", "t2", "t3");
    }

    @Test
    void testMultipleBranchesDownstream() {
        //      view
        //     / | \
        //   v1 v2 v3
        analyzer.addView("v1", "SELECT * FROM view");
        analyzer.addView("v2", "SELECT * FROM view");
        analyzer.addView("v3", "SELECT * FROM view");

        Set<String> subgraph = analyzer.findSubgraph("view", 0, 1, 0);

        assertThat(subgraph).containsExactlyInAnyOrder("view", "v1", "v2", "v3");
    }

    @Test
    void testQualifiedNamesInSubgraph() {
        analyzer.addView("schema1.view1", "SELECT * FROM catalog1.schema1.base");
        analyzer.addView("schema1.view2", "SELECT * FROM schema1.view1");

        Set<String> subgraph = analyzer.findSubgraph("schema1.view1", 1, 1, 0);

        assertThat(subgraph).containsExactlyInAnyOrder("schema1.view1", "catalog1.schema1.base", "schema1.view2");
    }

    @Test
    void testAsymmetricDepths() {
        // a -> b -> c -> d -> e -> f
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");
        analyzer.addView("e", "SELECT * FROM d");
        analyzer.addView("f", "SELECT * FROM e");

        // from D: 2 upstream, 1 downstream
        Set<String> subgraph = analyzer.findSubgraph("d", 2, 1, 0);

        assertThat(subgraph).containsExactlyInAnyOrder("d", "c", "b", "e");
        assertThat(subgraph).doesNotContain("a", "f");
    }

}

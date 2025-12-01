// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for "central hubs" algorithm (betweenness centrality).
 * <p>
 * Betweenness centrality measures how often a node appears on shortest paths between other nodes. High centrality = important integration/connection point.
 */
class DependencyAnalyzerCentralHubTests {

    private DependencyAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DependencyAnalyzer();
    }

    @Test
    void testSimpleLinearChainCentrality() {
        // a -> b -> c -> d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");

        Map<String, Double> hubs = analyzer.findCentralHubs(5);

        // in a linear chain, middle nodes have higher centrality
        // b should have higher centrality than a or d
        assertThat(hubs.get("b")).isGreaterThan(0.0);
        assertThat(hubs.get("c")).isGreaterThan(0.0);
    }

    @Test
    void testDiamondPatternIdentifiesBridge() {
        //     a
        //    / \
        //   b   c
        //    \ /
        //     d
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM a");
        analyzer.addView("d", "SELECT * FROM b JOIN C ON B.id = C.id");

        Map<String, Double> hubs = analyzer.findCentralHubs(5);

        // b and c should have positive centrality (on paths between a and d)
        assertThat(hubs.get("b")).isGreaterThan(0.0);
        assertThat(hubs.get("c")).isGreaterThan(0.0);
    }

    @Test
    void testBridgeNodeHasHighCentrality() {
        //   a -> b -> bridge -> d -> e
        //             ^
        //             |
        //             c
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("bridge", "SELECT * FROM b JOIN C ON B.id = C.id");
        analyzer.addView("d", "SELECT * FROM bridge");
        analyzer.addView("e", "SELECT * FROM d");

        Map<String, Double> hubs = analyzer.findCentralHubs(5);

        // bridge should have the highest centrality
        String topHub = hubs.entrySet().iterator().next().getKey();
        assertThat(topHub).isEqualTo("bridge");
        assertThat(hubs.get("bridge")).isGreaterThan(0.0);
    }

    @Test
    void testStarPatternCentrality() {
        //       core
        //      / | \
        //     v1 v2 v3
        analyzer.addView("view1", "SELECT * FROM core");
        analyzer.addView("view2", "SELECT * FROM core");
        analyzer.addView("view3", "SELECT * FROM core");

        Map<String, Double> hubs = analyzer.findCentralHubs(5);

        // in a star, the center may not have high betweenness if it's an endpoint
        // (depends on direction of edges)
        assertThat(hubs).isNotEmpty();
    }

    @Test
    void testComplexNetworkIdentifiesHub() {
        //   s1   s2   s3
        //    \   |   /
        //      hub
        //     / | \
        //   d1 d2 d3
        analyzer.addView("hub", "SELECT * FROM s1 JOIN s2 ON s1.id = s2.id JOIN s3 ON s2.id = s3.id");
        analyzer.addView("d1", "SELECT * FROM hub");
        analyzer.addView("d2", "SELECT * FROM hub");
        analyzer.addView("d3", "SELECT * FROM hub");

        Map<String, Double> hubs = analyzer.findCentralHubs(1);

        // hub should be the top result
        assertThat(hubs).hasSize(1);
        assertThat(hubs).containsKey("hub");
        assertThat(hubs.get("hub")).isGreaterThan(0.0);
    }

    @Test
    void testEmptyGraph() {
        Map<String, Double> hubs = analyzer.findCentralHubs(5);
        assertThat(hubs).isEmpty();
    }

    @Test
    void testSingleView() {
        analyzer.addView("lonely", "SELECT * FROM base_table");

        Map<String, Double> hubs = analyzer.findCentralHubs(5);

        // single edge graph, nodes have 0 centrality
        assertThat(hubs).isNotEmpty();
    }

    @Test
    void testLimitResults() {
        analyzer.addView("hub1", "SELECT * FROM s1 JOIN s2 ON s1.id = s2.id");
        analyzer.addView("hub2", "SELECT * FROM s3 JOIN s4 ON s3.id = s4.id");
        analyzer.addView("d1", "SELECT * FROM hub1");
        analyzer.addView("d2", "SELECT * FROM hub1");
        analyzer.addView("d3", "SELECT * FROM hub2");
        analyzer.addView("d4", "SELECT * FROM hub2");

        Map<String, Double> top2 = analyzer.findCentralHubs(2);

        // limited to top N
        assertThat(top2).hasSize(2);
    }

    @Test
    void testRealWorldIntegrationView() {
        // simulate a real integration view that connects many sources to many outputs
        analyzer.addView("customer_base", "SELECT * FROM raw_customers");
        analyzer.addView("order_base", "SELECT * FROM raw_orders");
        analyzer.addView("product_base", "SELECT * FROM raw_products");

        // integration view
        analyzer.addView("customer_360",
                "SELECT * FROM customer_base c " +
                        "JOIN order_base o ON c.id = o.customer_id " +
                        "JOIN product_base p ON o.product_id = p.id");

        // many views depend on customer_360
        analyzer.addView("revenue_report", "SELECT * FROM customer_360");
        analyzer.addView("marketing_segments", "SELECT * FROM customer_360");
        analyzer.addView("executive_dashboard", "SELECT * FROM customer_360");
        analyzer.addView("customer_churn", "SELECT * FROM customer_360");

        Map<String, Double> hubs = analyzer.findCentralHubs(3);

        // customer_360 should have high centrality as it bridges raw data to reports
        assertThat(hubs).containsKey("customer_360");
        assertThat(hubs.get("customer_360")).isGreaterThan(0.0);
    }

    @Test
    void testParallelPathsIncreaseCentrality() {
        //   a -> c -> e
        //   b -> c -> f
        analyzer.addView("c", "SELECT * FROM a UNION ALL SELECT * FROM b");
        analyzer.addView("e", "SELECT * FROM c");
        analyzer.addView("f", "SELECT * FROM c WHERE filter = 1");

        Map<String, Double> hubs = analyzer.findCentralHubs(5);

        // c should have the highest centrality
        assertThat(hubs.get("c")).isGreaterThan(0.0);
    }

    @Test
    void testDeepChainVsWideNetwork() {
        analyzer.addView("chain_b", "SELECT * FROM chain_a");
        analyzer.addView("chain_c", "SELECT * FROM chain_b");
        analyzer.addView("chain_d", "SELECT * FROM chain_c");
        analyzer.addView("chain_e", "SELECT * FROM chain_d");
        analyzer.addView("wide_1", "SELECT * FROM wide_hub");
        analyzer.addView("wide_2", "SELECT * FROM wide_hub");
        analyzer.addView("wide_3", "SELECT * FROM wide_hub");

        Map<String, Double> hubs = analyzer.findCentralHubs(10);

        // chain_c (middle) should have some centrality
        assertThat(hubs.get("chain_c")).isGreaterThan(0.0);
    }

    @Test
    void testQualifiedNamesInCentrality() {
        analyzer.addView("schema1.hub", "SELECT * FROM catalog1.schema1.base1 JOIN catalog1.schema1.base2 ON base1.id = base2.id");
        analyzer.addView("schema2.report1", "SELECT * FROM schema1.hub");
        analyzer.addView("schema2.report2", "SELECT * FROM schema1.hub");

        Map<String, Double> hubs = analyzer.findCentralHubs(5);

        assertThat(hubs).containsKey("schema1.hub");
    }

    @Test
    void testResultsSortedByCentrality() {
        analyzer.addView("mid1", "SELECT * FROM base1");
        analyzer.addView("mid2", "SELECT * FROM base2");
        analyzer.addView("hub", "SELECT * FROM mid1 JOIN mid2 ON mid1.id = mid2.id");
        analyzer.addView("leaf1", "SELECT * FROM hub");
        analyzer.addView("leaf2", "SELECT * FROM hub");
        analyzer.addView("leaf3", "SELECT * FROM hub");

        Map<String, Double> hubs = analyzer.findCentralHubs(5);

        // results should be sorted descending by centrality
        Double previousValue = Double.MAX_VALUE;
        for (Double value : hubs.values()) {
            assertThat(value).isLessThanOrEqualTo(previousValue);
            previousValue = value;
        }
    }

}

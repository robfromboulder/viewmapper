// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.FakeMessageService;
import com.github.robfromboulder.viewmapper.agent.FakeMessageService.ToolOutcome;
import com.github.robfromboulder.viewmapper.agent.types.SubgraphResult;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GenerateMermaidToolExecutor.
 */
class GenerateMermaidToolExecutorTest {

    private DependencyAnalyzer analyzer;
    private GenerateMermaidToolExecutor executor;

    @BeforeEach
    void setUp() {
        analyzer = new DependencyAnalyzer();
        executor = new GenerateMermaidToolExecutor(analyzer);
    }

    @Test
    void testGenerateSimpleDiagram() {
        // a -> b -> c
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");

        SubgraphResult subgraph = new SubgraphResult(Set.of("a", "b", "c"), "b");
        String mermaid = executor.renderSubgraph(subgraph);
        assertThat(mermaid).contains("```mermaid");
        assertThat(mermaid).contains("graph TB");
        assertThat(mermaid).contains("-->");
        assertThat(mermaid).contains("```");
    }

    @Test
    void testFocusViewStyling() {
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");

        SubgraphResult subgraph = new SubgraphResult(Set.of("a", "b", "c"), "b");
        String mermaid = executor.renderSubgraph(subgraph);
        assertThat(mermaid).contains("fill:#FF6B6B"); // focus view should have red styling
        assertThat(mermaid).contains("stroke:#D32F2F");
    }

    @Test
    void testUpstreamDownstreamStyling() {
        // a -> b -> c (b is focus, a is upstream, c is downstream)
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");

        SubgraphResult subgraph = new SubgraphResult(Set.of("a", "b", "c"), "b");
        String mermaid = executor.renderSubgraph(subgraph);
        assertThat(mermaid).contains("fill:#90CAF9"); // should have blue styling for upstream
        assertThat(mermaid).contains("fill:#A5D6A7"); // should have green styling for downstream
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
        analyzer.addView("d", "SELECT * FROM b JOIN c ON b.id = c.id");

        SubgraphResult subgraph = new SubgraphResult(Set.of("a", "b", "c", "d"), "d");
        String mermaid = executor.renderSubgraph(subgraph);
        assertThat(mermaid).contains("graph TB");
        int arrowCount = mermaid.split("-->").length - 1;
        assertThat(arrowCount).isGreaterThan(2); // should have multiple edges
    }

    @Test
    void testEmptySubgraph() {
        SubgraphResult subgraph = new SubgraphResult(Set.of(), "nonexistent");
        String mermaid = executor.renderSubgraph(subgraph);
        assertThat(mermaid).contains("No views in subgraph");
    }

    @Test
    void testTooLargeSubgraph() {
        Set<String> largeSet = new java.util.HashSet<>();
        for (int i = 1; i <= 101; i++) {
            largeSet.add("view" + i);
            analyzer.addView("view" + i, "SELECT * FROM base");
        }

        SubgraphResult subgraph = new SubgraphResult(largeSet, "view1");
        String mermaid = executor.renderSubgraph(subgraph);
        assertThat(mermaid).contains("too large");
        assertThat(mermaid).contains("101 views");
    }

    @Test
    void testQualifiedNameFormatting() {
        analyzer.addView("schema1.view1", "SELECT * FROM catalog1.schema2.table1");
        analyzer.addView("schema1.view2", "SELECT * FROM schema1.view1");

        SubgraphResult subgraph = new SubgraphResult(Set.of("catalog1.schema2.table1", "schema1.view1", "schema1.view2"), "schema1.view1");
        String mermaid = executor.renderSubgraph(subgraph);
        assertThat(mermaid).contains("schema"); // should contain simplified labels (not full qualified names in display)
    }

    @Test
    void testNodeIdGeneration() {
        analyzer.addView("view-with-dashes", "SELECT * FROM base");
        analyzer.addView("view.with.dots", "SELECT * FROM base");

        SubgraphResult subgraph = new SubgraphResult(Set.of("base", "view-with-dashes", "view.with.dots"), "base");
        String mermaid = executor.renderSubgraph(subgraph);
        assertThat(mermaid).contains("node1");
        assertThat(mermaid).contains("node2");
        assertThat(mermaid).contains("node3");
    }

    @Test
    void testMultipleEdgesFromSameNode() {
        analyzer.addView("v1", "SELECT * FROM base");
        analyzer.addView("v2", "SELECT * FROM base");
        analyzer.addView("v3", "SELECT * FROM base");

        SubgraphResult subgraph = new SubgraphResult(Set.of("base", "v1", "v2", "v3"), "base");
        String mermaid = executor.renderSubgraph(subgraph);
        int arrowCount = mermaid.split("-->").length - 1;
        assertThat(arrowCount).isEqualTo(3);
    }

    @Test
    void testGenerateFullSchemaDiagram() {
        analyzer.addView("dim_customers", "SELECT * FROM raw.customers");
        analyzer.addView("fact_orders", "SELECT * FROM raw.orders");
        analyzer.addView("customer_orders", "SELECT * FROM dim_customers JOIN fact_orders ON dim_customers.id = fact_orders.customer_id");

        String mermaid = executor.generateFullSchemaDiagram();
        assertThat(mermaid).contains("```mermaid");
        assertThat(mermaid).contains("graph TB");
        assertThat(mermaid).contains("-->");
        assertThat(mermaid).contains("```");
        assertThat(mermaid).contains("dim_customers");
        assertThat(mermaid).contains("fact_orders");
        assertThat(mermaid).contains("customer_orders");
    }

    @Test
    void testGenerateFullSchemaDiagramEmpty() {
        String mermaid = executor.generateFullSchemaDiagram();
        assertThat(mermaid).contains("No views in schema");
    }

    @Test
    void testGenerateMermaidFromToolArguments() {
        // a -> b -> c, called the way the model calls it, with JSON arguments
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");

        String mermaid = executeTool("generateMermaid", "b", 1, 1);
        assertThat(mermaid).contains("```mermaid");
        assertThat(mermaid).contains("\"a\"").contains("\"b\"").contains("\"c\"");
        assertThat(mermaid).contains("stroke-width:3px");
    }

    @Test
    void testGenerateMermaidRespectsDepth() {
        // a -> b -> c -> d, depth 1 around c drops a
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");

        String mermaid = executeTool("generateMermaid", "c", 1, 1);
        assertThat(mermaid).contains("\"b\"").contains("\"c\"").contains("\"d\"");
        assertThat(mermaid).doesNotContain("\"a\"");
    }

    @Test
    void testGenerateMermaidReportsTruncation() {
        // a -> b -> c -> d, capped below the views within depth
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");

        assertThat(executeTool("generateMermaid", "b", 1, 1)).doesNotContain("TRUNCATED");
        assertThat(executeTool("generateMermaid", "b", 1, 1, 2)).contains("```mermaid").contains("TRUNCATED");
    }

    @Test
    void testGenerateMermaidClampsMaxNodes() {
        // focus view with more dependents than a diagram can hold
        for (int i = 1; i <= 120; i++) {
            analyzer.addView("view" + i, "SELECT * FROM base");
        }

        for (int maxNodes : new int[]{0, 500}) {
            String mermaid = executeTool("generateMermaid", "base", 0, 1, maxNodes);
            assertThat(mermaid).startsWith("```mermaid").contains("TRUNCATED: diagram shows only 100 views");
        }
    }

    @Test
    void testFullSchemaTooLargePointsToGenerateMermaid() {
        for (int i = 1; i <= 101; i++) {
            analyzer.addView("view" + i, "SELECT * FROM base");
        }

        assertThat(executor.generateFullSchemaDiagram()).contains("Use generateMermaid");
    }

    // Helper methods

    private String executeTool(String name, Object... values) {
        // the model's JSON goes through the SDK's tool runner, which builds the arguments with Jackson
        List<String> names = List.of("focusView", "depthUpstream", "depthDownstream", "maxNodes");
        Map<String, Object> arguments = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            arguments.put(names.get(i), values[i]);
        }
        ToolOutcome outcome = FakeMessageService.callTool(executor.tools(), name, arguments);
        assertThat(outcome.isError()).as(outcome.content()).isFalse();
        return outcome.content();
    }

}

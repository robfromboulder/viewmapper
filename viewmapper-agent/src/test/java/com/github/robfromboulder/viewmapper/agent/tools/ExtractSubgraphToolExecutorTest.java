// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.types.SubgraphResult;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ExtractSubgraphToolExecutor.
 */
class ExtractSubgraphToolExecutorTest {

    private DependencyAnalyzer analyzer;
    private ExtractSubgraphToolExecutor executor;

    @BeforeEach
    void setUp() {
        // build test schema: a -> b -> c -> d -> e
        analyzer = new DependencyAnalyzer();
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        analyzer.addView("d", "SELECT * FROM c");
        analyzer.addView("e", "SELECT * FROM d");
        executor = new ExtractSubgraphToolExecutor(analyzer);
    }

    @Test
    void testExtractWithCustomDepths() {
        SubgraphResult result = executor.extractSubgraph("c", 1, 1, 50);
        assertThat(result.getFocusView()).isEqualTo("c");
        assertThat(result.getViews()).containsExactlyInAnyOrder("c", "b", "d");
        assertThat(result.getDepthUpstream()).isEqualTo(1);
        assertThat(result.getDepthDownstream()).isEqualTo(1);
    }

    @Test
    void testExtractWithDefaultDepths() {
        SubgraphResult result = executor.extractSubgraph("c", 2, 1, 50);
        assertThat(result.getFocusView()).isEqualTo("c");
        assertThat(result.getViews()).contains("c", "b", "d");
        assertThat(result.getViews()).contains("a"); // 2 levels up
    }

    @Test
    void testExtractUpstreamOnly() {
        SubgraphResult result = executor.extractSubgraph("d", 2, 0, 50);
        assertThat(result.getViews()).containsExactlyInAnyOrder("d", "c", "b");
        assertThat(result.getViews()).doesNotContain("e");
    }

    @Test
    void testExtractDownstreamOnly() {
        SubgraphResult result = executor.extractSubgraph("b", 0, 2, 50);
        assertThat(result.getViews()).containsExactlyInAnyOrder("b", "c", "d");
        assertThat(result.getViews()).doesNotContain("a");
    }

    @Test
    void testMaxNodesTruncation() {
        SubgraphResult result = executor.extractSubgraph("c", 2, 2, 3);
        assertThat(result.getViewCount()).isEqualTo(3);
        assertThat(result.getViews()).contains("c"); // focus always included
        assertThat(result.isTruncated()).isTrue();
    }

    @Test
    void testNonExistentView() {
        assertThatThrownBy(() -> executor.extractSubgraph("nonexistent", 1, 1, 50)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not found");
    }

    @Test
    void testIsolatedView() {
        analyzer.addView("isolated", "SELECT * FROM external_table");
        SubgraphResult result = executor.extractSubgraph("isolated", 1, 1, 50);
        assertThat(result.getViews()).contains("isolated", "external_table"); // should contain isolated + external_table (dependency)
    }

    @Test
    void testUnlimitedNodes() {
        SubgraphResult result = executor.extractSubgraph("c", 3, 3, 0);
        assertThat(result.isTruncated()).isFalse(); // 0 means unlimited
    }

    @Test
    void testComplexSubgraph() {
        //     x
        //    / \
        //   y   z
        //    \ /
        //     w
        analyzer.addView("y", "SELECT * FROM x");
        analyzer.addView("z", "SELECT * FROM x");
        analyzer.addView("w", "SELECT * FROM y JOIN z ON y.id = z.id");
        SubgraphResult result = executor.extractSubgraph("w", 2, 0, 50);
        assertThat(result.getViews()).containsExactlyInAnyOrder("w", "y", "z", "x"); // should include w, y, z (1 level up), and x (2 levels up)
    }

}

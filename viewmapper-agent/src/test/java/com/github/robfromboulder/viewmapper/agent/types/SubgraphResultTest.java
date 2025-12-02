// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.types;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for SubgraphResult.
 */
class SubgraphResultTest {

    @Test
    void testSimpleConstruction() {
        Set<String> views = Set.of("view1", "view2", "view3");
        SubgraphResult result = new SubgraphResult(views, "view2");
        assertThat(result.getViews()).containsExactlyInAnyOrder("view1", "view2", "view3");
        assertThat(result.getFocusView()).isEqualTo("view2");
        assertThat(result.getViewCount()).isEqualTo(3);
        assertThat(result.isTruncated()).isFalse();
    }

    @Test
    void testFullConstruction() {
        Set<String> views = Set.of("a", "b", "c");
        SubgraphResult result = new SubgraphResult(views, "b", 2, 1, true);
        assertThat(result.getDepthUpstream()).isEqualTo(2);
        assertThat(result.getDepthDownstream()).isEqualTo(1);
        assertThat(result.isTruncated()).isTrue();
    }

    @Test
    void testContains() {
        Set<String> views = Set.of("view1", "view2");
        SubgraphResult result = new SubgraphResult(views, "view1");
        assertThat(result.contains("view1")).isTrue();
        assertThat(result.contains("view2")).isTrue();
        assertThat(result.contains("view3")).isFalse();
    }

    @Test
    void testVisualizationFeasible() {
        // small subgraph - feasible
        Set<String> small = Set.of("v1", "v2", "v3");
        SubgraphResult smallResult = new SubgraphResult(small, "v1");
        assertThat(smallResult.isVisualizationFeasible()).isTrue();

        // exactly 50 - still feasible
        Set<String> exactly50 = generateViews(50);
        SubgraphResult mediumResult = new SubgraphResult(exactly50, "v1");
        assertThat(mediumResult.isVisualizationFeasible()).isTrue();

        // 51 views - not feasible
        Set<String> large = generateViews(51);
        SubgraphResult largeResult = new SubgraphResult(large, "v1");
        assertThat(largeResult.isVisualizationFeasible()).isFalse();
    }

    @Test
    void testViewSetIsImmutable() {
        Set<String> views = Set.of("view1", "view2");
        SubgraphResult result = new SubgraphResult(views, "view1");
        assertThatThrownBy(() -> result.getViews().add("view3")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testToString() {
        Set<String> views = Set.of("a", "b", "c");
        SubgraphResult result = new SubgraphResult(views, "b", 2, 1, false);
        String str = result.toString();
        assertThat(str).contains("focus='b'");
        assertThat(str).contains("views=3");
        assertThat(str).contains("upstream=2");
        assertThat(str).contains("downstream=1");
        assertThat(str).contains("truncated=false");
    }

    @Test
    void testEquality() {
        Set<String> views1 = Set.of("a", "b");
        Set<String> views2 = Set.of("a", "b");

        SubgraphResult r1 = new SubgraphResult(views1, "a", 1, 1, false);
        SubgraphResult r2 = new SubgraphResult(views2, "a", 1, 1, false);
        SubgraphResult r3 = new SubgraphResult(views1, "b", 1, 1, false);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1).isNotEqualTo(r3);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void testNullViews() {
        assertThatThrownBy(() -> new SubgraphResult(null, "focus")).isInstanceOf(NullPointerException.class).hasMessageContaining("Views");
    }

    @Test
    void testNullFocusView() {
        assertThatThrownBy(() -> new SubgraphResult(Set.of("a"), null)).isInstanceOf(NullPointerException.class).hasMessageContaining("Focus view");
    }

    private Set<String> generateViews(int count) {
        Set<String> views = new java.util.HashSet<>();
        for (int i = 1; i <= count; i++) views.add("v" + i);
        return views;
    }

}

// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.types.EntryPointSuggestion;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for SuggestEntryPointsToolExecutor.
 */
class SuggestEntryPointsToolExecutorTest {

    private SuggestEntryPointsToolExecutor executor;

    @BeforeEach
    void setUp() {
        // build test schema:
        // base -> mid1 -> leaf1
        //      -> mid2 -> leaf2
        //      -> mid3 -> leaf3
        DependencyAnalyzer analyzer = new DependencyAnalyzer();
        analyzer.addView("mid1", "SELECT * FROM base");
        analyzer.addView("mid2", "SELECT * FROM base");
        analyzer.addView("mid3", "SELECT * FROM base");
        analyzer.addView("leaf1", "SELECT * FROM mid1");
        analyzer.addView("leaf2", "SELECT * FROM mid2");
        analyzer.addView("leaf3", "SELECT * FROM mid3");
        executor = new SuggestEntryPointsToolExecutor(analyzer);
    }

    @Test
    void testSuggestHighImpact() {
        List<EntryPointSuggestion> suggestions = executor.suggestEntryPoints("high-impact", 5);
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getViewName()).isEqualTo("base");
        assertThat(suggestions.get(0).getScore()).isEqualTo(3.0);
        assertThat(suggestions.get(0).getType()).isEqualTo(EntryPointSuggestion.EntryPointType.HIGH_IMPACT);
    }

    @Test
    void testSuggestLeafViews() {
        List<EntryPointSuggestion> suggestions = executor.suggestEntryPoints("leaf-views", 5);
        assertThat(suggestions).hasSize(3);
        assertThat(suggestions).extracting(EntryPointSuggestion::getViewName).containsExactlyInAnyOrder("leaf1", "leaf2", "leaf3");
        assertThat(suggestions.get(0).getType()).isEqualTo(EntryPointSuggestion.EntryPointType.LEAF_VIEW);
    }

    @Test
    void testSuggestCentralHubs() {
        List<EntryPointSuggestion> suggestions = executor.suggestEntryPoints("central-hubs", 5);
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getType()).isEqualTo(EntryPointSuggestion.EntryPointType.CENTRAL_HUB);
    }

    @Test
    void testSuggestWithDefaultLimit() {
        List<EntryPointSuggestion> suggestions = executor.suggestEntryPoints("high-impact", 5);
        assertThat(suggestions).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    void testSuggestWithCustomLimit() {
        List<EntryPointSuggestion> suggestions = executor.suggestEntryPoints("leaf-views", 2);
        assertThat(suggestions).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void testInvalidStrategy() {
        assertThatThrownBy(() -> executor.suggestEntryPoints("invalid-strategy", 5)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown strategy");
    }

    @Test
    void testCaseInsensitiveStrategy() {
        List<EntryPointSuggestion> upper = executor.suggestEntryPoints("HIGH-IMPACT", 5);
        List<EntryPointSuggestion> lower = executor.suggestEntryPoints("high-impact", 5);
        assertThat(upper).hasSameSizeAs(lower);
    }

    @Test
    void testEmptySchema() {
        DependencyAnalyzer emptyAnalyzer = new DependencyAnalyzer();
        SuggestEntryPointsToolExecutor emptyExecutor = new SuggestEntryPointsToolExecutor(emptyAnalyzer);
        List<EntryPointSuggestion> suggestions = emptyExecutor.suggestEntryPoints("high-impact", 5);
        assertThat(suggestions).isEmpty();
    }

}

// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.types;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for EntryPointSuggestion.
 */
class EntryPointSuggestionTest {

    @Test
    void testHighImpactFactory() {
        EntryPointSuggestion suggestion = EntryPointSuggestion.highImpact("customer_360", 15);
        assertThat(suggestion.getViewName()).isEqualTo("customer_360");
        assertThat(suggestion.getScore()).isEqualTo(15.0);
        assertThat(suggestion.getReason()).contains("15 views depend");
        assertThat(suggestion.getType()).isEqualTo(EntryPointSuggestion.EntryPointType.HIGH_IMPACT);
    }

    @Test
    void testLeafViewFactory() {
        EntryPointSuggestion suggestion = EntryPointSuggestion.leafView("final_report");
        assertThat(suggestion.getViewName()).isEqualTo("final_report");
        assertThat(suggestion.getScore()).isEqualTo(0.0);
        assertThat(suggestion.getReason()).contains("Final output");
        assertThat(suggestion.getType()).isEqualTo(EntryPointSuggestion.EntryPointType.LEAF_VIEW);
    }

    @Test
    void testCentralHubFactory() {
        EntryPointSuggestion suggestion = EntryPointSuggestion.centralHub("integration_view", 42.5);
        assertThat(suggestion.getViewName()).isEqualTo("integration_view");
        assertThat(suggestion.getScore()).isEqualTo(42.5);
        assertThat(suggestion.getReason()).contains("Central hub");
        assertThat(suggestion.getReason()).contains("42.5");
        assertThat(suggestion.getType()).isEqualTo(EntryPointSuggestion.EntryPointType.CENTRAL_HUB);
    }

    @Test
    void testComparable() {
        EntryPointSuggestion high = EntryPointSuggestion.highImpact("view1", 100);
        EntryPointSuggestion medium = EntryPointSuggestion.highImpact("view2", 50);
        EntryPointSuggestion low = EntryPointSuggestion.highImpact("view3", 10);

        List<EntryPointSuggestion> suggestions = new ArrayList<>();
        suggestions.add(medium);
        suggestions.add(low);
        suggestions.add(high);

        // should be sorted descending by score
        Collections.sort(suggestions);
        assertThat(suggestions.get(0)).isEqualTo(high);
        assertThat(suggestions.get(1)).isEqualTo(medium);
        assertThat(suggestions.get(2)).isEqualTo(low);
    }

    @Test
    void testToString() {
        EntryPointSuggestion suggestion = EntryPointSuggestion.highImpact("test_view", 5);
        String str = suggestion.toString();
        assertThat(str).contains("test_view");
        assertThat(str).contains("5");
    }

    @Test
    void testEquality() {
        EntryPointSuggestion s1 = EntryPointSuggestion.highImpact("view", 10);
        EntryPointSuggestion s2 = EntryPointSuggestion.highImpact("view", 10);
        EntryPointSuggestion s3 = EntryPointSuggestion.highImpact("view", 11);
        assertThat(s1).isEqualTo(s2);
        assertThat(s1).isNotEqualTo(s3);
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
    }

    @Test
    void testTypeDescriptions() {
        assertThat(EntryPointSuggestion.EntryPointType.HIGH_IMPACT.getDescription()).contains("Many views depend");
        assertThat(EntryPointSuggestion.EntryPointType.LEAF_VIEW.getDescription()).contains("Final output");
        assertThat(EntryPointSuggestion.EntryPointType.CENTRAL_HUB.getDescription()).contains("Connects");
    }

}

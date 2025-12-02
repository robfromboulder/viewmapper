// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.types;

import java.util.Objects;

/**
 * Represents a suggested entry point for schema exploration.
 * <p>
 * Contains the view name, a score indicating importance/relevance, and a human-readable reason explaining why this is a good entry point.
 */
public class EntryPointSuggestion implements Comparable<EntryPointSuggestion> {

    /**
     * Enum for all the entry points available to suggest.
     */
    public enum EntryPointType {
        HIGH_IMPACT("High Impact - Many views depend on this"),
        LEAF_VIEW("Leaf View - Final output/report"),
        CENTRAL_HUB("Central Hub - Connects many sources to consumers");

        private final String description;

        EntryPointType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Factory method for central hubs.
     */
    public static EntryPointSuggestion centralHub(String viewName, double centralityScore) {
        return new EntryPointSuggestion(viewName, centralityScore, String.format("Central hub (centrality: %.2f) connecting sources to consumers", centralityScore), EntryPointType.CENTRAL_HUB);
    }

    /**
     * Factory method for high-impact views.
     */
    public static EntryPointSuggestion highImpact(String viewName, int dependentCount) {
        return new EntryPointSuggestion(viewName, dependentCount, String.format("%d views depend on this (foundational/core view)", dependentCount), EntryPointType.HIGH_IMPACT);
    }

    /**
     * Factory method for leaf views.
     */
    public static EntryPointSuggestion leafView(String viewName) {
        return new EntryPointSuggestion(viewName, 0.0, "Final output/report with no dependents", EntryPointType.LEAF_VIEW);
    }

    private final String viewName;
    private final double score;
    private final String reason;
    private final EntryPointType type;

    public EntryPointSuggestion(String viewName, double score, String reason, EntryPointType type) {
        this.viewName = Objects.requireNonNull(viewName, "View name cannot be null");
        this.score = score;
        this.reason = Objects.requireNonNull(reason, "Reason cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
    }

    public String getReason() {
        return reason;
    }

    public double getScore() {
        return score;
    }

    public EntryPointType getType() {
        return type;
    }

    public String getViewName() {
        return viewName;
    }

    @Override
    public int compareTo(EntryPointSuggestion other) {
        return Double.compare(other.score, this.score); // higher scores first
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntryPointSuggestion that = (EntryPointSuggestion) o;
        return Double.compare(that.score, score) == 0 && Objects.equals(viewName, that.viewName) && Objects.equals(reason, that.reason) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(viewName, score, reason, type);
    }

    @Override
    public String toString() {
        return String.format("%s - %s (score: %.2f)", viewName, reason, score);
    }

}

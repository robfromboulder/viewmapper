// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.types;

/**
 * Represents the complexity level of a database schema based on view count.
 * <p>
 * Used to determine the appropriate exploration strategy:
 * - SIMPLE: Full diagram feasible
 * - MODERATE: Suggest grouping or multiple diagrams
 * - COMPLEX: Require focused exploration
 * - VERY_COMPLEX: Guided entry point selection mandatory
 */
public enum ComplexityLevel {

    /**
     * Less than 20 views - can show full dependency graph.
     */
    SIMPLE(0, 19, "Full diagram feasible"),

    /**
     * 20-100 views - suggest grouping or iterative exploration.
     */
    MODERATE(20, 99, "Suggest grouping by domain or iterative exploration"),

    /**
     * 100-500 views - require focus point before visualization.
     */
    COMPLEX(100, 499, "Require focused exploration with entry point"),

    /**
     * 500+ views - guided exploration with multiple iterations required.
     */
    VERY_COMPLEX(500, Integer.MAX_VALUE, "Guided exploration required");

    /**
     * Determines complexity level based on view count.
     *
     * @param viewCount Number of views in the schema
     * @return Appropriate complexity level
     */
    public static ComplexityLevel fromViewCount(int viewCount) {
        for (ComplexityLevel level : values()) {
            if (viewCount >= level.minViews && viewCount <= level.maxViews) {
                return level;
            }
        }
        return VERY_COMPLEX;
    }

    private final String guidance;
    private final int minViews;
    private final int maxViews;

    ComplexityLevel(int minViews, int maxViews, String guidance) {
        this.minViews = minViews;
        this.maxViews = maxViews;
        this.guidance = guidance;
    }

    public String getGuidance() {
        return guidance;
    }

    public int getMinViews() {
        return minViews;
    }

    public int getMaxViews() {
        return maxViews;
    }

    /**
     * Returns true if full diagram visualization is feasible.
     */
    public boolean isFullDiagramFeasible() {
        return this == SIMPLE;
    }

    /**
     * Returns true if entry point selection is required.
     */
    public boolean requiresEntryPoint() {
        return this == COMPLEX || this == VERY_COMPLEX;
    }

}

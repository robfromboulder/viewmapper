// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.types;

import java.util.Objects;

/**
 * Result of schema complexity analysis.
 * <p>
 * Provides information about schema size and recommended exploration strategy.
 */
public class SchemaComplexity {

    /**
     * Factory method to create with level calculated from view count.
     */
    public static SchemaComplexity fromViewCount(String schemaName, int viewCount) {
        return new SchemaComplexity(schemaName, viewCount, ComplexityLevel.fromViewCount(viewCount));
    }

    private final ComplexityLevel level;
    private final String schemaName;
    private final int viewCount;

    /**
     * Default constructor.
     */
    public SchemaComplexity(String schemaName, int viewCount, ComplexityLevel level) {
        this.schemaName = Objects.requireNonNull(schemaName, "Schema name cannot be null");
        this.viewCount = viewCount;
        this.level = Objects.requireNonNull(level, "Complexity level cannot be null");
    }

    public String getGuidance() {
        return level.getGuidance();
    }

    public ComplexityLevel getLevel() {
        return level;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public int getViewCount() {
        return viewCount;
    }

    public boolean isFullDiagramFeasible() {
        return level.isFullDiagramFeasible();
    }

    public boolean requiresEntryPoint() {
        return level.requiresEntryPoint();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaComplexity that = (SchemaComplexity) o;
        return viewCount == that.viewCount && Objects.equals(schemaName, that.schemaName) && level == that.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaName, viewCount, level);
    }

    @Override
    public String toString() {
        return String.format("SchemaComplexity{schema='%s', views=%d, level=%s}", schemaName, viewCount, level);
    }

}

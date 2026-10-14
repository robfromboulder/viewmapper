// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.anthropic.helpers.BetaRunnableTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.robfromboulder.viewmapper.agent.types.ComplexityLevel;
import com.github.robfromboulder.viewmapper.agent.types.SchemaComplexity;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import java.util.List;
import java.util.Objects;

/**
 * Tool for analyzing schema complexity.
 * <p>
 * Counts views in the schema and assesses complexity level to guide exploration strategy.
 */
public class AnalyzeSchemaToolExecutor {

    /**
     * Arguments for analyzeSchema.
     */
    @JsonTypeName("analyzeSchema")
    @JsonClassDescription("Analyzes schema to count views and assess complexity level. Use this FIRST to understand schema size.")
    public static class Arguments {
        @JsonPropertyDescription("Name of the schema to analyze")
        public String schemaName;
    }

    private final DependencyAnalyzer analyzer;

    /**
     * Default constructor.
     */
    public AnalyzeSchemaToolExecutor(DependencyAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "Analyzer cannot be null");
    }

    /**
     * Analyzes the schema to count views and assess complexity.
     * <p>
     * Use this tool FIRST to understand schema size before suggesting exploration strategies.
     *
     * @param schemaName The name of the schema to analyze
     * @return Complexity analysis with view count and recommended strategy
     */
    public SchemaComplexity analyzeSchema(String schemaName) {
        int viewCount = analyzer.getViewCount();
        ComplexityLevel level = ComplexityLevel.fromViewCount(viewCount);
        return new SchemaComplexity(schemaName, viewCount, level);
    }

    /**
     * Returns the tools this executor provides to the model.
     */
    public List<BetaRunnableTool> tools() {
        return List.of(BetaRunnableTool.of(Arguments.class, a -> RunnableTools.result(analyzeSchema(a.schemaName))));
    }

}

// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.types.ComplexityLevel;
import com.github.robfromboulder.viewmapper.agent.types.SchemaComplexity;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import dev.langchain4j.agent.tool.Tool;

import java.util.Objects;

/**
 * Tool for analyzing schema complexity.
 * <p>
 * Counts views in the schema and assesses complexity level to guide exploration strategy.
 */
public class AnalyzeSchemaToolExecutor {

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
    @Tool("Analyzes schema to count views and assess complexity level. Use this FIRST to understand schema size.")
    public SchemaComplexity analyzeSchema(String schemaName) {
        int viewCount = analyzer.getViewCount();
        ComplexityLevel level = ComplexityLevel.fromViewCount(viewCount);
        return new SchemaComplexity(schemaName, viewCount, level);
    }

}

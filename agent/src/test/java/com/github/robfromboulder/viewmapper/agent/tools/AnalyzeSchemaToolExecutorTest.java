// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.types.ComplexityLevel;
import com.github.robfromboulder.viewmapper.agent.types.SchemaComplexity;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AnalyzeSchemaToolExecutor.
 */
class AnalyzeSchemaToolExecutorTest {

    private DependencyAnalyzer analyzer;
    private AnalyzeSchemaToolExecutor executor;

    @BeforeEach
    void setUp() {
        analyzer = new DependencyAnalyzer();
        executor = new AnalyzeSchemaToolExecutor(analyzer);
    }

    @Test
    void testAnalyzeSimpleSchema() {
        for (int i = 1; i <= 10; i++) analyzer.addView("view" + i, "SELECT * FROM base");
        SchemaComplexity result = executor.analyzeSchema("test_schema");
        assertThat(result.getSchemaName()).isEqualTo("test_schema");
        assertThat(result.getViewCount()).isEqualTo(11); // graph also includes "base" as a node, so 10 views + 1 base = 11
        assertThat(result.getLevel()).isEqualTo(ComplexityLevel.SIMPLE);
        assertThat(result.isFullDiagramFeasible()).isTrue();
    }

    @Test
    void testAnalyzeModerateSchema() {
        for (int i = 1; i <= 50; i++) analyzer.addView("view" + i, "SELECT * FROM base");
        SchemaComplexity result = executor.analyzeSchema("moderate_schema");
        assertThat(result.getViewCount()).isEqualTo(51); // 50 views + base
        assertThat(result.getLevel()).isEqualTo(ComplexityLevel.MODERATE);
        assertThat(result.isFullDiagramFeasible()).isFalse();
        assertThat(result.requiresEntryPoint()).isFalse();
    }

    @Test
    void testAnalyzeComplexSchema() {
        for (int i = 1; i <= 200; i++) analyzer.addView("view" + i, "SELECT * FROM base");
        SchemaComplexity result = executor.analyzeSchema("complex_schema");
        assertThat(result.getViewCount()).isEqualTo(201); // 200 views + base
        assertThat(result.getLevel()).isEqualTo(ComplexityLevel.COMPLEX);
        assertThat(result.requiresEntryPoint()).isTrue();
    }

    @Test
    void testAnalyzeEmptySchema() {
        SchemaComplexity result = executor.analyzeSchema("empty");
        assertThat(result.getViewCount()).isEqualTo(0);
        assertThat(result.getLevel()).isEqualTo(ComplexityLevel.SIMPLE);
    }

    @Test
    void testAnalyzeVeryComplexSchema() {
        for (int i = 1; i <= 600; i++) analyzer.addView("view" + i, "SELECT * FROM base");
        SchemaComplexity result = executor.analyzeSchema("very_complex");
        assertThat(result.getViewCount()).isEqualTo(601); // 600 views + base
        assertThat(result.getLevel()).isEqualTo(ComplexityLevel.VERY_COMPLEX);
    }

}

// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.types;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for SchemaComplexity.
 */
class SchemaComplexityTest {

    @Test
    void testFromViewCountSimple() {
        SchemaComplexity complexity = SchemaComplexity.fromViewCount("test_schema", 10);
        assertThat(complexity.getSchemaName()).isEqualTo("test_schema");
        assertThat(complexity.getViewCount()).isEqualTo(10);
        assertThat(complexity.getLevel()).isEqualTo(ComplexityLevel.SIMPLE);
        assertThat(complexity.isFullDiagramFeasible()).isTrue();
        assertThat(complexity.requiresEntryPoint()).isFalse();
    }

    @Test
    void testFromViewCountComplex() {
        SchemaComplexity complexity = SchemaComplexity.fromViewCount("large_schema", 250);
        assertThat(complexity.getSchemaName()).isEqualTo("large_schema");
        assertThat(complexity.getViewCount()).isEqualTo(250);
        assertThat(complexity.getLevel()).isEqualTo(ComplexityLevel.COMPLEX);
        assertThat(complexity.isFullDiagramFeasible()).isFalse();
        assertThat(complexity.requiresEntryPoint()).isTrue();
    }

    @Test
    void testManualConstruction() {
        SchemaComplexity complexity = new SchemaComplexity("custom", 50, ComplexityLevel.MODERATE);
        assertThat(complexity.getViewCount()).isEqualTo(50);
        assertThat(complexity.getLevel()).isEqualTo(ComplexityLevel.MODERATE);
    }

    @Test
    void testGuidance() {
        SchemaComplexity simple = SchemaComplexity.fromViewCount("s1", 5);
        assertThat(simple.getGuidance()).contains("Full diagram feasible");
        SchemaComplexity complex = SchemaComplexity.fromViewCount("s2", 200);
        assertThat(complex.getGuidance()).contains("focused exploration");
    }

    @Test
    void testToString() {
        SchemaComplexity complexity = SchemaComplexity.fromViewCount("my_schema", 100);
        String str = complexity.toString();
        assertThat(str).contains("my_schema");
        assertThat(str).contains("100");
        assertThat(str).contains("COMPLEX");
    }

    @Test
    void testEquality() {
        SchemaComplexity c1 = new SchemaComplexity("test", 50, ComplexityLevel.MODERATE);
        SchemaComplexity c2 = new SchemaComplexity("test", 50, ComplexityLevel.MODERATE);
        SchemaComplexity c3 = new SchemaComplexity("test", 51, ComplexityLevel.MODERATE);
        assertThat(c1).isEqualTo(c2);
        assertThat(c1).isNotEqualTo(c3);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }

    @Test
    void testNullSchemaName() {
        assertThatThrownBy(() -> new SchemaComplexity(null, 10, ComplexityLevel.SIMPLE)).isInstanceOf(NullPointerException.class).hasMessageContaining("Schema name");
    }

    @Test
    void testNullComplexityLevel() {
        assertThatThrownBy(() -> new SchemaComplexity("test", 10, null)).isInstanceOf(NullPointerException.class).hasMessageContaining("Complexity level");
    }

}

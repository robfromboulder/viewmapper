// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.types;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ComplexityLevel enum.
 */
class ComplexityLevelTest {

    @Test
    void testFromViewCountSimple() {
        assertThat(ComplexityLevel.fromViewCount(0)).isEqualTo(ComplexityLevel.SIMPLE);
        assertThat(ComplexityLevel.fromViewCount(10)).isEqualTo(ComplexityLevel.SIMPLE);
        assertThat(ComplexityLevel.fromViewCount(19)).isEqualTo(ComplexityLevel.SIMPLE);
    }

    @Test
    void testFromViewCountModerate() {
        assertThat(ComplexityLevel.fromViewCount(20)).isEqualTo(ComplexityLevel.MODERATE);
        assertThat(ComplexityLevel.fromViewCount(50)).isEqualTo(ComplexityLevel.MODERATE);
        assertThat(ComplexityLevel.fromViewCount(99)).isEqualTo(ComplexityLevel.MODERATE);
    }

    @Test
    void testFromViewCountComplex() {
        assertThat(ComplexityLevel.fromViewCount(100)).isEqualTo(ComplexityLevel.COMPLEX);
        assertThat(ComplexityLevel.fromViewCount(250)).isEqualTo(ComplexityLevel.COMPLEX);
        assertThat(ComplexityLevel.fromViewCount(499)).isEqualTo(ComplexityLevel.COMPLEX);
    }

    @Test
    void testFromViewCountVeryComplex() {
        assertThat(ComplexityLevel.fromViewCount(500)).isEqualTo(ComplexityLevel.VERY_COMPLEX);
        assertThat(ComplexityLevel.fromViewCount(1000)).isEqualTo(ComplexityLevel.VERY_COMPLEX);
        assertThat(ComplexityLevel.fromViewCount(10000)).isEqualTo(ComplexityLevel.VERY_COMPLEX);
    }

    @Test
    void testBoundaryValues() {
        assertThat(ComplexityLevel.fromViewCount(19)).isEqualTo(ComplexityLevel.SIMPLE);
        assertThat(ComplexityLevel.fromViewCount(20)).isEqualTo(ComplexityLevel.MODERATE);
        assertThat(ComplexityLevel.fromViewCount(99)).isEqualTo(ComplexityLevel.MODERATE);
        assertThat(ComplexityLevel.fromViewCount(100)).isEqualTo(ComplexityLevel.COMPLEX);
        assertThat(ComplexityLevel.fromViewCount(499)).isEqualTo(ComplexityLevel.COMPLEX);
        assertThat(ComplexityLevel.fromViewCount(500)).isEqualTo(ComplexityLevel.VERY_COMPLEX);
    }

    @Test
    void testIsFullDiagramFeasible() {
        assertThat(ComplexityLevel.SIMPLE.isFullDiagramFeasible()).isTrue();
        assertThat(ComplexityLevel.MODERATE.isFullDiagramFeasible()).isFalse();
        assertThat(ComplexityLevel.COMPLEX.isFullDiagramFeasible()).isFalse();
        assertThat(ComplexityLevel.VERY_COMPLEX.isFullDiagramFeasible()).isFalse();
    }

    @Test
    void testRequiresEntryPoint() {
        assertThat(ComplexityLevel.SIMPLE.requiresEntryPoint()).isFalse();
        assertThat(ComplexityLevel.MODERATE.requiresEntryPoint()).isFalse();
        assertThat(ComplexityLevel.COMPLEX.requiresEntryPoint()).isTrue();
        assertThat(ComplexityLevel.VERY_COMPLEX.requiresEntryPoint()).isTrue();
    }

    @Test
    void testGuidanceMessages() {
        assertThat(ComplexityLevel.SIMPLE.getGuidance()).contains("Full diagram feasible");
        assertThat(ComplexityLevel.MODERATE.getGuidance()).contains("grouping");
        assertThat(ComplexityLevel.COMPLEX.getGuidance()).contains("focused exploration");
        assertThat(ComplexityLevel.VERY_COMPLEX.getGuidance()).contains("Guided exploration");
    }

    @Test
    void testMinMaxViews() {
        assertThat(ComplexityLevel.SIMPLE.getMinViews()).isEqualTo(0);
        assertThat(ComplexityLevel.SIMPLE.getMaxViews()).isEqualTo(19);
        assertThat(ComplexityLevel.MODERATE.getMinViews()).isEqualTo(20);
        assertThat(ComplexityLevel.MODERATE.getMaxViews()).isEqualTo(99);
        assertThat(ComplexityLevel.COMPLEX.getMinViews()).isEqualTo(100);
        assertThat(ComplexityLevel.COMPLEX.getMaxViews()).isEqualTo(499);
        assertThat(ComplexityLevel.VERY_COMPLEX.getMinViews()).isEqualTo(500);
        assertThat(ComplexityLevel.VERY_COMPLEX.getMaxViews()).isEqualTo(Integer.MAX_VALUE);
    }

}

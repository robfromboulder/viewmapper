// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.discovery.TestDatasetDiscoveryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ListSchemasToolExecutor.
 */
class ListSchemasToolExecutorTest {

    private ListSchemasToolExecutor executor;

    @BeforeEach
    void setUp() {
        TestDatasetDiscoveryProvider provider = new TestDatasetDiscoveryProvider("simple_ecommerce");
        executor = new ListSchemasToolExecutor(provider);
    }

    @Test
    void testListSchemasWithTestCatalog() {
        List<String> schemas = executor.listSchemas("test");
        assertThat(schemas).containsExactly("simple_ecommerce");
    }

    @Test
    void testListSchemasReturnsNonNullList() {
        List<String> schemas = executor.listSchemas("test");
        assertThat(schemas).isNotNull();
    }

    @Test
    void testListSchemasWithInvalidCatalogThrowsException() {
        assertThatThrownBy(() -> executor.listSchemas("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown catalog");
    }

}

// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.discovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for TestDatasetDiscoveryProvider.
 */
class TestDatasetDiscoveryProviderTest {

    private TestDatasetDiscoveryProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TestDatasetDiscoveryProvider();
    }

    @Test
    void testListCatalogsReturnsTestCatalog() {
        List<String> catalogs = provider.listCatalogs();
        assertThat(catalogs).containsExactly("test");
    }

    @Test
    void testListSchemasReturnsAllDatasets() {
        List<String> schemas = provider.listSchemas("test");
        assertThat(schemas).containsExactlyInAnyOrder(
                "simple_ecommerce",
                "moderate_analytics",
                "complex_enterprise",
                "realistic_bi_warehouse"
        );
    }

    @Test
    void testListSchemasWithInvalidCatalogThrowsException() {
        assertThatThrownBy(() -> provider.listSchemas("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown catalog 'invalid'")
                .hasMessageContaining("Test datasets use catalog 'test'");
    }

    @Test
    void testListSchemasWithNullCatalogThrowsException() {
        assertThatThrownBy(() -> provider.listSchemas(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Catalog parameter required")
                .hasMessageContaining("Test datasets use catalog 'test'");
    }

    @Test
    void testListSchemasWithEmptyCatalogThrowsException() {
        assertThatThrownBy(() -> provider.listSchemas(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Catalog parameter required");
    }

}

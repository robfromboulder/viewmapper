// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.discovery;

import java.util.ArrayList;
import java.util.List;

/**
 * Test dataset implementation of DiscoveryProvider.
 * <p>
 * Provides a synthetic catalog/schema structure for embedded test datasets,
 * ensuring discovery workflows are identical to JDBC connections. This enables
 * testing and demos without requiring a live Trino instance.
 * <p>
 * <strong>Virtual Catalog Structure:</strong>
 * <ul>
 *   <li>Catalog: {@code "test"}</li>
 *   <li>Schemas: {@code simple_ecommerce}, {@code moderate_analytics},
 *       {@code complex_enterprise}, {@code realistic_bi_warehouse}</li>
 * </ul>
 * <p>
 * <strong>Example Usage:</strong>
 * <pre>{@code
 * DiscoveryProvider provider = new TestDatasetDiscoveryProvider();
 *
 * List<String> catalogs = provider.listCatalogs();
 * // Returns: ["test"]
 *
 * List<String> schemas = provider.listSchemas("test");
 * // Returns: ["simple_ecommerce", "moderate_analytics", "complex_enterprise", "realistic_bi_warehouse"]
 *
 * // User can then analyze: test.simple_ecommerce
 * }</pre>
 * <p>
 * This design makes test dataset exploration identical to JDBC discovery,
 * so users learn one workflow that applies to both test and production environments.
 */
public class TestDatasetDiscoveryProvider implements DiscoveryProvider {

    /**
     * Synthetic catalog name for test datasets.
     */
    private static final String TEST_CATALOG = "test";

    /**
     * Available test datasets, presented as schemas within the "test" catalog.
     */
    private static final List<String> TEST_DATASETS = List.of(
            "simple_ecommerce",
            "moderate_analytics",
            "complex_enterprise",
            "realistic_bi_warehouse"
    );

    /**
     * Lists the synthetic catalog containing test datasets.
     * <p>
     * Always returns {@code ["test"]}.
     *
     * @return List containing the single synthetic catalog "test"
     */
    @Override
    public List<String> listCatalogs() {
        return List.of(TEST_CATALOG);
    }

    /**
     * Lists all test datasets as schemas within the "test" catalog.
     * <p>
     * Returns the four embedded test datasets:
     * <ul>
     *   <li>{@code simple_ecommerce} - 11 views (SIMPLE)</li>
     *   <li>{@code moderate_analytics} - 35 views (MODERATE)</li>
     *   <li>{@code complex_enterprise} - 154 views (COMPLEX)</li>
     *   <li>{@code realistic_bi_warehouse} - 86 views (COMPLEX)</li>
     * </ul>
     *
     * @param catalog Catalog name (must be "test")
     * @return List of test dataset names
     * @throws IllegalArgumentException if catalog is not "test"
     */
    @Override
    public List<String> listSchemas(String catalog) {
        if (catalog == null || catalog.isEmpty()) throw new IllegalArgumentException("Catalog parameter required. Test datasets use catalog 'test'");
        if (!TEST_CATALOG.equals(catalog)) throw new IllegalArgumentException("Unknown catalog '" + catalog + "'. Test datasets use catalog 'test'");
        return new ArrayList<>(TEST_DATASETS);
    }

}

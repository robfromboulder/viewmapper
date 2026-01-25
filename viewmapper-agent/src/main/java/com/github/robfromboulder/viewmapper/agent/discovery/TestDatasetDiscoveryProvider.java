// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.discovery;

import java.util.List;

/**
 * Test dataset implementation of DiscoveryProvider.
 * <p>
 * Provides a synthetic catalog/schema structure for the loaded test dataset,
 * enabling discovery workflows identical to JDBC connections. This enables
 * testing and demos without requiring a live Trino instance.
 * <p>
 * <strong>Virtual Catalog Structure:</strong>
 * <ul>
 *   <li>Catalog: {@code "test"}</li>
 *   <li>Schema: The loaded dataset name (e.g., {@code simple_ecommerce})</li>
 * </ul>
 * <p>
 * <strong>Example Usage:</strong>
 * <pre>{@code
 * DiscoveryProvider provider = new TestDatasetDiscoveryProvider("simple_ecommerce");
 *
 * List<String> catalogs = provider.listCatalogs();
 * // Returns: ["test"]
 *
 * List<String> schemas = provider.listSchemas("test");
 * // Returns: ["simple_ecommerce"]
 *
 * // Agent sees one schema, proceeds directly to: analyzeSchema("test.simple_ecommerce")
 * }</pre>
 * <p>
 * By returning only the loaded dataset, the agent knows exactly which schema
 * to analyze without requiring an explicit {@code --schema} parameter.
 */
public class TestDatasetDiscoveryProvider implements DiscoveryProvider {

    /**
     * Synthetic catalog name for test datasets.
     */
    private static final String TEST_CATALOG = "test";

    /**
     * The loaded dataset name, presented as the only schema.
     */
    private final String loadedDataset;

    /**
     * Creates a provider for the specified loaded dataset.
     *
     * @param loadedDataset The dataset name that was loaded (e.g., "simple_ecommerce")
     */
    public TestDatasetDiscoveryProvider(String loadedDataset) {
        if (loadedDataset == null || loadedDataset.isEmpty()) throw new IllegalArgumentException("Loaded dataset name required");
        this.loadedDataset = loadedDataset;
    }

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
     * Lists the loaded dataset as the only schema within the "test" catalog.
     * <p>
     * Returns only the dataset that was specified when this provider was created,
     * allowing the agent to proceed directly to analysis without ambiguity.
     *
     * @param catalog Catalog name (must be "test")
     * @return List containing only the loaded dataset name
     * @throws IllegalArgumentException if catalog is not "test"
     */
    @Override
    public List<String> listSchemas(String catalog) {
        if (catalog == null || catalog.isEmpty()) throw new IllegalArgumentException("Catalog parameter required. Test datasets use catalog 'test'");
        if (!TEST_CATALOG.equals(catalog)) throw new IllegalArgumentException("Unknown catalog '" + catalog + "'. Test datasets use catalog 'test'");
        return List.of(loadedDataset);
    }

}

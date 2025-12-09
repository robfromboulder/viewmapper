// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.discovery;

import java.util.List;

/**
 * Interface for discovering catalogs and schemas in Trino instances.
 * <p>
 * Provides a unified abstraction for both JDBC-based discovery (live Trino)
 * and test dataset discovery (embedded test data), ensuring consistent UX
 * across different connection modes.
 * <p>
 * <strong>Example Usage:</strong>
 * <pre>{@code
 * // For JDBC connections
 * DiscoveryProvider provider = new JdbcDiscoveryProvider(jdbcUrl);
 * List<String> catalogs = provider.listCatalogs();
 * List<String> schemas = provider.listSchemas("viewzoo");
 *
 * // For test datasets
 * DiscoveryProvider provider = new TestDatasetDiscoveryProvider();
 * List<String> catalogs = provider.listCatalogs();  // Returns ["test"]
 * List<String> schemas = provider.listSchemas("test");  // Returns dataset names
 * }</pre>
 */
public interface DiscoveryProvider {

    /**
     * Lists all available catalogs.
     * <p>
     * For JDBC: Executes {@code SHOW CATALOGS} against live Trino instance.
     * <p>
     * For test datasets: Returns synthetic catalog(s) containing test data.
     *
     * @return List of catalog names, never null
     */
    List<String> listCatalogs();

    /**
     * Lists all schemas in the specified catalog.
     * <p>
     * For JDBC: Executes {@code SHOW SCHEMAS FROM catalog} against live Trino.
     * Validates catalog parameter against bound catalog if connection is
     * catalog-bound.
     * <p>
     * For test datasets: Returns dataset names if catalog matches synthetic
     * catalog name ("test").
     *
     * @param catalog Catalog name (required for multi-catalog connections,
     *                optional for single-catalog connections where it may be null)
     * @return List of schema names in the specified catalog, never null
     * @throws IllegalArgumentException if catalog is invalid or required but not provided
     */
    List<String> listSchemas(String catalog);

}

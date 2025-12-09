// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.discovery.DiscoveryProvider;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;
import java.util.Objects;

/**
 * Tool for listing schemas within a Trino catalog.
 * <p>
 * Enables schema discovery after users have identified a catalog of interest.
 * Works seamlessly with both multi-catalog and single-catalog connections.
 * <p>
 * <strong>Use Cases:</strong>
 * <ul>
 *   <li>User asks: "What schemas are in viewzoo?"</li>
 *   <li>After showing catalogs, guide user to explore schemas</li>
 *   <li>Progressive discovery: catalogs → schemas → analysis</li>
 * </ul>
 * <p>
 * <strong>Example Conversation:</strong>
 * <pre>
 * User: "What schemas are in viewzoo?"
 * Agent: [Calls listSchemas with catalog="viewzoo"]
 * Returns: ["example", "test", "dev"]
 * Agent: "Found 3 schemas: example, test, dev. Which should I analyze?"
 * </pre>
 * <p>
 * <strong>Catalog Parameter Handling:</strong>
 * <ul>
 *   <li><strong>Multi-catalog connections:</strong> Catalog parameter required (cannot be null)</li>
 *   <li><strong>Single-catalog connections:</strong> Catalog parameter optional (uses bound catalog if null)</li>
 * </ul>
 */
public class ListSchemasToolExecutor {

    private final DiscoveryProvider provider;

    /**
     * Creates a schema listing tool.
     *
     * @param provider Provider for catalog/schema discovery
     * @throws IllegalArgumentException if provider is null
     */
    public ListSchemasToolExecutor(DiscoveryProvider provider) {
        this.provider = Objects.requireNonNull(provider, "Discovery provider cannot be null");
    }

    /**
     * Lists all schemas in the specified catalog.
     * <p>
     * For JDBC: Executes {@code SHOW SCHEMAS FROM catalog}.
     * <p>
     * For test datasets: Returns test dataset names if catalog is "test".
     * <p>
     * If the connection is bound to a catalog (single-catalog mode), the catalog
     * parameter is optional and will default to the bound catalog. If the
     * connection allows multi-catalog exploration, the catalog parameter is required.
     *
     * @param catalog Catalog name (required for multi-catalog, optional for single-catalog)
     * @return List of schema names in the specified catalog
     */
    @Tool("Lists all schemas in a Trino catalog. Use this when user wants to know what schemas are available in a specific catalog.")
    public List<String> listSchemas(String catalog) {
        return provider.listSchemas(catalog);
    }

}

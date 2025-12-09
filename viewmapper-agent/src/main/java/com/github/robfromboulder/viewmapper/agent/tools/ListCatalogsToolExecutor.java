// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.discovery.DiscoveryProvider;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;
import java.util.Objects;

/**
 * Tool for listing available Trino catalogs.
 * <p>
 * Enables natural catalog discovery at the start of exploration workflows,
 * especially important for multi-catalog connections (recommended default)
 * where users haven't specified a catalog in their JDBC URL.
 * <p>
 * <strong>Use Cases:</strong>
 * <ul>
 *   <li>User asks: "What catalogs are available?"</li>
 *   <li>User asks: "What's in my database?" (proactively show catalogs)</li>
 *   <li>Multi-catalog exploration workflow (recommended configuration)</li>
 * </ul>
 * <p>
 * <strong>Example Conversation:</strong>
 * <pre>
 * User: "What catalogs can I explore?"
 * Agent: [Calls listCatalogs]
 * Returns: ["viewzoo", "production", "staging"]
 * Agent: "I found 3 catalogs: viewzoo, production, staging. Which interests you?"
 * </pre>
 */
public class ListCatalogsToolExecutor {

    private final DiscoveryProvider provider;

    /**
     * Creates a catalog listing tool.
     *
     * @param provider Provider for catalog/schema discovery
     * @throws IllegalArgumentException if provider is null
     */
    public ListCatalogsToolExecutor(DiscoveryProvider provider) {
        this.provider = Objects.requireNonNull(provider, "Discovery provider cannot be null");
    }

    /**
     * Lists all catalogs available in the Trino instance or test environment.
     * <p>
     * For JDBC: Returns actual catalogs from {@code SHOW CATALOGS}.
     * <p>
     * For test datasets: Returns synthetic catalog {@code ["test"]}.
     * <p>
     * Use this when the user wants to explore available catalogs, especially
     * at the start of a multi-catalog exploration session.
     *
     * @return List of catalog names
     */
    @Tool("Lists all available Trino catalogs. Use this when user asks what catalogs are available or wants to explore their database.")
    public List<String> listCatalogs() {
        return provider.listCatalogs();
    }

}

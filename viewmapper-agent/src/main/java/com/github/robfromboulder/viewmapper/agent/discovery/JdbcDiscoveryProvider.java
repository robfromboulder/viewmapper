// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.discovery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JDBC-based implementation of DiscoveryProvider for live Trino connections.
 * <p>
 * Executes metadata queries against a Trino instance to discover catalogs and schemas.
 * Handles both single-catalog connections (catalog in URL) and multi-catalog connections
 * (no catalog in URL).
 * <p>
 * <strong>Connection Modes:</strong>
 * <ul>
 *   <li><strong>Multi-catalog (recommended):</strong> {@code jdbc:trino://host:port?user=username}
 *       <br>Allows exploring any catalog. Schema parameter required for {@link #listSchemas(String)}.</li>
 *   <li><strong>Single-catalog (advanced):</strong> {@code jdbc:trino://host:port/catalog?user=username}
 *       <br>Bound to specific catalog. Schema parameter optional (uses bound catalog if null).</li>
 * </ul>
 * <p>
 * <strong>Example Usage:</strong>
 * <pre>{@code
 * // Multi-catalog mode
 * DiscoveryProvider provider = new JdbcDiscoveryProvider("jdbc:trino://localhost:8080?user=claude");
 * List<String> catalogs = provider.listCatalogs();  // ["viewzoo", "production", "staging"]
 * List<String> schemas = provider.listSchemas("viewzoo");  // ["example", "test", "dev"]
 *
 * // Single-catalog mode
 * DiscoveryProvider provider = new JdbcDiscoveryProvider("jdbc:trino://localhost:8080/production?user=claude");
 * List<String> catalogs = provider.listCatalogs();  // ["production", "viewzoo", ...]
 * List<String> schemas = provider.listSchemas(null);  // Uses "production" from URL
 * }</pre>
 */
public class JdbcDiscoveryProvider implements DiscoveryProvider {

    private final String boundCatalog;
    private final String jdbcUrl;

    /**
     * Creates a JDBC discovery provider.
     * <p>
     * Connects to Trino to detect if the URL specifies a catalog (single-catalog mode)
     * or allows multi-catalog exploration.
     *
     * @param jdbcUrl JDBC connection URL (must start with "jdbc:trino://")
     * @throws IllegalArgumentException if jdbcUrl is null
     * @throws RuntimeException         if unable to connect to Trino
     */
    public JdbcDiscoveryProvider(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "JDBC URL required");

        // detect if connection is catalog-bound
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            String catalog = conn.getCatalog();
            this.boundCatalog = (catalog != null && !catalog.trim().isEmpty()) ? catalog : null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to Trino: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all catalogs available in the Trino instance.
     * <p>
     * Executes {@code SHOW CATALOGS} query. Returns all catalogs visible to the
     * user, regardless of whether the connection is catalog-bound.
     *
     * @return List of catalog names, never null
     * @throws RuntimeException if database query fails
     */
    @Override
    public List<String> listCatalogs() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CATALOGS")) {
            List<String> catalogs = new ArrayList<>();
            while (rs.next()) catalogs.add(rs.getString(1));
            return catalogs;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list catalogs: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all schemas in the specified catalog.
     * <p>
     * For single-catalog connections: If catalog parameter is null, uses the
     * bound catalog from the URL. If catalog parameter is non-null, it must
     * match the bound catalog.
     * <p>
     * For multi-catalog connections: Catalog parameter is required (cannot be null).
     *
     * @param catalog Catalog name (required for multi-catalog, optional for single-catalog)
     * @return List of schema names, never null
     * @throws IllegalArgumentException if catalog is invalid or required but not provided
     * @throws RuntimeException         if database query fails
     */
    @Override
    public List<String> listSchemas(String catalog) {
        String targetCatalog;

        if (boundCatalog == null) {
            // multi-catalog mode: catalog parameter is required
            if (catalog == null || catalog.isEmpty()) throw new IllegalArgumentException("Catalog parameter required when connection has no catalog");
            targetCatalog = catalog;
        } else {
            // single-catalog mode: connection is bound to a specific catalog
            if (!boundCatalog.equals(catalog))
                throw new IllegalArgumentException("Connection is bound to catalog '" + boundCatalog + "'. Cannot query different catalog '" + catalog + "'");
            targetCatalog = boundCatalog;
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW SCHEMAS FROM " + targetCatalog)) {
            List<String> schemas = new ArrayList<>();
            while (rs.next()) schemas.add(rs.getString(1));
            return schemas;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list schemas in catalog '" + targetCatalog + "': " + e.getMessage(), e);
        }
    }

}

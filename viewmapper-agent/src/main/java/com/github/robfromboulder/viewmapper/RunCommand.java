// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.robfromboulder.viewmapper.agent.ViewMapperAgent;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI command for running the agent.
 */
@Command(name = "run", description = "Run the agent with specified parameters")
public class RunCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Your question or command about the schema")
    private String prompt;

    @Option(names = {"--connection"}, description = "Connection string: 'test://<dataset_name>' or 'jdbc:trino://...'", required = true)
    private String connection;

    @Option(names = {"--schema"}, description = "Trino schema name (required when connecting via JDBC)")
    private String schema;

    @Option(names = {"--output"}, description = "Output format: text (default) or json", defaultValue = "text")
    private String outputFormat;

    @Option(names = {"--verbose"}, description = "Show detailed debugging information")
    private boolean verbose;

    /**
     * Load view dependencies and call the agent to map them.
     */
    @Override
    public Integer call() throws Exception {
        try {
            // use connection string to initialize analyzer
            DependencyAnalyzer analyzer = new DependencyAnalyzer();
            if (connection.startsWith("test://")) {
                String datasetName = connection.substring(7);
                loadFromFile(analyzer, datasetName);
            } else if (connection.startsWith("jdbc:trino://")) {
                if (schema == null || schema.trim().isEmpty())
                    throw new IllegalArgumentException("--schema parameter is required for JDBC connections\nExample: --connection jdbc:trino://user:pass@host:8080/catalog --schema analytics");
                loadFromJdbc(analyzer, connection, schema);
            } else {
                throw new IllegalArgumentException("Invalid connection string. Must start with 'test://' or 'jdbc:trino://'\nExamples:\n  --connection test://simple_ecommerce\n  --connection jdbc:trino://user:pass@localhost:8080/catalog --schema analytics");
            }

            // call agent with dependency analyzer and user prompt
            if (verbose) System.err.println("Analyzing schema with " + analyzer.getViewCount() + " views");
            ViewMapperAgent agent = new ViewMapperAgent(analyzer);
            String response = agent.chat(prompt);

            // print agent response to stdout
            if (verbose) System.err.println("Writing output with format: " + outputFormat);
            if ("json".equalsIgnoreCase(outputFormat)) {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(java.util.Map.of("response", response));
                System.out.println(json);
            } else {
                System.out.println(response);
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) e.printStackTrace();
            return 1;
        }
    }

    /**
     * POJO for loading dataset file from JSON.
     */
    static class DataFile {
        public String description;
        public List<View> views;
    }

    /**
     * POJO for loading view from JSON.
     */
    static class View {
        public String name;
        public String sql;
    }

    /**
     * Loads view dependencies from JSON resource file.
     * Catalog/schema handling: none, schema parameter has no effect
     */
    private void loadFromFile(DependencyAnalyzer analyzer, String datasetName) throws Exception {
        if (verbose) System.err.println("Loading from file...");
        java.io.InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("datasets/" + datasetName + ".json");
        if (resourceStream == null) throw new IllegalArgumentException("Dataset not found: " + datasetName);
        ObjectMapper mapper = new ObjectMapper();
        DataFile data = mapper.readValue(resourceStream, DataFile.class);
        for (View view : data.views) analyzer.addView(view.name, view.sql);
    }

    /**
     * Loads view dependencies from a live Trino database via JDBC.
     * Uses a single query to fetch all views and their definitions.
     * <p>
     * Catalog handling:
     * - If URL specifies catalog: --schema must be simple name (bound to that catalog)
     * - If URL has no catalog: --schema must be "catalog.schema" format
     */
    private void loadFromJdbc(DependencyAnalyzer analyzer, String jdbcUrl, String schemaName) throws Exception {
        if (verbose) System.err.println("Loading from Trino...");
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            String catalog;
            String schema;
            final String urlCatalog = conn.getCatalog();
            if (urlCatalog != null && !urlCatalog.trim().isEmpty()) {  // url specifies catalog, so conversation is bound to it
                if (schemaName.contains(".")) {
                    throw new IllegalArgumentException("Connection is bound to catalog '" + urlCatalog + "'. " +
                            "Use simple schema name (not catalog.schema).\n" +
                            "Example: --schema analytics"
                    );
                }
                catalog = urlCatalog;
                schema = schemaName;
            } else {  // no catalog in url, so require qualified schema to proceed
                if (!schemaName.contains(".")) {
                    throw new IllegalArgumentException("Connection URL doesn't specify catalog. " +
                            "Use qualified schema: --schema catalog.schema\n" +
                            "Example: --schema viewzoo.example"
                    );
                }
                String[] parts = schemaName.split("\\.", 2);
                catalog = parts[0];
                schema = parts[1];
            }

            // query for all views and their SQL definitions
            if (verbose) System.err.println("Querying for catalog: " + catalog + ", schema: " + schema);
            String sql = "SELECT table_name, view_definition FROM information_schema.views WHERE table_catalog = ? AND table_schema = ? ORDER BY table_name";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, catalog);
                stmt.setString(2, schema);
                try (ResultSet rs = stmt.executeQuery()) {
                    boolean empty = true;
                    while (rs.next()) {
                        String viewName = rs.getString("table_name");
                        String viewSql = rs.getString("view_definition");
                        String fullyQualifiedName = catalog + "." + schema + "." + viewName;
                        analyzer.addView(fullyQualifiedName, viewSql);
                        empty = false;
                    }
                    if (empty) throw new IllegalArgumentException("No views found in " + catalog + "." + schema);
                }
            }
        }
    }

}

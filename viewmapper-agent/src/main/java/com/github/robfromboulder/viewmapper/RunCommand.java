// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.robfromboulder.viewmapper.agent.ViewMapperAgent;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

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

    @Option(names = {"--schema"}, description = "Trino schema name (used when connecting via JDBC)")
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
            ObjectMapper mapper = new ObjectMapper();

            // use connection string to initialize analyzer
            DependencyAnalyzer analyzer = new DependencyAnalyzer();
            if (connection.startsWith("test://")) {
                String datasetName = connection.substring(7);
                java.io.InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("datasets/" + datasetName + ".json");
                if (resourceStream == null) throw new IllegalArgumentException("Dataset not found: " + datasetName);
                DataFile data = mapper.readValue(resourceStream, DataFile.class);
                for (View view : data.views) analyzer.addView(view.name, view.sql);
            } else if (connection.startsWith("jdbc:")) {
                throw new UnsupportedOperationException("JDBC connections not yet implemented. Use --connection test://<dataset_name> for testing.");
            } else {
                throw new IllegalArgumentException("Invalid connection string. Must start with 'test://' or 'jdbc://'\nExamples:\n  --connection test://simple_ecommerce\n  --connection jdbc:trino://localhost:8080/catalog");
            }
            if (verbose) System.err.println("Initialized analyzer with " + analyzer.getViewCount() + " views");

            // call agent with dependency analyzer and user prompt
            ViewMapperAgent agent = new ViewMapperAgent(analyzer);
            String response = agent.chat(prompt);

            // print agent response to stdout
            if ("json".equalsIgnoreCase(outputFormat)) {
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

}

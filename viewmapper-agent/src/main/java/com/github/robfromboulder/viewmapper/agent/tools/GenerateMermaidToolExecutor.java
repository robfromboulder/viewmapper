// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.types.SubgraphResult;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Tool for generating Mermaid diagrams from dependency subgraphs.
 * <p>
 * Creates formatted Mermaid syntax with styling to highlight focus view, upstream dependencies, and downstream dependents.
 */
public class GenerateMermaidToolExecutor {

    private final DependencyAnalyzer analyzer;
    private final ExtractSubgraphToolExecutor extractor;

    /**
     * Default constructor.
     */
    public GenerateMermaidToolExecutor(DependencyAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "Analyzer cannot be null");
        this.extractor = new ExtractSubgraphToolExecutor(analyzer);
    }

    /**
     * Generates a Mermaid diagram for the entire schema.
     * <p>
     * Use this for SIMPLE schemas where showing all views is feasible.
     *
     * @return Mermaid diagram syntax for complete schema
     */
    @Tool("Generates Mermaid diagram for entire schema. Use for SIMPLE schemas (<20 views) to show full dependency graph.")
    public String generateFullSchemaDiagram() {
        Graph<String, DefaultEdge> graph = analyzer.getGraph();
        Set<String> allViews = graph.vertexSet();
        if (allViews.isEmpty())
            return "graph TB\n    empty[No views in schema]";
        else if (allViews.size() > 100)
            return String.format("graph TB\n    error[Schema too large: %d views. Use extractSubgraph instead.]", allViews.size());

        // start diagram
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("```mermaid\n");
        mermaid.append("graph TB\n");

        // generate nodes
        Map<String, String> nodeIds = generateNodeIds(allViews);
        for (String view : allViews) {
            String nodeId = nodeIds.get(view);
            String label = formatLabel(view);
            mermaid.append(String.format("    %s[\"%s\"]\n", nodeId, label));
        }
        mermaid.append("\n");

        // generate edges
        for (DefaultEdge edge : graph.edgeSet()) {
            String source = graph.getEdgeSource(edge);
            String target = graph.getEdgeTarget(edge);
            String sourceId = nodeIds.get(source);
            String targetId = nodeIds.get(target);
            mermaid.append(String.format("    %s --> %s\n", sourceId, targetId));
        }
        mermaid.append("```\n");

        return mermaid.toString();
    }

    /**
     * Generates a Mermaid diagram of the subgraph around a view.
     * <p>
     * Takes the same arguments as extractSubgraph rather than a subgraph, because the model can only pass JSON values,
     * and a subgraph copied through the model could be altered on the way back.
     *
     * @param focusView       The view to focus on (center of the subgraph)
     * @param depthUpstream   How many levels of dependencies to include (incoming edges)
     * @param depthDownstream How many levels of dependents to include (outgoing edges)
     * @param maxNodes        Maximum nodes in result (0 = unlimited, default = 50 for readability)
     * @return Mermaid diagram syntax
     */
    @Tool("Generates Mermaid diagram of the subgraph around a view, within specified depths. Use for MODERATE/COMPLEX schemas once a focus view is chosen.")
    public String generateMermaid(
            @P("View to focus on, at the center of the subgraph") String focusView,
            @P("Levels of dependencies to include") int depthUpstream,
            @P("Levels of dependents to include") int depthDownstream,
            @P(value = "Maximum views to include (0 = unlimited, default 50)", required = false) Integer maxNodes) {
        return renderSubgraph(extractor.extractSubgraph(focusView, depthUpstream, depthDownstream, maxNodes));
    }

    /**
     * Renders a subgraph as a Mermaid diagram.
     * <p>
     * Applies styling rules:
     * - Focus view: Red/bold border
     * - Upstream dependencies: Blue background
     * - Downstream dependents: Green background
     *
     * @param subgraph The subgraph to visualize
     * @return Mermaid diagram syntax
     */
    public String renderSubgraph(SubgraphResult subgraph) {
        Objects.requireNonNull(subgraph, "Subgraph cannot be null");
        if (subgraph.getViewCount() == 0)
            return "graph TB\n    empty[No views in subgraph]";
        else if (subgraph.getViewCount() > 100)
            return String.format("graph TB\n    error[Subgraph too large: %d views. Maximum 100 for readability.]", subgraph.getViewCount());

        // start diagram
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("```mermaid\n");
        mermaid.append("graph TB\n");

        // categorize views
        String focusView = subgraph.getFocusView();
        Set<String> upstream = findUpstream(focusView, subgraph.getViews());
        Set<String> downstream = findDownstream(focusView, subgraph.getViews());

        // generate node declarations with labels
        Map<String, String> nodeIds = generateNodeIds(subgraph.getViews());
        for (String view : subgraph.getViews()) {
            String nodeId = nodeIds.get(view);
            String label = formatLabel(view);
            mermaid.append(String.format("    %s[\"%s\"]\n", nodeId, label));
        }
        mermaid.append("\n");

        // generate edges
        Graph<String, DefaultEdge> graph = analyzer.getGraph();
        for (String view : subgraph.getViews()) {
            for (DefaultEdge edge : graph.outgoingEdgesOf(view)) {
                String target = graph.getEdgeTarget(edge);
                if (subgraph.contains(target)) {
                    String sourceId = nodeIds.get(view);
                    String targetId = nodeIds.get(target);
                    mermaid.append(String.format("    %s --> %s\n", sourceId, targetId));
                }
            }
        }
        mermaid.append("\n");

        // apply styling
        String focusId = nodeIds.get(focusView);
        mermaid.append(String.format("    style %s fill:#FF6B6B,stroke:#D32F2F,stroke-width:3px\n", focusId));
        for (String view : upstream) {
            String nodeId = nodeIds.get(view);
            mermaid.append(String.format("    style %s fill:#90CAF9,stroke:#1976D2\n", nodeId));
        }
        for (String view : downstream) {
            String nodeId = nodeIds.get(view);
            mermaid.append(String.format("    style %s fill:#A5D6A7,stroke:#388E3C\n", nodeId));
        }
        mermaid.append("```\n");

        return mermaid.toString();
    }

    /**
     * Finds downstream dependents of focus view within the subgraph.
     */
    private Set<String> findDownstream(String focusView, Set<String> subgraphViews) {
        Set<String> downstream = new HashSet<>();
        Graph<String, DefaultEdge> graph = analyzer.getGraph();
        for (DefaultEdge edge : graph.outgoingEdgesOf(focusView)) {
            String target = graph.getEdgeTarget(edge);
            if (subgraphViews.contains(target)) downstream.add(target);
        }
        return downstream;
    }

    /**
     * Finds upstream dependencies of focus view within the subgraph.
     */
    private Set<String> findUpstream(String focusView, Set<String> subgraphViews) {
        Set<String> upstream = new HashSet<>();
        Graph<String, DefaultEdge> graph = analyzer.getGraph();
        for (DefaultEdge edge : graph.incomingEdgesOf(focusView)) {
            String source = graph.getEdgeSource(edge);
            if (subgraphViews.contains(source)) upstream.add(source);
        }
        return upstream;
    }

    /**
     * Formats view name for display in diagram, simplifying fully qualified names for readability.
     */
    private String formatLabel(String viewName) {
        String[] parts = viewName.split("\\.");
        if (parts.length == 3) {
            return parts[1] + "." + parts[2];
        } else if (parts.length == 2) {
            return parts[0] + "." + parts[1];
        }
        return viewName;
    }

    /**
     * Generates valid Mermaid node IDs from view names, replacing special characters with underscores.
     */
    private Map<String, String> generateNodeIds(Set<String> views) {
        Map<String, String> ids = new HashMap<>();
        int counter = 1;
        for (String view : views) {
            String nodeId = "node" + counter++;
            ids.put(view, nodeId);
        }
        return ids;
    }

}

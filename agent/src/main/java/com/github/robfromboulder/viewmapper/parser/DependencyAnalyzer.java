// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analyzes view dependencies and provides graph-based insights.
 * <p>
 * This class builds a directed dependency graph where:
 * - Nodes are table/view names (fully qualified: catalog.schema.table)
 * - Edges point from dependencies TO dependents (upstream -> downstream)
 * <p>
 * Provides methods for finding:
 * - High-impact views (most dependents, by out-degree)
 * - Leaf views (no dependents, zero out-degree)
 * - Central hub views (betweenness centrality)
 * - Subgraphs around a focus view (BFS-based)
 */
public class DependencyAnalyzer {

    private final Graph<String, DefaultEdge> graph;
    private final TrinoSqlParser parser;

    /**
     * Creates a new DependencyAnalyzer with an empty graph.
     */
    public DependencyAnalyzer() {
        this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        this.parser = new TrinoSqlParser();
    }

    /**
     * Adds a view and its dependencies to the graph.
     * <p>
     * Parses the SQL to extract dependencies and creates edges from each dependency to this view (dependency -> view).
     *
     * @param viewName The fully qualified name of the view
     * @param sql      The SQL definition of the view
     */
    public void addView(String viewName, String sql) {
        // add view node if not present
        if (!graph.containsVertex(viewName)) graph.addVertex(viewName);

        // extract dependencies and create edges
        Set<TableReference> dependencies = parser.extractDependencies(sql);
        for (TableReference dep : dependencies) {
            String depName = dep.getFullyQualifiedName();

            // add dependency node if not present
            if (!graph.containsVertex(depName)) graph.addVertex(depName);

            // create edge: dependency -> view
            try {
                graph.addEdge(depName, viewName);
            } catch (IllegalArgumentException e) {
                // edge already exists or would create a cycle
                // in real-world scenarios, cycles shouldn't be definable, but we handle it gracefully
            }
        }
    }

    /**
     * Calculates betweenness centrality for all views.
     * <p>
     * Betweenness centrality measures how often a node appears on shortest paths between other nodes. High centrality indicates a view that connects many
     * upstream sources to many downstream consumers - a "hub" or integration point.
     *
     * @param limit Maximum number of results to return
     * @return Map of view name to centrality score, sorted descending by score
     */
    public Map<String, Double> findCentralHubs(int limit) {
        BetweennessCentrality<String, DefaultEdge> centrality = new BetweennessCentrality<>(graph);
        return graph.vertexSet().stream()
                .collect(Collectors.toMap(view -> view, centrality::getVertexScore))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * Finds high-impact views sorted by number of dependents (out-degree).
     * <p>
     * High-impact views are those that many other views depend on. These are typically core/foundational views in the schema.
     *
     * @param limit Maximum number of results to return
     * @return Map of view name to dependent count, sorted descending by count
     */
    public Map<String, Integer> findHighImpactViews(int limit) {
        return graph.vertexSet().stream()
                .collect(Collectors.toMap(view -> view, graph::outDegreeOf))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * Finds leaf views (zero out-degree).
     * <p>
     * Leaf views have no dependents, meaning they are likely final outputs, reports, or end-user facing views.
     *
     * @return List of view names with no dependents
     */
    public List<String> findLeafViews() {
        return graph.vertexSet().stream()
                .filter(view -> graph.outDegreeOf(view) == 0)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Extracts a focused subgraph around a specific view.
     * <p>
     * Uses BFS to traverse upstream (dependencies) and downstream (dependents) from the focus view up to specified depths.
     *
     * @param focusView       The view to focus on
     * @param depthUpstream   How many levels of dependencies to include (incoming edges)
     * @param depthDownstream How many levels of dependents to include (outgoing edges)
     * @param maxNodes        Maximum number of nodes to include (0 = unlimited)
     * @return Set of view names in the subgraph, including the focus view
     */
    public Set<String> findSubgraph(String focusView, int depthUpstream, int depthDownstream, int maxNodes) {
        if (!graph.containsVertex(focusView)) return new HashSet<>();

        Set<String> subgraph = new HashSet<>();
        subgraph.add(focusView);

        // traverse upstream (dependencies) - follow incoming edges
        if (depthUpstream > 0) {
            Set<String> upstream = bfsUpstream(focusView, depthUpstream);
            subgraph.addAll(upstream);
        }

        // traverse downstream (dependents) - follow outgoing edges
        if (depthDownstream > 0) {
            Set<String> downstream = bfsDownstream(focusView, depthDownstream);
            subgraph.addAll(downstream);
        }

        // limit results if specified
        if (maxNodes > 0 && subgraph.size() > maxNodes) {
            List<String> sorted = new ArrayList<>(subgraph);
            sorted.remove(focusView);
            sorted.sort(Comparator.comparingInt(v -> -(graph.inDegreeOf(v) + graph.outDegreeOf(v))));
            Set<String> limited = new HashSet<>();
            limited.add(focusView);
            limited.addAll(sorted.subList(0, Math.min(sorted.size(), maxNodes - 1)));
            return limited;
        }

        return subgraph;
    }

    /**
     * BFS traversal following incoming edges (dependencies).
     */
    private Set<String> bfsUpstream(String start, int maxDepth) {
        List<String> queue = new ArrayList<>();
        queue.add(start);

        Map<String, Integer> depths = new HashMap<>();
        depths.put(start, 0);

        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            int currentDepth = depths.get(current);
            if (currentDepth >= maxDepth) continue;
            for (DefaultEdge edge : graph.incomingEdgesOf(current)) {
                String source = graph.getEdgeSource(edge);
                if (!depths.containsKey(source)) {
                    depths.put(source, currentDepth + 1);
                    visited.add(source);
                    queue.add(source);
                }
            }
        }
        return visited;
    }

    /**
     * BFS traversal following outgoing edges (dependents).
     */
    private Set<String> bfsDownstream(String start, int maxDepth) {
        List<String> queue = new ArrayList<>();
        queue.add(start);

        Map<String, Integer> depths = new HashMap<>();
        depths.put(start, 0);

        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            int currentDepth = depths.get(current);
            if (currentDepth >= maxDepth) continue;
            for (DefaultEdge edge : graph.outgoingEdgesOf(current)) {
                String target = graph.getEdgeTarget(edge);
                if (!depths.containsKey(target)) {
                    depths.put(target, currentDepth + 1);
                    visited.add(target);
                    queue.add(target);
                }
            }
        }
        return visited;
    }

    /**
     * Checks if a view exists in the graph.
     */
    public boolean containsView(String viewName) {
        // todo not covered by test cases, remove if not needed
        return graph.containsVertex(viewName);
    }

    /**
     * Gets the number of dependencies (incoming edges) for a view.
     */
    public int getDependencyCount(String viewName) {
        // todo not covered by test cases, remove if not needed
        return graph.containsVertex(viewName) ? graph.inDegreeOf(viewName) : 0;
    }

    /**
     * Gets the number of dependents (outgoing edges) for a view.
     */
    public int getDependentCount(String viewName) {
        // todo not covered by test cases, remove if not needed
        return graph.containsVertex(viewName) ? graph.outDegreeOf(viewName) : 0;
    }

    /**
     * Returns the underlying graph (for testing or advanced analysis).
     */
    public Graph<String, DefaultEdge> getGraph() {
        // todo not covered by test cases, remove if not needed
        return graph;
    }

    /**
     * Returns the total number of views in the graph.
     */
    public int getViewCount() {
        // todo not covered by test cases, remove if not needed
        return graph.vertexSet().size();
    }

}

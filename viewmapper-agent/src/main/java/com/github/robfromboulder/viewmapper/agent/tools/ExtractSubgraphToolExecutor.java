// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.types.SubgraphResult;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Objects;
import java.util.Set;

/**
 * Tool for extracting focused subgraphs around a specific view.
 * <p>
 * Uses BFS to traverse upstream (dependencies) and downstream (dependents) to build a manageable subset.
 */
public class ExtractSubgraphToolExecutor {

    public static final int DEFAULT_MAX_NODES = 50; // reasonable for diagram readability

    // shared with generateMermaid, which selects its views the same way
    public static final String FOCUS_VIEW_DESCRIPTION = "View to focus on, at the center of the subgraph";
    public static final String DEPTH_UPSTREAM_DESCRIPTION = "Levels of dependencies to include";
    public static final String DEPTH_DOWNSTREAM_DESCRIPTION = "Levels of dependents to include";

    private final DependencyAnalyzer analyzer;

    /**
     * Default constructor.
     */
    public ExtractSubgraphToolExecutor(DependencyAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "Analyzer cannot be null");
    }

    /**
     * Extracts a focused subgraph around a specific view.
     * <p>
     * Use this after user selects an entry point to get a visualizable subset of the schema.
     *
     * @param focusView       The view to focus on (center of the subgraph)
     * @param depthUpstream   How many levels of dependencies to include (incoming edges)
     * @param depthDownstream How many levels of dependents to include (outgoing edges)
     * @param maxNodes        Maximum nodes in result (0 = unlimited, default = 50 for readability)
     * @return Subgraph result with view list and metadata
     */
    @Tool("Extracts focused subgraph around a view. Returns views within specified depths. Use after selecting entry point.")
    public SubgraphResult extractSubgraph(
            @P(FOCUS_VIEW_DESCRIPTION) String focusView,
            @P(DEPTH_UPSTREAM_DESCRIPTION) int depthUpstream,
            @P(DEPTH_DOWNSTREAM_DESCRIPTION) int depthDownstream,
            @P(value = "Maximum views to include (0 = unlimited, default " + DEFAULT_MAX_NODES + ")", required = false)
            Integer maxNodes) {
        if (!analyzer.containsView(focusView)) throw new IllegalArgumentException(String.format("View '%s' not found in schema", focusView));
        int effectiveMaxNodes = maxNodes != null ? maxNodes : DEFAULT_MAX_NODES;
        Set<String> views = analyzer.findSubgraph(focusView, depthUpstream, depthDownstream, effectiveMaxNodes);
        boolean truncated = effectiveMaxNodes > 0 && views.size() >= effectiveMaxNodes;
        return new SubgraphResult(views, focusView, depthUpstream, depthDownstream, truncated);
    }

}

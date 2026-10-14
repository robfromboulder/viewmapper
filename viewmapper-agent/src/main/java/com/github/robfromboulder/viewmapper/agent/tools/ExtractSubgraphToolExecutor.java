// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.anthropic.helpers.BetaRunnableTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.robfromboulder.viewmapper.agent.types.SubgraphResult;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import java.util.List;
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

    /**
     * Arguments for extractSubgraph.
     */
    @JsonTypeName("extractSubgraph")
    @JsonClassDescription("Extracts focused subgraph around a view. Returns views within specified depths. "
            + "Use after selecting entry point.")
    public static class Arguments {
        @JsonPropertyDescription(FOCUS_VIEW_DESCRIPTION)
        public String focusView;

        @JsonPropertyDescription(DEPTH_UPSTREAM_DESCRIPTION)
        public int depthUpstream;

        @JsonPropertyDescription(DEPTH_DOWNSTREAM_DESCRIPTION)
        public int depthDownstream;

        @JsonPropertyDescription("Maximum views to include, " + DEFAULT_MAX_NODES
                + " unless the user asks for more, 0 for unlimited")
        public Integer maxNodes;
    }

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
    public SubgraphResult extractSubgraph(String focusView, int depthUpstream, int depthDownstream, Integer maxNodes) {
        if (!analyzer.containsView(focusView)) throw new IllegalArgumentException(String.format("View '%s' not found in schema", focusView));
        int effectiveMaxNodes = maxNodes != null ? maxNodes : DEFAULT_MAX_NODES;
        Set<String> views = analyzer.findSubgraph(focusView, depthUpstream, depthDownstream, effectiveMaxNodes);
        boolean truncated = effectiveMaxNodes > 0 && views.size() >= effectiveMaxNodes;
        return new SubgraphResult(views, focusView, depthUpstream, depthDownstream, truncated);
    }

    /**
     * Returns the tools this executor provides to the model.
     */
    public List<BetaRunnableTool> tools() {
        return List.of(BetaRunnableTool.of(Arguments.class,
                a -> RunnableTools.result(extractSubgraph(a.focusView, a.depthUpstream, a.depthDownstream, a.maxNodes))));
    }

}

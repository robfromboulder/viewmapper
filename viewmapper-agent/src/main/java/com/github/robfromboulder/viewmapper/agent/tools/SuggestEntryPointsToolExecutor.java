// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.types.EntryPointSuggestion;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Tool for suggesting meaningful entry points for schema exploration.
 * <p>
 * Provides different strategies: high-impact views, leaf views, or central hubs.
 */
public class SuggestEntryPointsToolExecutor {

    public static final int DEFAULT_LIMIT = 5;

    private final DependencyAnalyzer analyzer;

    /**
     * Default constructor.
     */
    public SuggestEntryPointsToolExecutor(DependencyAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "Analyzer cannot be null");
    }

    /**
     * Suggests entry points for schema exploration based on the specified strategy.
     * <p>
     * Use this when schema is too large for full visualization and user needs guidance on where to start.
     *
     * @param strategy Strategy for finding entry points: "high-impact", "leaf-views", or "central-hubs"
     * @param limit    Maximum number of suggestions to return (default: 5)
     * @return List of entry point suggestions with scores and reasons
     */
    @Tool("Suggests entry points for exploration. Strategies: 'high-impact' (foundational views), 'leaf-views' (final outputs), 'central-hubs' (integration points)")
    public List<EntryPointSuggestion> suggestEntryPoints(String strategy, Integer limit) {
        int effectiveLimit = limit != null ? limit : DEFAULT_LIMIT;
        return switch (strategy.toLowerCase()) {
            case "high-impact" -> suggestHighImpact(effectiveLimit);
            case "leaf-views" -> suggestLeafViews(effectiveLimit);
            case "central-hubs" -> suggestCentralHubs(effectiveLimit);
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategy + ". Use 'high-impact', 'leaf-views', or 'central-hubs'");
        };
    }

    private List<EntryPointSuggestion> suggestCentralHubs(int limit) {
        Map<String, Double> centralHubs = analyzer.findCentralHubs(limit);
        return centralHubs.entrySet().stream().map(entry -> EntryPointSuggestion.centralHub(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    private List<EntryPointSuggestion> suggestHighImpact(int limit) {
        Map<String, Integer> highImpact = analyzer.findHighImpactViews(limit);
        return highImpact.entrySet().stream().map(entry -> EntryPointSuggestion.highImpact(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    private List<EntryPointSuggestion> suggestLeafViews(int limit) {
        List<String> leafViews = analyzer.findLeafViews();
        return leafViews.stream().limit(limit).map(EntryPointSuggestion::leafView).collect(Collectors.toList());
    }

}

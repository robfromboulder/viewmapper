// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.types;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Result of subgraph extraction around a focus view.
 * <p>
 * Contains the set of views in the subgraph, the focus view,
 * and metadata about the extraction (depth, node count).
 */
public class SubgraphResult {

    private final Set<String> views;
    private final String focusView;
    private final int depthUpstream;
    private final int depthDownstream;
    private final boolean truncated;

    /**
     * Constructor without truncation info.
     */
    public SubgraphResult(Set<String> views, String focusView) {
        this(views, focusView, 0, 0, false);
    }

    /**
     * Constructor with truncation info.
     */
    public SubgraphResult(Set<String> views, String focusView, int depthUpstream, int depthDownstream, boolean truncated) {
        this.views = Collections.unmodifiableSet(new HashSet<>(Objects.requireNonNull(views, "Views cannot be null")));
        this.focusView = Objects.requireNonNull(focusView, "Focus view cannot be null");
        this.depthUpstream = depthUpstream;
        this.depthDownstream = depthDownstream;
        this.truncated = truncated;
    }

    public Set<String> getViews() {
        return views;
    }

    public String getFocusView() {
        return focusView;
    }

    public int getViewCount() {
        return views.size();
    }

    public int getDepthUpstream() {
        return depthUpstream;
    }

    public int getDepthDownstream() {
        return depthDownstream;
    }

    public boolean isTruncated() {
        return truncated;
    }

    /**
     * Checks if the subgraph contains the given view.
     */
    public boolean contains(String viewName) {
        return views.contains(viewName);
    }

    /**
     * Returns true if the subgraph is small enough for visualization.
     * Threshold: 50 nodes is reasonable for diagram readability.
     */
    public boolean isVisualizationFeasible() {
        return views.size() <= 50;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubgraphResult that = (SubgraphResult) o;
        return depthUpstream == that.depthUpstream && depthDownstream == that.depthDownstream && truncated == that.truncated && Objects.equals(views, that.views) && Objects.equals(focusView, that.focusView);
    }

    @Override
    public int hashCode() {
        return Objects.hash(views, focusView, depthUpstream, depthDownstream, truncated);
    }

    @Override
    public String toString() {
        return String.format("SubgraphResult{focus='%s', views=%d, upstream=%d, downstream=%d, truncated=%s}", focusView, views.size(), depthUpstream, depthDownstream, truncated);
    }

}

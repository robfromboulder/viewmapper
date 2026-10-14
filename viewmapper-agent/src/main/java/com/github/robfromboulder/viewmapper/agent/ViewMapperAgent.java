// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.RequestOptions;
import com.anthropic.helpers.BetaRunnableTool;
import com.anthropic.helpers.BetaToolRunner;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaTextBlock;
import com.anthropic.models.beta.messages.BetaThinkingConfigAdaptive;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.anthropic.models.beta.messages.ToolRunnerCreateParams;
import com.anthropic.services.blocking.beta.MessageService;
import com.github.robfromboulder.viewmapper.agent.discovery.DiscoveryProvider;
import com.github.robfromboulder.viewmapper.agent.tools.AnalyzeSchemaToolExecutor;
import com.github.robfromboulder.viewmapper.agent.tools.ExtractSubgraphToolExecutor;
import com.github.robfromboulder.viewmapper.agent.tools.GenerateMermaidToolExecutor;
import com.github.robfromboulder.viewmapper.agent.tools.ListCatalogsToolExecutor;
import com.github.robfromboulder.viewmapper.agent.tools.ListSchemasToolExecutor;
import com.github.robfromboulder.viewmapper.agent.tools.SuggestEntryPointsToolExecutor;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Claude-powered agent that intelligently maps relationships between Trino views.
 * <p>
 * Orchestrates tool usage to guide users through complex dependency graphs with agentic reasoning.
 */
public class ViewMapperAgent {

    // retries some refusal categories on another model, server-side
    static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    // stops a runaway tool loop well before the timeout would
    static final int MAX_ITERATIONS = 25;

    // the API requires an explicit cap, and long Mermaid diagrams need room
    static final int MAX_TOKENS = 16000;

    static final String STRUCTURED_OUTPUTS_BETA = "structured-outputs-2025-11-13";

    static final String SYSTEM_PROMPT = """
            You are a database schema expert helping users explore complex dependency graphs.
            
            CRITICAL: You MUST use the provided tools to analyze schemas and generate diagrams.
            NEVER describe or explain diagrams without actually generating them using tools.
            
            DISCOVERY STRATEGY (MULTI-CATALOG FIRST):
            1. IMPORTANT: Most users connect with multi-catalog URLs (no catalog specified)
            2. If user asks "what catalogs" or "what schemas", use discovery tools immediately
            3. If user asks vague questions like "explore database" or "what's here", proactively offer discovery
            4. After showing catalogs/schemas, guide user to select one for analysis
            5. Once schema selected, use analyzeSchema to start exploration
            6. Discovery tools are fast (<500ms) - use them proactively to help users orient
            7. For multi-catalog connections, guide: catalogs → schemas → analysis
            8. For single-catalog connections, guide: schemas → analysis
            
            DISCOVERY TOOLS (if available):
            - listCatalogs: Show available catalogs (no parameters) - use this FIRST for multi-catalog connections
            - listSchemas: Show schemas in a catalog (requires catalog parameter unless connection is catalog-bound)
            
            MULTI-CATALOG AWARENESS:
            - Multi-catalog is the recommended default configuration
            - Read-only tool makes exploring all catalogs safe
            - Encourage users to explore multiple catalogs naturally
            
            MANDATORY WORKFLOW:
            1. ALWAYS call analyzeSchema first (required for every request)
            2. Based on complexity level returned:
               - SIMPLE (<20 views): IMMEDIATELY call generateFullSchemaDiagram() - no parameters needed
               - MODERATE (20-100 views): Call suggestEntryPoints, then extract subgraph after user selects
               - COMPLEX (100-500 views): REQUIRE entry point selection, then extract subgraph
               - VERY_COMPLEX (500+ views): Guide step-by-step through entry point selection
            
            3. When user asks for a diagram:
               - If SIMPLE: Call generateFullSchemaDiagram() and return the output directly
               - If not SIMPLE: Explain why entry point is needed, suggest options
            
            4. When user selects a focus view:
               - Call generateMermaid with the focus view and appropriate depth
               - Return the diagram output directly
            
            TOOL USAGE RULES:
            - analyzeSchema: ALWAYS call this FIRST
            - generateFullSchemaDiagram: Call for SIMPLE schemas when diagram requested (no parameters)
            - suggestEntryPoints: Call for non-SIMPLE schemas
            - extractSubgraph: Call to list the views around a focus view without drawing them
            - generateMermaid: Call with focus view and depths for non-SIMPLE schemas
            
            RESPONSE REQUIREMENTS:
            - When a tool returns a Mermaid diagram (starts with ```mermaid), include it in your response
            - When a diagram is followed by a TRUNCATED note, tell the user the diagram is incomplete and how to see more
            - Do NOT summarize or explain the diagram content without showing the actual diagram code
            - Be concise - let the diagram speak for itself
            - After showing diagram, you may add brief observations
            
            Remember: ALWAYS use tools. NEVER fake or describe output without calling the actual tool.
            """;

    private final AnthropicConfig config;
    private final MessageService messages;
    private final List<BetaRunnableTool> tools;

    /**
     * Creates a ViewMapperAgent with Anthropic Claude integration using environment configuration.
     *
     * @param analyzer Dependency analyzer with loaded schema
     */
    public ViewMapperAgent(DependencyAnalyzer analyzer) {
        this(analyzer, null, AnthropicConfig.fromEnvironment());
    }

    /**
     * Creates a ViewMapperAgent with discovery tools enabled.
     *
     * @param analyzer Dependency analyzer with loaded schema
     * @param provider Provider for catalog/schema discovery (null disables discovery tools)
     */
    public ViewMapperAgent(DependencyAnalyzer analyzer, DiscoveryProvider provider) {
        this(analyzer, provider, AnthropicConfig.fromEnvironment());
    }

    /**
     * Creates a ViewMapperAgent with explicit Anthropic configuration.
     * <p>
     * Primarily for testing or when overriding environment-based configuration.
     *
     * @param analyzer Dependency analyzer with loaded schema
     * @param provider Provider for catalog/schema discovery (null disables discovery tools)
     * @param config   Configuration for Anthropic API
     */
    public ViewMapperAgent(DependencyAnalyzer analyzer, DiscoveryProvider provider, AnthropicConfig config) {
        this(analyzer, provider, config, AnthropicOkHttpClient.builder()
                .apiKey(Objects.requireNonNull(config, "Config cannot be null").getApiKey())
                .timeout(config.getTimeout())
                .build().beta().messages());
    }

    /**
     * Constructor for testing with a custom message service.
     *
     * @param analyzer Dependency analyzer with loaded schema
     * @param provider Provider for catalog/schema discovery (null disables discovery tools)
     * @param config   Configuration for Anthropic API
     * @param messages Message service that answers model calls (e.g., a fake for testing)
     */
    ViewMapperAgent(DependencyAnalyzer analyzer, DiscoveryProvider provider, AnthropicConfig config, MessageService messages) {
        Objects.requireNonNull(analyzer, "Analyzer cannot be null");
        this.config = Objects.requireNonNull(config, "Config cannot be null");
        this.messages = Objects.requireNonNull(messages, "Message service cannot be null");
        List<BetaRunnableTool> tools = new ArrayList<>();
        if (provider != null) {
            tools.addAll(new ListCatalogsToolExecutor(provider).tools());
            tools.addAll(new ListSchemasToolExecutor(provider).tools());
        }
        tools.addAll(new AnalyzeSchemaToolExecutor(analyzer).tools());
        tools.addAll(new SuggestEntryPointsToolExecutor(analyzer).tools());
        tools.addAll(new ExtractSubgraphToolExecutor(analyzer).tools());
        tools.addAll(new GenerateMermaidToolExecutor(analyzer).tools());
        this.tools = List.copyOf(tools);
    }

    /**
     * Processes a user prompt.
     *
     * @param userPrompt Natural language prompt or command
     * @return Agent's response with analysis and recommendations
     * @throws IllegalStateException if the model declines, runs out of output tokens, or never finishes calling tools
     */
    public String chat(String userPrompt) {
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(config.getModelName())
                .maxTokens(MAX_TOKENS)
                .system(SYSTEM_PROMPT)
                .thinking(BetaThinkingConfigAdaptive.builder().build())
                .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.of(config.getEffort())).build())
                .addBeta(FALLBACK_BETA)
                .addBeta(STRUCTURED_OUTPUTS_BETA)
                .fallbacksDefault()
                .addUserMessage(userPrompt);
        tools.forEach(params::addTool);
        ToolRunnerCreateParams runnerParams = ToolRunnerCreateParams.builder()
                .initialMessageParams(params.build())
                .maxIterations(MAX_ITERATIONS)
                .build();

        // the runner returns each response, thinking blocks included, with the tool results that follow it
        BetaMessage last = null;
        for (BetaMessage message : new BetaToolRunner(messages, runnerParams, RequestOptions.none())) last = message;
        if (last == null) throw new IllegalStateException("Claude returned no response");
        return answer(last);
    }

    /**
     * Returns the text of the final response, or throws if it doesn't answer the question.
     */
    private String answer(BetaMessage message) {
        BetaStopReason reason = message.stopReason().orElse(null);
        if (BetaStopReason.REFUSAL.equals(reason)) {
            String category = message.stopDetails().flatMap(d -> d.category()).map(Object::toString).orElse("unspecified");
            throw new IllegalStateException("Claude declined to answer this question (category: " + category + ")");
        } else if (BetaStopReason.MAX_TOKENS.equals(reason)) {
            throw new IllegalStateException("Claude's answer exceeded " + MAX_TOKENS
                    + " output tokens and was cut off; ask for a smaller diagram, such as one view with lower depths");
        } else if (BetaStopReason.TOOL_USE.equals(reason)) {
            throw new IllegalStateException("Claude was still calling tools after " + MAX_ITERATIONS + " model calls");
        }
        String text = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(BetaTextBlock::text)
                .collect(Collectors.joining());
        if (text.isBlank()) throw new IllegalStateException("Claude returned no answer (stop reason: " + reason + ")");
        return text;
    }

}

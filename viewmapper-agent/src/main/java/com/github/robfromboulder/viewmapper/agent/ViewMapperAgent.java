// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent;

import com.github.robfromboulder.viewmapper.agent.discovery.DiscoveryProvider;
import com.github.robfromboulder.viewmapper.agent.tools.AnalyzeSchemaToolExecutor;
import com.github.robfromboulder.viewmapper.agent.tools.ExtractSubgraphToolExecutor;
import com.github.robfromboulder.viewmapper.agent.tools.GenerateMermaidToolExecutor;
import com.github.robfromboulder.viewmapper.agent.tools.ListCatalogsToolExecutor;
import com.github.robfromboulder.viewmapper.agent.tools.ListSchemasToolExecutor;
import com.github.robfromboulder.viewmapper.agent.tools.SuggestEntryPointsToolExecutor;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * LangChain4j-powered agent that intelligently maps relationships between Trino views.
 * <p>
 * Orchestrates tool usage to guide users through complex dependency graphs with agentic reasoning.
 */
public class ViewMapperAgent {

    private final Assistant assistant;

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
        Objects.requireNonNull(config, "Config cannot be null");
        Objects.requireNonNull(analyzer, "Analyzer cannot be null");

        ChatLanguageModel model = AnthropicChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(config.getTimeout())
                .build();

        List<Object> tools = new ArrayList<>();
        if (provider != null) {
            tools.add(new ListCatalogsToolExecutor(provider));
            tools.add(new ListSchemasToolExecutor(provider));
        }
        tools.add(new AnalyzeSchemaToolExecutor(analyzer));
        tools.add(new SuggestEntryPointsToolExecutor(analyzer));
        tools.add(new ExtractSubgraphToolExecutor(analyzer));
        tools.add(new GenerateMermaidToolExecutor(analyzer));

        this.assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .tools(tools.toArray())
                .build();
    }

    /**
     * Constructor for testing with a custom ChatLanguageModel.
     *
     * @param analyzer Dependency analyzer with loaded schema
     * @param provider Provider for catalog/schema discovery (null disables discovery tools)
     * @param model    Custom chat language model (e.g., mock for testing)
     */
    ViewMapperAgent(DependencyAnalyzer analyzer, DiscoveryProvider provider, ChatLanguageModel model) {
        List<Object> tools = new ArrayList<>();
        if (provider != null) {
            tools.add(new ListCatalogsToolExecutor(provider));
            tools.add(new ListSchemasToolExecutor(provider));
        }
        tools.add(new AnalyzeSchemaToolExecutor(analyzer));
        tools.add(new SuggestEntryPointsToolExecutor(analyzer));
        tools.add(new ExtractSubgraphToolExecutor(analyzer));
        tools.add(new GenerateMermaidToolExecutor(analyzer));

        this.assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .tools(tools.toArray())
                .build();
    }

    /**
     * Define directions for agent decisions and tool-calling.
     */
    interface Assistant {
        @SystemMessage("""
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
                   - Call extractSubgraph with appropriate depth
                   - Call generateMermaid with the subgraph result
                   - Return the diagram output directly
                
                TOOL USAGE RULES:
                - analyzeSchema: ALWAYS call this FIRST
                - generateFullSchemaDiagram: Call for SIMPLE schemas when diagram requested (no parameters)
                - suggestEntryPoints: Call for non-SIMPLE schemas
                - extractSubgraph: Call after user selects focus view
                - generateMermaid: Call with subgraph result for non-SIMPLE schemas
                
                RESPONSE REQUIREMENTS:
                - When a tool returns a Mermaid diagram (starts with ```mermaid), include it in your response
                - Do NOT summarize or explain the diagram content without showing the actual diagram code
                - Be concise - let the diagram speak for itself
                - After showing diagram, you may add brief observations
                
                Remember: ALWAYS use tools. NEVER fake or describe output without calling the actual tool.
                """)
        String chat(String userPrompt);
    }

    /**
     * Processes a user prompt.
     *
     * @param userPrompt Natural language prompt or command
     * @return Agent's response with analysis and recommendations
     */
    public String chat(String userPrompt) {
        return assistant.chat(userPrompt);
    }

}

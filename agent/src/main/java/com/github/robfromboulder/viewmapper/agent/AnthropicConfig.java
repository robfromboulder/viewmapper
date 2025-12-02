// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for Anthropic API integration.
 * <p>
 * Loads configuration from environment variables:
 * <ul>
 * <li>ANTHROPIC_API_KEY_FOR_VIEWMAPPER: Agent-specific API key (recommended for production)</li>
 * <li>ANTHROPIC_API_KEY: Generic API key (fallback for development)</li>
 * <li>ANTHROPIC_MODEL: Optional model name (defaults to claude-3-7-sonnet-20250219)</li>
 * <li>ANTHROPIC_TIMEOUT_SECONDS: Optional timeout in seconds (defaults to 60)</li>
 * </ul>
 * <p>
 * <b>Production Best Practice:</b> Use ANTHROPIC_API_KEY_FOR_VIEWMAPPER with a dedicated API key.
 * This enables cost tracking, independent rate limits, and security isolation per agent.
 * <p>
 * <b>Model Selection:</b> Default uses Claude 3.7 Sonnet for reliable tool calling.
 * Haiku may not consistently execute tools in agentic workflows.
 */
public class AnthropicConfig {

    public static final String DEFAULT_MODEL = "claude-3-7-sonnet-20250219";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Builder class supporting chained method calls.
     */
    public static class Builder {
        private String apiKey;
        private String modelName;
        private Duration timeout;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public AnthropicConfig build() {
            return new AnthropicConfig(this);
        }
    }

    /**
     * Creates a builder for manual configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Loads configuration from environment variables.
     * <p>
     * API key lookup hierarchy (tries in order):
     * 1. ANTHROPIC_API_KEY_FOR_VIEWMAPPER (agent-specific, recommended for production)
     * 2. ANTHROPIC_API_KEY (fallback for development/shared environments)
     *
     * @return Configuration loaded from environment
     * @throws IllegalStateException if no API key is found
     */
    public static AnthropicConfig fromEnvironment() {
        // read environment vars
        String apiKey = System.getenv("ANTHROPIC_API_KEY_FOR_VIEWMAPPER");
        if (apiKey == null || apiKey.isBlank()) apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("ANTHROPIC_API_KEY[_FOR_VIEWMAPPER] variables not defined");
        Builder builder = builder().apiKey(apiKey);

        // optional model override
        String model = System.getenv("ANTHROPIC_MODEL");
        if (model != null && !model.isBlank()) builder.modelName(model);

        // optional timeout override
        String timeoutStr = System.getenv("ANTHROPIC_TIMEOUT_SECONDS");
        if (timeoutStr != null && !timeoutStr.isBlank()) {
            try {
                int seconds = Integer.parseInt(timeoutStr);
                builder.timeout(Duration.ofSeconds(seconds));
            } catch (NumberFormatException e) {
                // Ignore invalid timeout, use default
            }
        }

        return builder.build();
    }

    private final String apiKey;
    private final String modelName;
    private final Duration timeout;

    /**
     * Private constructor using builder.
     */
    private AnthropicConfig(Builder builder) {
        this.apiKey = Objects.requireNonNull(builder.apiKey, "API key cannot be null");
        this.modelName = (builder.modelName == null) ? DEFAULT_MODEL : builder.modelName;
        this.timeout = (builder.timeout == null) ? DEFAULT_TIMEOUT : builder.timeout;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public String toString() {
        return String.format("AnthropicConfig{model='%s', timeout=%s, apiKey='***'}", modelName, timeout);
    }

}

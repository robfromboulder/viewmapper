// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Anthropic LLM configuration.
 */
class AnthropicConfigTest {

    @Test
    void testBuilderWithAllFields() {
        AnthropicConfig config = AnthropicConfig.builder().apiKey("test-key").modelName("claude-opus-4").timeout(Duration.ofSeconds(30)).build();
        assertThat(config.getApiKey()).isEqualTo("test-key");
        assertThat(config.getModelName()).isEqualTo("claude-opus-4");
        assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void testBuilderWithDefaults() {
        AnthropicConfig config = AnthropicConfig.builder().apiKey("test-key").build();
        assertThat(config.getApiKey()).isEqualTo("test-key");
        assertThat(config.getModelName()).isEqualTo("claude-3-7-sonnet-20250219");
        assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void testNullApiKey() {
        assertThatThrownBy(() -> AnthropicConfig.builder().build()).isInstanceOf(NullPointerException.class).hasMessageContaining("API key");
    }

    @Test
    void testToStringHidesApiKey() {
        AnthropicConfig config = AnthropicConfig.builder().apiKey("secret-key-12345").build();
        String str = config.toString();
        assertThat(str).doesNotContain("secret-key-12345");
        assertThat(str).contains("***");
        assertThat(str).contains("claude-3-7-sonnet-20250219");
    }

    @Test
    void testCustomModel() {
        AnthropicConfig config = AnthropicConfig.builder().apiKey("test").modelName("claude-haiku-4").build();
        assertThat(config.getModelName()).isEqualTo("claude-haiku-4");
    }

    @Test
    void testCustomTimeout() {
        AnthropicConfig config = AnthropicConfig.builder().apiKey("test").timeout(Duration.ofMinutes(5)).build();
        assertThat(config.getTimeout()).isEqualTo(Duration.ofMinutes(5));
    }

}

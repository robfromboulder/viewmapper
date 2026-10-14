// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent;

import com.anthropic.models.beta.messages.BetaContentBlockParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.github.robfromboulder.viewmapper.agent.FakeMessageService.ToolOutcome;
import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ViewMapperAgent using FakeMessageService.
 * <p>
 * These tests drive the SDK's tool runner without making actual API calls.
 */
class ViewMapperAgentTest {

    private DependencyAnalyzer analyzer;
    private FakeMessageService fake;

    @BeforeEach
    void setUp() {
        analyzer = new DependencyAnalyzer();
        analyzer.addView("b", "SELECT * FROM a");
        analyzer.addView("c", "SELECT * FROM b");
        fake = new FakeMessageService();
    }

    @Test
    void testChatReturnsFinalText() {
        fake.reply(FakeMessageService.text("This is the answer"));
        assertThat(agent().chat("Test prompt")).isEqualTo("This is the answer");
        assertThat(fake.requests()).hasSize(1);
    }

    @Test
    void testRequestCarriesModelSettings() {
        fake.reply(FakeMessageService.text("ok"));
        AnthropicConfig config = AnthropicConfig.builder().apiKey("test").modelName("claude-test").effort("high").build();
        new ViewMapperAgent(analyzer, null, config, fake.service()).chat("Test prompt");

        MessageCreateParams request = fake.requests().getFirst();
        assertThat(request.model().asString()).isEqualTo("claude-test");
        assertThat(request.maxTokens()).isEqualTo(ViewMapperAgent.MAX_TOKENS);
        assertThat(request.outputConfig().flatMap(c -> c.effort()).map(Object::toString)).contains("high");
        assertThat(request.thinking().map(t -> t.isAdaptive())).contains(true);
        assertThat(request._body().toString()).contains("fallbacks");
        assertThat(request.betas().orElseThrow().stream().map(Object::toString)).contains(ViewMapperAgent.FALLBACK_BETA);
    }

    @Test
    void testToolsOfferedWithoutDiscovery() {
        fake.reply(FakeMessageService.text("ok"));
        agent().chat("Test prompt");
        assertThat(toolNames(fake.requests().getFirst())).containsExactlyInAnyOrder(
                "analyzeSchema", "suggestEntryPoints", "extractSubgraph", "generateFullSchemaDiagram", "generateMermaid");
    }

    @Test
    void testToolCallReturnsResultAndThinking() {
        fake.reply(FakeMessageService.toolUse("call_1", "analyzeSchema", Map.of("schemaName", "test")));
        fake.reply(FakeMessageService.text("The schema has 3 views"));
        assertThat(agent().chat("How big is the schema?")).isEqualTo("The schema has 3 views");

        // the second request returns the tool result, after the assistant turn with its thinking block intact
        assertThat(fake.requests()).hasSize(2);
        ToolOutcome outcome = fake.toolResults(1).getFirst();
        assertThat(outcome.isError()).isFalse();
        assertThat(outcome.content()).contains("\"viewCount\":3");
        List<BetaContentBlockParam> assistant = fake.requests().get(1).messages().get(1).content().asBetaContentBlockParams();
        assertThat(assistant.getFirst().thinking().map(t -> t.signature())).contains("sig-call_1");
    }

    @Test
    void testToolFailureReturnsError() {
        Map<String, Object> input = Map.of("focusView", "missing", "depthUpstream", 1, "depthDownstream", 1, "maxNodes", 50);
        fake.reply(FakeMessageService.toolUse("call_1", "generateMermaid", input));
        fake.reply(FakeMessageService.text("That view doesn't exist"));
        agent().chat("Draw missing");
        ToolOutcome outcome = fake.toolResults(1).getFirst();
        assertThat(outcome.isError()).isTrue();
        assertThat(outcome.content()).contains("missing");
    }

    @Test
    void testRefusalThrows() {
        fake.reply(FakeMessageService.refusal("cyber"));
        assertThatThrownBy(() -> agent().chat("Test prompt")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("declined").hasMessageContaining("cyber");
    }

    @Test
    void testTruncatedAnswerThrows() {
        fake.reply(FakeMessageService.truncated("```mermaid\ngraph TB\n"));
        assertThatThrownBy(() -> agent().chat("Test prompt")).isInstanceOf(IllegalStateException.class).hasMessageContaining("cut off");
    }

    @Test
    void testRunawayToolLoopThrows() {
        for (int i = 0; i < ViewMapperAgent.MAX_ITERATIONS; i++) {
            fake.reply(FakeMessageService.toolUse("call_" + i, "analyzeSchema", Map.of("schemaName", "test")));
        }
        assertThatThrownBy(() -> agent().chat("Test prompt")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("still calling tools");
        assertThat(fake.requests()).hasSize(ViewMapperAgent.MAX_ITERATIONS);
    }

    private ViewMapperAgent agent() {
        return new ViewMapperAgent(analyzer, null, AnthropicConfig.builder().apiKey("test").build(), fake.service());
    }

    private static List<String> toolNames(MessageCreateParams request) {
        return request.tools().orElseThrow().stream().map(t -> t.asBetaTool().name()).toList();
    }

}

// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent;

import com.anthropic.core.ObjectMappers;
import com.anthropic.core.RequestOptions;
import com.anthropic.helpers.BetaRunnableTool;
import com.anthropic.helpers.BetaToolRunner;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaMessageParam;
import com.anthropic.models.beta.messages.BetaToolResultBlockParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.anthropic.models.beta.messages.ToolRunnerCreateParams;
import com.anthropic.services.blocking.beta.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fake message service for testing without API calls.
 * <p>
 * Replays scripted responses in order and records every request it receives.
 */
public class FakeMessageService {

    /**
     * Content and error flag of one tool result.
     */
    public record ToolOutcome(String content, boolean isError) {
    }

    private final List<MessageCreateParams> requests = new ArrayList<>();
    private final Deque<String> responses = new ArrayDeque<>();

    /**
     * Calls one tool the way the model does, with JSON arguments, and returns its result.
     *
     * @param tools     Tools offered to the model
     * @param name      Name of the tool to call
     * @param arguments Arguments to serialize as the tool input
     * @return Tool result the runner sends back to the model
     */
    public static ToolOutcome callTool(List<BetaRunnableTool> tools, String name, Map<String, Object> arguments) {
        FakeMessageService fake = new FakeMessageService();
        fake.reply(toolUse("call_1", name, arguments));
        fake.reply(text("done"));
        MessageCreateParams.Builder params = MessageCreateParams.builder().model("test").maxTokens(1000).addUserMessage("test");
        tools.forEach(params::addTool);
        ToolRunnerCreateParams runnerParams = ToolRunnerCreateParams.builder().initialMessageParams(params.build()).build();
        for (BetaMessage ignored : new BetaToolRunner(fake.service(), runnerParams, RequestOptions.none())) {
            // drain the runner
        }
        return fake.toolResults(1).getFirst();
    }

    /**
     * Returns a response that ends the turn with text.
     */
    public static String text(String text) {
        return message("end_turn", List.of(Map.of("type", "text", "text", text)), null);
    }

    /**
     * Returns a response that calls one tool, preceded by a thinking block that must be returned unchanged.
     */
    public static String toolUse(String id, String name, Map<String, Object> input) {
        return message("tool_use", List.of(
                Map.of("type", "thinking", "thinking", "", "signature", "sig-" + id),
                Map.of("type", "tool_use", "id", id, "name", name, "input", input)), null);
    }

    /**
     * Returns a response that declines with the given category.
     */
    public static String refusal(String category) {
        return message("refusal", List.of(), Map.of("type", "refusal", "category", category));
    }

    /**
     * Returns a response that ran out of output tokens.
     */
    public static String truncated(String text) {
        return message("max_tokens", List.of(Map.of("type", "text", "text", text)), null);
    }

    /**
     * Queues a response for the next request.
     */
    public FakeMessageService reply(String json) {
        responses.add(json);
        return this;
    }

    /**
     * Returns every request received, in order.
     */
    public List<MessageCreateParams> requests() {
        return requests;
    }

    /**
     * Returns the message service backed by the queued responses.
     */
    public MessageService service() {
        Class<?>[] types = {MessageService.class};
        return (MessageService) Proxy.newProxyInstance(MessageService.class.getClassLoader(), types, (proxy, method, args) -> {
            if (method.getName().equals("create") && args != null && args[0] instanceof MessageCreateParams params) {
                requests.add(params);
                if (responses.isEmpty()) throw new IllegalStateException("No scripted response left");
                return ObjectMappers.jsonMapper().readValue(responses.removeFirst(), BetaMessage.class);
            } else if (method.getName().equals("toString")) {
                return "FakeMessageService";
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    /**
     * Returns the tool results sent with the given request.
     */
    public List<ToolOutcome> toolResults(int request) {
        BetaMessageParam last = requests.get(request).messages().getLast();
        return last.content().asBetaContentBlockParams().stream()
                .flatMap(block -> block.toolResult().stream())
                .map(r -> new ToolOutcome(r.content().map(FakeMessageService::contentText).orElse(""), r.isError().orElse(false)))
                .toList();
    }

    private static String contentText(BetaToolResultBlockParam.Content content) {
        if (content.isString()) return content.asString();
        return content.asBlocks().stream()
                .flatMap(block -> block.text().stream())
                .map(t -> t.text())
                .reduce("", String::concat);
    }

    private static String message(String stopReason, List<Map<String, Object>> content, Map<String, Object> stopDetails) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("id", "msg_test");
        message.put("type", "message");
        message.put("role", "assistant");
        message.put("model", "test");
        message.put("content", content);
        message.put("stop_reason", stopReason);
        message.put("stop_sequence", null);
        message.put("stop_details", stopDetails);
        message.put("usage", Map.of("input_tokens", 1, "output_tokens", 1));
        try {
            return ObjectMappers.jsonMapper().writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

}

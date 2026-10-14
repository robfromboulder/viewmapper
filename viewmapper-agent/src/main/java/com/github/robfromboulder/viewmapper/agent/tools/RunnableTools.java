// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.anthropic.helpers.BetaRunnableTool;
import com.anthropic.models.beta.messages.BetaTool;
import com.anthropic.models.beta.messages.BetaToolResultBlockParam;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Helpers for exposing tool methods to the SDK's tool runner.
 */
final class RunnableTools {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RunnableTools() {
    }

    /**
     * Converts a tool method's return value into tool result content, serializing anything but a string as JSON.
     *
     * @param result Value returned by a tool method
     * @return Tool result content
     */
    static BetaToolResultBlockParam.Content result(Object result) {
        if (result instanceof String text) return BetaToolResultBlockParam.Content.ofString(text);
        try {
            return BetaToolResultBlockParam.Content.ofString(MAPPER.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize tool result: " + e.getMessage(), e);
        }
    }

    /**
     * Declares a tool that takes no arguments.
     * <p>
     * The SDK can't derive these from an arguments class, since it requires at least one property.
     *
     * @param name        Tool name the model calls
     * @param description Tool description the model reads
     * @param method      Tool method to run
     * @return Runnable tool
     */
    static BetaRunnableTool withoutArguments(String name, String description, Supplier<Object> method) {
        BetaTool.InputSchema.Properties none = BetaTool.InputSchema.Properties.builder().putAllAdditionalProperties(Map.of()).build();
        BetaTool definition = BetaTool.builder()
                .name(name)
                .description(description)
                .inputSchema(BetaTool.InputSchema.builder().properties(none).build())
                .build();
        return BetaRunnableTool.of(definition, input -> result(method.get()));
    }

}

// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock ChatLanguageModel for testing without API calls.
 * <p>
 * Allows configuring canned responses for specific keywords, and supports tools by implementing the overloaded generate() method.
 */
public class MockChatLanguageModel implements ChatLanguageModel {

    private int callCount = 0;
    private final Map<String, String> cannedResponses = new HashMap<>();
    private String defaultResponse = "Mock response";

    /**
     * Returns the number of times generate() was called.
     */
    public int getCallCount() {
        return callCount;
    }

    /**
     * Sets the default response when no specific response is configured.
     */
    public void setDefaultResponse(String response) {
        this.defaultResponse = response;
    }

    /**
     * Configures a canned response for any input containing the keyword.
     */
    public void setWhenContains(String keyword, String output) {
        cannedResponses.put("*" + keyword + "*", output);
    }

    /**
     * Generate a response to the specified messages.
     */
    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        callCount++;

        // check at least one message is present
        if (messages.isEmpty()) return Response.from(AiMessage.from(defaultResponse));

        // check for exact match
        String lastMessage = messages.getLast().toString();
        if (cannedResponses.containsKey(lastMessage)) return Response.from(AiMessage.from(cannedResponses.get(lastMessage)));

        // check for keyword matches
        for (Map.Entry<String, String> entry : cannedResponses.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("*") && key.endsWith("*")) {
                String keyword = key.substring(1, key.length() - 1);
                if (lastMessage.contains(keyword)) return Response.from(AiMessage.from(entry.getValue()));
            }
        }

        return Response.from(AiMessage.from(defaultResponse));
    }

    /**
     * Generate a response to the specified messages using tools.
     */
    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        return generate(messages);
    }

}

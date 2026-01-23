// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock ChatModel for testing without API calls.
 * <p>
 * Allows configuring canned responses for specific keywords.
 */
public class MockChatModel implements ChatModel {

    private int callCount = 0;
    private final Map<String, String> cannedResponses = new HashMap<>();
    private String defaultResponse = "Mock response";

    /**
     * Returns the number of times doChat() was called.
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
     * Implementation of the ChatModel interface for testing.
     */
    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        callCount++;

        List<ChatMessage> messages = chatRequest.messages();

        // check at least one message is present
        if (messages.isEmpty()) {
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(defaultResponse))
                    .build();
        }

        // get the last message
        String lastMessage = messages.getLast().toString();

        // check for exact match
        if (cannedResponses.containsKey(lastMessage)) {
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(cannedResponses.get(lastMessage)))
                    .build();
        }

        // check for keyword matches
        for (Map.Entry<String, String> entry : cannedResponses.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("*") && key.endsWith("*")) {
                String keyword = key.substring(1, key.length() - 1);
                if (lastMessage.contains(keyword)) {
                    return ChatResponse.builder()
                            .aiMessage(AiMessage.from(entry.getValue()))
                            .build();
                }
            }
        }

        return ChatResponse.builder()
                .aiMessage(AiMessage.from(defaultResponse))
                .build();
    }

}

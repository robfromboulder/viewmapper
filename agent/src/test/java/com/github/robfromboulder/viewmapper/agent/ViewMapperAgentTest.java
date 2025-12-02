// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent;

import com.github.robfromboulder.viewmapper.parser.DependencyAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ViewMapperAgent using MockChatLanguageModel.
 * <p>
 * These tests verify agent behavior without making actual API calls.
 */
class ViewMapperAgentTest {

    private ViewMapperAgent agent;
    private DependencyAnalyzer analyzer;
    private MockChatLanguageModel mockModel;

    @BeforeEach
    void setUp() {
        analyzer = new DependencyAnalyzer();
        mockModel = new MockChatLanguageModel();
        agent = new ViewMapperAgent(analyzer, mockModel);
    }

    @Test
    void testChatWithMockResponse() {
        mockModel.setDefaultResponse("This is a mock response from the agent");
        String response = agent.chat("Test query");
        assertThat(response).isEqualTo("This is a mock response from the agent");
        assertThat(mockModel.getCallCount()).isEqualTo(1);
    }

    @Test
    void testChatWithKeywordResponse() {
        mockModel.setWhenContains("analyze", "I will analyze the schema for you");
        mockModel.setWhenContains("diagram", "Here is your diagram");
        String response1 = agent.chat("Please analyze the schema");
        assertThat(response1).contains("analyze the schema");
        String response2 = agent.chat("Generate a diagram");
        assertThat(response2).contains("diagram");
        assertThat(mockModel.getCallCount()).isEqualTo(2);
    }

    @Test
    void testMultipleInteractions() {
        mockModel.setDefaultResponse("Mock response");
        agent.chat("First query");
        agent.chat("Second query");
        agent.chat("Third query");
        assertThat(mockModel.getCallCount()).isEqualTo(3);
    }

    @Test
    void testWithPopulatedAnalyzer() {
        for (int i = 1; i <= 10; i++) analyzer.addView("view" + i, "SELECT * FROM base");
        mockModel.setDefaultResponse("I see you have views in the schema");
        String response = agent.chat("How many views?");
        assertThat(response).contains("views in the schema");
        assertThat(analyzer.getViewCount()).isEqualTo(11); // 10 views + base
    }

    @Test
    void testEmptyQuery() {
        mockModel.setDefaultResponse("Please provide a query");
        String response = agent.chat("empty"); // empty string is still a valid query, though not useful
        assertThat(response).isNotEmpty();
    }

}

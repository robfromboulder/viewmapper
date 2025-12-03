# © 2024-2025 Rob Dickinson (robfromboulder)

"""
Unit tests for ViewMapper MCP Server

Tests tool registration, error handling, and context management without requiring
actual Java CLI execution.
"""

import os
from unittest.mock import MagicMock, patch

import pytest

# Set required environment variables before importing mcp_server
os.environ["ANTHROPIC_API_KEY_FOR_VIEWMAPPER"] = "fake-api-key"
os.environ["VIEWMAPPER_JAR"] = "/fake/path/viewmapper.jar"

from mcp_server import (
    build_prompt_with_history,
    call_tool,
    conversation_histories,
    get_session_id,
    list_tools,
)


class TestToolRegistration:
    """Test that tools are registered correctly."""

    @pytest.mark.asyncio
    async def test_list_tools_returns_single_tool(self):
        """Verify exactly one tool is registered."""
        tools = await list_tools()
        assert len(tools) == 1

    @pytest.mark.asyncio
    async def test_tool_has_correct_name(self):
        """Verify tool name matches expected value."""
        tools = await list_tools()
        assert tools[0].name == "explore_trino_views"

    @pytest.mark.asyncio
    async def test_tool_requires_query(self):
        """Verify tool schema requires query parameter."""
        tools = await list_tools()
        schema = tools[0].inputSchema
        assert "query" in schema["properties"]
        assert set(schema["required"]) == {"query"}


class TestPromptBuilding:
    """Test conversation history management and prompt building."""

    def test_build_prompt_without_history(self):
        """When no history, return query unchanged."""
        result = build_prompt_with_history([], "Show me the diagram")
        assert result == "Show me the diagram"

    def test_build_prompt_with_single_turn(self):
        """Include previous turn in prompt."""
        history = [
            {"role": "user", "content": "What are the views?"},
            {"role": "assistant", "content": "There are 11 views in the schema."}
        ]
        result = build_prompt_with_history(history, "Show me the diagram")

        assert "Previous conversation:" in result
        assert "User: What are the views?" in result
        assert "Assistant: There are 11 views in the schema." in result
        assert "Current question: Show me the diagram" in result

    def test_build_prompt_truncates_long_responses(self):
        """Long assistant responses should be truncated."""
        long_response = "A" * 500
        history = [
            {"role": "user", "content": "Analyze schema"},
            {"role": "assistant", "content": long_response}
        ]
        result = build_prompt_with_history(history, "Next question")

        assert len(result) < len(long_response) + 100
        assert "..." in result

    def test_build_prompt_limits_history_turns(self):
        """Only include last 3 turns (6 messages) to avoid context bloat."""
        # Create 5 turns (10 messages)
        history = []
        for i in range(5):
            history.append({"role": "user", "content": f"Question {i}"})
            history.append({"role": "assistant", "content": f"Answer {i}"})

        result = build_prompt_with_history(history, "Final question")

        # Should only include turns 2, 3, 4 (not 0, 1)
        assert "Question 0" not in result
        assert "Question 1" not in result
        assert "Question 2" in result
        assert "Question 3" in result
        assert "Question 4" in result

    def test_get_session_id_returns_default(self):
        """For now, always return default session."""
        assert get_session_id() == "default"


class TestToolExecution:
    """Test tool execution with mocked subprocess calls."""

    @pytest.mark.asyncio
    async def test_unknown_tool_returns_error(self):
        """Calling unknown tool returns error message."""
        result = await call_tool("unknown_tool", {})
        assert len(result) == 1
        assert "Unknown tool" in result[0].text

    @pytest.mark.asyncio
    async def test_missing_query_returns_error(self):
        """Missing query parameter returns error."""
        result = await call_tool("explore_trino_views", {})
        assert len(result) == 1
        assert "query" in result[0].text.lower()
        assert "required" in result[0].text.lower()

    @pytest.mark.asyncio
    @patch("mcp_server.subprocess.run")
    async def test_successful_execution(self, mock_subprocess):
        """Successful Java CLI execution returns response."""
        # Setup mocks
        mock_subprocess.return_value = MagicMock(
            returncode=0,
            stdout="Here is your diagram:\n```mermaid\ngraph TB\n```",
            stderr=""
        )

        # Clear conversation history
        conversation_histories.clear()

        # Execute
        result = await call_tool("explore_trino_views", {
            "query": "Show me the diagram"
        })

        # Verify
        assert len(result) == 1
        assert "mermaid" in result[0].text
        assert mock_subprocess.called

        # Verify command structure
        call_args = mock_subprocess.call_args
        cmd = call_args[0][0]
        assert "-jar" in cmd
        assert "run" in cmd
        assert "--connection" in cmd

    @pytest.mark.asyncio
    @patch("mcp_server.subprocess.run")
    async def test_java_error_handling(self, mock_subprocess):
        """Java CLI errors are properly formatted."""
        mock_subprocess.return_value = MagicMock(
            returncode=1,
            stdout="",
            stderr="Error: Invalid SQL syntax"
        )

        result = await call_tool("explore_trino_views", {
            "query": "Show diagram"
        })

        assert len(result) == 1
        assert "ViewMapper Error" in result[0].text
        assert "Invalid SQL syntax" in result[0].text

    @pytest.mark.asyncio
    @patch("mcp_server.subprocess.run")
    async def test_timeout_handling(self, mock_subprocess):
        """Timeout errors are properly formatted."""
        mock_subprocess.side_effect = subprocess.TimeoutExpired("java", 60)

        result = await call_tool("explore_trino_views", {
            "query": "Show diagram"
        })

        assert len(result) == 1
        assert "timed out" in result[0].text.lower()

    @pytest.mark.asyncio
    @patch("mcp_server.subprocess.run")
    async def test_conversation_history_accumulates(self, mock_subprocess):
        """Verify conversation history builds up across calls."""
        mock_subprocess.return_value = MagicMock(
            returncode=0,
            stdout="Response text",
            stderr=""
        )

        # Clear history
        conversation_histories.clear()

        # First call
        await call_tool("explore_trino_views", {
            "query": "First question"
        })

        # Verify history has one turn
        session_id = get_session_id()
        assert len(conversation_histories[session_id]) == 2  # User + assistant

        # Second call
        await call_tool("explore_trino_views", {
            "query": "Second question"
        })

        # Verify history has two turns
        assert len(conversation_histories[session_id]) == 4  # 2 turns × 2 messages

        # Verify the enhanced prompt includes history
        second_call_args = mock_subprocess.call_args_list[1]
        cmd = second_call_args[0][0]
        # The prompt is the argument after "run"
        run_index = cmd.index("run")
        prompt = cmd[run_index + 1]
        assert "Previous conversation:" in prompt
        assert "First question" in prompt


# Need to import subprocess for the TimeoutExpired exception
import subprocess

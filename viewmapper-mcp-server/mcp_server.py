#!/usr/bin/env python3
# © 2024-2025 Rob Dickinson (robfromboulder)

"""
ViewMapper MCP Server

Exposes ViewMapper Java CLI as an MCP tool for Claude Desktop.
Maintains conversation context across multiple turns for natural schema exploration.
"""

import asyncio
import os
import subprocess

from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import TextContent, Tool

# Server instance
app = Server("viewmapper")

# Conversation history storage
# Key: session_id (we'll use a simple default session for now)
# Value: list of {"role": "user"|"assistant", "content": str}
conversation_histories: dict[str, list[dict[str, str]]] = {}

# Configuration from environment
VIEWMAPPER_JAR = os.getenv("VIEWMAPPER_JAR")
if not VIEWMAPPER_JAR:
    raise ValueError(
        "VIEWMAPPER_JAR environment variable is required. "
        "Set it to the path of your viewmapper JAR file."
    )

ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY_FOR_VIEWMAPPER") or os.getenv("ANTHROPIC_API_KEY")
if not ANTHROPIC_API_KEY:
    raise ValueError(
        "ANTHROPIC_API_KEY_FOR_VIEWMAPPER or ANTHROPIC_API_KEY environment variable is required. "
        "This is passed to the ViewMapper Java agent."
    )

CONNECTION = os.getenv("VIEWMAPPER_CONNECTION", "test://simple_ecommerce")
VERBOSE = os.getenv("VIEWMAPPER_VERBOSE", "").lower() in ("1", "true", "yes")


def get_session_id() -> str:
    """
    Get session ID for conversation tracking.

    For now, using a single default session since MCP doesn't provide explicit session IDs.
    Future enhancement: Extract session info from MCP context if available.
    """
    return "default"


def build_prompt_with_history(history: list[dict[str, str]], current_query: str) -> str:
    """
    Build enhanced prompt that includes conversation history.

    Format:
        [Previous conversation:]
        User: <previous query>
        Assistant: <previous response summary>
        ...

        Current question: <current query>

    Args:
        history: List of previous conversation turns
        current_query: Current user query

    Returns:
        Enhanced prompt string with context
    """
    if not history:
        return current_query

    prompt_parts = ["Previous conversation:"]

    # Include last N turns (avoid overwhelming context)
    max_history_turns = 3
    recent_history = history[-(max_history_turns * 2):]  # Each turn = user + assistant message

    for msg in recent_history:
        role = "User" if msg["role"] == "user" else "Assistant"
        content = msg["content"]

        # Truncate long assistant responses (keep first 200 chars)
        if msg["role"] == "assistant" and len(content) > 200:
            content = content[:200] + "..."

        prompt_parts.append(f"{role}: {content}")

    prompt_parts.append("")
    prompt_parts.append(f"Current question: {current_query}")

    return "\n".join(prompt_parts)


@app.list_tools()
async def list_tools() -> list[Tool]:
    """Register ViewMapper tool with MCP."""
    return [
        Tool(
            name="explore_trino_views",
            description=(
                "Explore Trino view dependencies with AI-powered dependency analysis and Mermaid diagram generation. "
                "The agent intelligently guides you through complex view hierarchies, suggests entry points, and creates focused visualizations. "
                "Maintains conversation context for natural multi-turn exploration."
            ),
            inputSchema={
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": (
                            "Natural language question about the schema. Examples:\n"
                            "- 'Show me the full dependency diagram'\n"
                            "- 'What are the high-impact views?'\n"
                            "- 'Focus on customer_360 view with 2 levels upstream'\n"
                            "- 'What are the leaf views?'"
                        )
                    }
                },
                "required": ["query"]
            }
        )
    ]


@app.call_tool()
async def call_tool(name: str, arguments: dict) -> list[TextContent]:
    """
    Handle tool invocation from Claude Desktop.

    Args:
        name: Tool name (should be "explore_trino_views")
        arguments: Tool arguments (query)

    Returns:
        List containing single TextContent with agent response
    """
    if name != "explore_trino_views":
        return [TextContent(
            type="text",
            text=f"❌ Unknown tool: {name}"
        )]

    # Extract arguments
    query = arguments.get("query")

    if not query:
        return [TextContent(
            type="text",
            text="❌ Error: 'query' parameter is required"
        )]

    # Get session and build prompt with history
    session_id = get_session_id()
    history = conversation_histories.get(session_id, [])
    enhanced_query = build_prompt_with_history(history, query)

    # Build Java CLI command
    cmd = [
        "java", "-jar", VIEWMAPPER_JAR,
        "run",
        enhanced_query,
        "--connection", CONNECTION,
        "--output", "text"
    ]
    if VERBOSE:
        cmd.append("--verbose")

    # Execute Java CLI command
    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=60,
            env={
                **os.environ,
                "ANTHROPIC_API_KEY": ANTHROPIC_API_KEY
            }
        )

        if result.returncode != 0:
            error_msg = result.stderr.strip() or "Unknown error"
            return [TextContent(
                type="text",
                text=f"❌ ViewMapper Error:\n{error_msg}"
            )]

        response = result.stdout.strip()

        # Update conversation history
        history.append({"role": "user", "content": query})
        history.append({"role": "assistant", "content": response})
        conversation_histories[session_id] = history

        # Return response directly (includes any Mermaid diagrams)
        return [TextContent(type="text", text=response)]

    except subprocess.TimeoutExpired:
        return [TextContent(
            type="text",
            text="⏱️ Request timed out after 60 seconds. Try a simpler query or smaller dataset."
        )]
    except FileNotFoundError:
        return [TextContent(
            type="text",
            text=f"❌ Error: Java or JAR file not found. Check VIEWMAPPER_JAR={VIEWMAPPER_JAR}"
        )]
    except Exception as e:
        return [TextContent(
            type="text",
            text=f"❌ Unexpected error: {str(e)}"
        )]


async def main():
    """Run the MCP server using stdio transport."""
    async with stdio_server() as (read_stream, write_stream):
        await app.run(
            read_stream,
            write_stream,
            app.create_initialization_options()
        )


if __name__ == "__main__":
    asyncio.run(main())

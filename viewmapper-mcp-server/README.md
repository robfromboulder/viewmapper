# viewmapper-mcp-server
This module exposes the ViewMapper agent as an MCP tool for Claude Desktop.

## Overview

This MCP server enables Claude Desktop to explore Trino database schemas using the ViewMapper agent. It maintains conversation context across multiple turns, allowing natural schema exploration with AI-powered dependency analysis and Mermaid diagram generation.

## Architecture

```
┌──────────────────┐
│  Claude Desktop  │
└────────┬─────────┘
         │ stdio (JSON-RPC)
         ↓
┌──────────────────┐
│  Python MCP      │  ← This module (conversation context management)
│  Server          │
└────────┬─────────┘
         │ subprocess
         ↓
┌──────────────────┐
│  Java CLI        │  ← ViewMapper agent (LangChain4j + Trino parser)
│  (viewmapper)    │
└──────────────────┘
```

## Prerequisites

1. **Java 24** - Required to run ViewMapper CLI
2. **Python 3.10+** - Required for MCP server
3. **uv** - Python package manager (recommended) or pip
4. **ViewMapper JAR** - Built from `../viewmapper-agent/` directory
5. **Anthropic API Key** - For Claude agent in Java CLI

## Installation

### 1. Build ViewMapper JAR

```bash
cd ../viewmapper-agent
mvn clean package
# Produces: target/viewmapper-478.jar
```

### 2. Set Up Python Environment

```bash
cd viewmapper-mcp-server

# Using uv (recommended)
uv venv
uv pip install -e ".[dev]"  # Includes pytest for testing

# Or using pip
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -e ".[dev]"
```

### 3. Configure Environment Variables

Set these environment variables in your shell or Claude Desktop configuration:

```bash
# Required: Anthropic API key (passed to Java agent)
export ANTHROPIC_API_KEY_FOR_VIEWMAPPER="sk-ant-..."

# Optional: Connection string (default: test://simple_ecommerce)
export VIEWMAPPER_CONNECTION="test://simple_ecommerce"

# Required: Path to ViewMapper JAR
export VIEWMAPPER_JAR="/path/to/viewmapper/viewmapper-agent/target/viewmapper-478.jar"

# Optional: Enable verbose logging
export VIEWMAPPER_VERBOSE="1"
```

### 4. Configure Claude Desktop

Edit Claude Desktop's configuration file:

**macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
**Linux:** `~/.config/Claude/claude_desktop_config.json`

Add the ViewMapper MCP server:

**Option A: Using venv (recommended for stability)**

```json
{
  "mcpServers": {
    "viewmapper": {
      "command": "/absolute/path/to/viewmapper/viewmapper-mcp-server/venv/bin/python",
      "args": [
        "mcp_server.py"
      ],
      "cwd": "/absolute/path/to/viewmapper/viewmapper-mcp-server",
      "env": {
        "ANTHROPIC_API_KEY_FOR_VIEWMAPPER": "sk-ant-your-key-here",
        "VIEWMAPPER_CONNECTION": "test://simple_ecommerce",
        "VIEWMAPPER_JAR": "/absolute/path/to/viewmapper/viewmapper-agent/target/viewmapper-478.jar"
      }
    }
  }
}
```

**Option B: Using uv**

```json
{
  "mcpServers": {
    "viewmapper": {
      "command": "uv",
      "args": [
        "--directory",
        "/absolute/path/to/viewmapper/viewmapper-mcp-server",
        "run",
        "python",
        "mcp_server.py"
      ],
      "env": {
        "ANTHROPIC_API_KEY_FOR_VIEWMAPPER": "sk-ant-your-key-here",
        "VIEWMAPPER_CONNECTION": "test://simple_ecommerce",
        "VIEWMAPPER_JAR": "/absolute/path/to/viewmapper/viewmapper-agent/target/viewmapper-478.jar"
      }
    }
  }
}
```

**Important:** Use absolute paths, not relative paths or `~`.

### 5. Restart Claude Desktop

After editing the configuration, restart Claude Desktop completely (not just close/reopen a chat window).

## Usage

### Basic Commands

Once configured, you can use ViewMapper in Claude Desktop:

```
Show me the full dependency diagram
```

```
What are the high-impact entry points?
```

```
Show me 2 levels upstream and 1 level downstream from customer_360 view
```

### Multi-Turn Conversations

The MCP server maintains conversation context, enabling natural follow-up questions:

```
User: What are the leaf views?
Claude: [Lists leaf views...]

User: Show me the diagram for the first one
Claude: [Generates diagram without needing to re-specify dataset...]
```

### Example Datasets

The ViewMapper project includes 4 test datasets embedded in the JAR. Access via `VIEWMAPPER_CONNECTION`:

1. **test://simple_ecommerce** (11 views) - SIMPLE complexity
2. **test://moderate_analytics** (35 views) - MODERATE complexity
3. **test://complex_enterprise** (154 views) - COMPLEX complexity
4. **test://realistic_bi_warehouse** (86 views) - COMPLEX complexity

See `../viewmapper-agent/src/main/resources/datasets/README.md` for detailed descriptions and example queries.

## Testing

### Run Unit Tests

```bash
# Using pytest (recommended)
pytest

# With verbose output
pytest -v

# Run specific test file
pytest tests/test_mcp_server.py

# Run specific test
pytest tests/test_mcp_server.py::TestToolRegistration::test_list_tools_returns_single_tool
```

### Manual Testing with Claude Desktop

1. Configure Claude Desktop (see Installation step 4)
2. Restart Claude Desktop
3. Open a new chat
4. Try a simple query:
   ```
   Show me the full dependency diagram
   ```
5. Verify Claude responds with schema analysis and/or diagram

## Features

### Conversation Context Management

The MCP server automatically maintains conversation history across turns:

- **Context Window:** Last 3 turns (6 messages) included in prompts
- **Response Truncation:** Long assistant responses truncated to 200 chars in history
- **Session Isolation:** Future enhancement - currently uses single default session

### Error Handling

The server provides clear error messages for common issues:

- ❌ Missing environment variables (JAR path, API key)
- ❌ Dataset file not found
- ❌ Java CLI errors (with stderr output)
- ⏱️ Timeout after 60 seconds

### Output Format

The Java agent returns responses that may include:

- Natural language explanations
- Embedded Mermaid diagrams (` ```mermaid...``` `)
- Analysis results (complexity, entry points, etc.)

Claude Desktop automatically renders Mermaid diagrams inline.

## Architecture Details

### Stateful Context Management (Option A)

The MCP server uses **prompt-based context enhancement**:

1. Maintains conversation history in memory (per session)
2. Builds enhanced prompt: `Previous conversation: ... Current question: ...`
3. Passes enhanced prompt to Java CLI
4. Java agent treats it as extended user input

**Trade-offs:**
- ✅ Simple implementation (no Java changes needed)
- ✅ Works immediately with existing agent
- ✅ Easy to debug (history visible in prompts)
- ⚠️ History grows unbounded (mitigated by limiting to 3 turns)
- ⚠️ Not as clean as structured context

**Future Enhancement:** Implement structured context (Option B) by adding `--context` parameter to Java CLI and using LangChain4j's `ChatMemory`.

### Tool Interface

**Single tool:** `explore_trino_views`

**Parameters:**
- `query` (required) - Natural language question

**Connection:**
- Dataset selected via `VIEWMAPPER_CONNECTION` environment variable
- Default: `test://simple_ecommerce`
- Live Trino connections (`jdbc:...`) not yet supported

## Troubleshooting

### Claude Desktop doesn't see the tool

1. Check MCP server is configured correctly in `claude_desktop_config.json`
2. Verify absolute paths (not `~` or relative)
3. Restart Claude Desktop completely
4. Check Claude Desktop logs (Help → View Logs)

### "VIEWMAPPER_JAR not found" error

Verify the environment variable points to the correct JAR:

```bash
ls -la "$VIEWMAPPER_JAR"
# Should show: viewmapper-478.jar
```

### "API key required" error

Ensure API key is set:

```bash
echo $ANTHROPIC_API_KEY_FOR_VIEWMAPPER
# Should print: sk-ant-...
```

### Java errors or timeouts

Enable verbose mode to see detailed Java output:

```json
{
  "mcpServers": {
    "viewmapper": {
      "env": {
        "VIEWMAPPER_VERBOSE": "1"
      }
    }
  }
}
```

Check Claude Desktop logs for stderr output.

### Conversation context not working

The context feature uses a simple default session. If conversations aren't connected:

1. Verify multiple messages are being sent in same chat window
2. Enable verbose mode to see enhanced prompts
3. Check Claude Desktop logs for errors

## Development

### Project Structure

```
viewmapper-mcp-server/
├── pyproject.toml          # uv/pip configuration
├── mcp_server.py           # Main server implementation
├── tests/
│   ├── __init__.py
│   └── test_mcp_server.py  # Unit tests (mocked subprocess)
└── README.md               # This file
```

### Code Style

- Python 3.10+ type hints
- Async/await for MCP handlers
- Comprehensive error handling
- Clear error messages with emoji indicators (❌, ⏱️)

### Testing Strategy

**Unit Tests:**
- Mock subprocess calls (no Java execution)
- Test tool registration, parameter validation
- Test error handling, timeout handling
- Test conversation history management

**Manual Testing:**
- Actual Claude Desktop integration
- Real Java CLI execution
- End-to-end workflow validation

### Adding New Features

**To add new tool parameters:**

1. Update `inputSchema` in `list_tools()`
2. Extract parameter in `call_tool()`
3. Add validation and error handling
4. Pass to Java CLI via command-line argument
5. Add unit tests

**To enhance context management:**

1. Modify `build_prompt_with_history()` for different formatting
2. Adjust `MAX_HISTORY_TURNS` to change context window
3. Add session isolation logic in `get_session_id()`

## Limitations

1. **No live Trino connections** - Only test datasets supported (Java CLI limitation)
2. **Single session** - All conversations share same history (future: multi-session)
3. **Unbounded dataset size** - Large datasets may timeout or overwhelm agent
4. **Prompt-based context** - Not as elegant as structured context (future: Option B)
5. **Static dataset selection** - Must restart Claude Desktop to change VIEWMAPPER_CONNECTION

## Future Enhancements

### Planned Features

1. **Live Trino Support** - When Java CLI adds JDBC integration
2. **Multi-Session Context** - Isolate conversations by MCP session ID
3. **Structured Context (Option B)** - Add `--context` parameter to Java CLI
4. **Response Caching** - Cache analysis results to avoid re-computation
5. **Streaming Output** - Stream long responses instead of blocking

### Upgrade Path to Option B

When ready to implement structured context:

1. Add `--context` parameter to `RunCommand.java`
2. Modify `ViewMapperAgent` to use LangChain4j's `ChatMemory`
3. Update MCP server to pass JSON context instead of text
4. Maintain backward compatibility with prompt-based approach

## License

© 2024-2025 Rob Dickinson (robfromboulder)

## Support

For issues or questions:

1. Check `../viewmapper-agent/CLAUDE.md` for Java agent details
2. Check `../ARCHITECTURE.md` for overall design
3. Review `../viewmapper-agent/datasets/README.md` for dataset examples
4. Check MCP documentation: https://modelcontextprotocol.io/

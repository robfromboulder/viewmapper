# viewmapper-mcp-server
This module exposes the ViewMapper agent as an MCP tool for Claude Desktop, enabling natural language exploration of Trino database schemas with conversation context.

## Dependencies

- **Java 24** - Required to run ViewMapper CLI
- **Python 3.14** - Required for MCP server
- **ViewMapper JAR** - Built from `../viewmapper-agent/` directory
- **Anthropic API Key** - For Claude agent in Java CLI

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

# create virtual environment
python3.14 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# install package with dev dependencies (includes pytest)
pip install -e ".[dev]"
```

### 3. Configure Environment Variables

Set these environment variables in your shell or Claude Desktop configuration:

```bash
# Required: Anthropic API key (passed to Java agent)
export ANTHROPIC_API_KEY_FOR_VIEWMAPPER="sk-ant-..."

# Required: Path to ViewMapper JAR
export VIEWMAPPER_JAR="/path/to/viewmapper/viewmapper-agent/target/viewmapper-478.jar"

# Optional: Connection string (default: test://simple_ecommerce)
export VIEWMAPPER_CONNECTION="test://simple_ecommerce"

# Optional: Enable verbose logging
export VIEWMAPPER_VERBOSE="1"
```

### 4. Configure Claude Desktop

Edit Claude Desktop's configuration file:

**macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
**Linux:** `~/.config/Claude/claude_desktop_config.json`

Add the ViewMapper MCP server:

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

The MCP server maintains conversation context (last 3 turns), enabling natural follow-up questions:

```
User: What are the leaf views?
Claude: [Lists leaf views...]

User: Show me the diagram for the first one
Claude: [Generates diagram without needing to re-specify dataset...]
```

### Available Test Datasets

The ViewMapper project includes 4 test datasets embedded in the JAR. Access via `VIEWMAPPER_CONNECTION`:

1. **test://simple_ecommerce** (11 views) - SIMPLE complexity
2. **test://moderate_analytics** (35 views) - MODERATE complexity
3. **test://realistic_bi_warehouse** (86 views) - COMPLEX complexity
4. **test://complex_enterprise** (154 views) - COMPLEX complexity

See `../viewmapper-agent/README.md` for detailed descriptions and example queries.

## Troubleshooting

### Issue: "Claude Desktop doesn't see the tool"

1. Check MCP server is configured correctly in `claude_desktop_config.json`
2. Verify absolute paths (not `~` or relative)
3. Restart Claude Desktop completely
4. Check Claude Desktop logs (Help → View Logs)

### Issue: "VIEWMAPPER_JAR not found" error

Verify the environment variable points to the correct JAR:

```bash
ls -la "$VIEWMAPPER_JAR"
# Should show: viewmapper-478.jar
```

### Issue: "API key required" error

Ensure API key is set:

```bash
echo $ANTHROPIC_API_KEY_FOR_VIEWMAPPER
# Should print: sk-ant-...
```

### Issue: Java errors or timeouts

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

### Issue: Conversation context not working

The MCP server maintains conversation history automatically. If conversations aren't connected:

1. Verify multiple messages are being sent in same chat window
2. Enable verbose mode to see enhanced prompts
3. Check Claude Desktop logs for errors

## Additional Documentation

- **../ARCHITECTURE.md** - Overall project architecture and design decisions
- **../viewmapper-agent/README.md** - ViewMapper agent usage and test datasets
- **CLAUDE.md** - AI assistant context and development guidelines
- **TESTING.md** - Comprehensive testing procedures and scenarios
- **MCP Documentation** - https://modelcontextprotocol.io/

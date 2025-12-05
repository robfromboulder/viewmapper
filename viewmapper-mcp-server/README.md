# viewmapper-mcp-server
This module exposes the ViewMapper agent as an MCP tool for Claude Desktop, enabling natural language exploration of Trino database schemas with conversation context.

## Dependencies

- **Trino 478** - For schemas to map
- **Java 24** - Required to run ViewMapper CLI
- **Python 3.14** - Required for MCP server
- **ViewMapper JAR** - Built from `../viewmapper-agent/` directory
- **Anthropic API Key** - For Claude agent in Java CLI

## Usage

The recommended way to use ViewMapper is via Docker, which bundles all dependencies.

### 1. Build Docker Image

```bash
cd viewmapper-mcp-server
cp ../viewmapper-agent/target/viewmapper-478.jar .
docker image rm -f viewmapper:478 && docker build --no-cache -t viewmapper:478 .
```

### 2. Configure Claude Desktop

Edit Claude Desktop's configuration file:

**macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
**Linux:** `~/.config/Claude/claude_desktop_config.json`

Add the ViewMapper MCP server:

```json
{
  "mcpServers": {
    "viewmapper-mcp-server": {
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "-e", "ANTHROPIC_API_KEY_FOR_VIEWMAPPER=sk-ant-your-key-here",
        "-e", "VIEWMAPPER_CONNECTION=test://simple_ecommerce",
        "viewmapper:478"
      ]
    }
  }
}
```

**Configuration options:**
- Replace `sk-ant-your-key-here` with your Anthropic API key
- Change `VIEWMAPPER_CONNECTION` to use different test datasets (see Available Test Datasets below)

### 3. Restart Claude Desktop

After editing the configuration, restart Claude Desktop completely (not just close/reopen a chat window).

## Prompting Guide

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

1. Verify Docker image is built: `docker images | grep viewmapper`
2. Check MCP server is configured correctly in `claude_desktop_config.json`
3. Restart Claude Desktop completely
4. Check Claude Desktop logs (Help → View Logs)

### Issue: "API key required" error

Verify your API key is correctly set in the Docker args:

```json
"-e", "ANTHROPIC_API_KEY_FOR_VIEWMAPPER=sk-ant-your-actual-key"
```

### Issue: Docker container not starting

Test the container manually:

```bash
docker run -i --rm \
  -e ANTHROPIC_API_KEY_FOR_VIEWMAPPER="sk-ant-..." \
  -e VIEWMAPPER_CONNECTION="test://simple_ecommerce" \
  viewmapper:478
```

### Issue: Conversation context not working

The MCP server maintains conversation history automatically. If conversations aren't connected:

1. Verify multiple messages are being sent in same chat window
2. Check Claude Desktop logs for errors
3. Rebuild Docker image if you've made recent changes

## Additional Documentation

- **../ARCHITECTURE.md** - Overall project architecture and design decisions
- **../viewmapper-agent/README.md** - ViewMapper agent usage and test datasets
- **CLAUDE.md** - AI assistant context and development guidelines
- **TESTING.md** - Comprehensive testing procedures and scenarios
- **MCP Documentation** - https://modelcontextprotocol.io/

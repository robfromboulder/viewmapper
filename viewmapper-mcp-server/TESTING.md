# Testing instructions for viewmapper-mcp-server

## Claude Desktop Configuration

1. Build the JAR:
   ```bash
   cd ../viewmapper-agent
   mvn clean package
   ```

2. Configure Claude Desktop (`~/Library/Application Support/Claude/claude_desktop_config.json`):

   ```json
   {
     "mcpServers": {
       "viewmapper": {
         "command": "/absolute/path/to/viewmapper/viewmapper-mcp-server/venv/bin/python",
         "args": ["mcp_server.py"],
         "cwd": "/absolute/path/to/viewmapper/viewmapper-mcp-server",
         "env": {
           "ANTHROPIC_API_KEY_FOR_VIEWMAPPER": "sk-ant-your-key-here",
           "VIEWMAPPER_CONNECTION": "test://simple_ecommerce",
           "VIEWMAPPER_JAR": "/absolute/path/to/viewmapper/viewmapper-agent/target/viewmapper-483.jar"
         }
       }
     }
   }
   ```

   Use absolute paths.

3. Restart Claude Desktop

## Manual Test Scenarios

**Catalog Discovery:**
```
What catalogs are available?
```

**Schema Discovery:**
```
What schemas are in the test catalog?
```

**Complete Discovery Flow:**
```
First: What catalogs can I explore?
Then: Show me the schemas in test.
Then: Analyze the simple_ecommerce schema.
```

**Simple Schema:**
```
Show me the full dependency diagram
```

**Entry Points:**
```
What are the high-impact entry points?
```

**Multi-Turn Conversation:**
```
First: What are the leaf views?
Then: Show me the diagram for the first one.
```

## Debugging

**Enable verbose mode:**
```json
"env": {
  "VIEWMAPPER_VERBOSE": "1"
}
```

**Check Claude Desktop logs:**
- macOS: `~/Library/Logs/Claude/`
- Look for stderr output from Java CLI

**Test Python script directly:**
```bash
export ANTHROPIC_API_KEY_FOR_VIEWMAPPER="sk-ant-..."
export VIEWMAPPER_CONNECTION="test://simple_ecommerce"
export VIEWMAPPER_JAR="/absolute/path/to/viewmapper/viewmapper-agent/target/viewmapper-483.jar"
echo '{"query": "Show me the full dependency diagram"}' | python3 mcp_server.py
```


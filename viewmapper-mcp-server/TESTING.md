# TESTING.md

## Prerequisites

Python **3.14** is required and used in this documentation.

Check your Python version:
```bash
python3 --version
```

Install Python 3.14 if necessary:
**macOS (using Homebrew):**
```bash
brew install python@3.14
```
**Ubuntu/Debian:**
```bash
sudo apt install python3.14 python3.14-venv
```

## Running Unit Tests

### 1. Create Virtual Environment

```bash
python3.14 -m venv venv
source venv/bin/activate
```

### 2. Install Dependencies

```bash
pip install --upgrade pip
pip install mcp pytest pytest-asyncio
```

### 3. Run Tests

```bash
# Run all tests
pytest

# Run with verbose output
pytest -v

# Run specific test file
pytest tests/test_mcp_server.py

# Run specific test
pytest tests/test_mcp_server.py::TestToolRegistration::test_list_tools_returns_single_tool

# Run with coverage
pip install pytest-cov
pytest --cov=mcp_server --cov-report=html
```

## Test Structure

The test suite uses mocked subprocess calls to avoid requiring:
- A built ViewMapper JAR
- Anthropic API key
- Java runtime

### Test Categories

**Tool Registration Tests (`TestToolRegistration`):**
- Verify tool is registered with correct name
- Verify input schema is correct
- Verify required parameters

**Prompt Building Tests (`TestPromptBuilding`):**
- Test conversation history formatting
- Test history truncation
- Test context window limits
- Test session ID handling

**Tool Execution Tests (`TestToolExecution`):**
- Test parameter validation
- Test error handling (missing files, Java errors, timeouts)
- Test subprocess invocation
- Test conversation history accumulation

## Manual Testing with Claude Desktop

### Prerequisites

1. **Python 3.14** (system-wide or in specific location)
2. **ViewMapper JAR** built from `../viewmapper-agent/`
3. **Anthropic API Key**

### Setup

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

   **Important:** Use absolute paths!

3. Restart Claude Desktop

### Test Queries

Try these queries in Claude Desktop:

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

### Debugging

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
export VIEWMAPPER_JAR="/absolute/path/to/viewmapper/viewmapper-agent/target/viewmapper-478.jar"
echo '{"query": "Show me the full dependency diagram"}' | python3 mcp_server.py
```

## Continuous Integration

To add CI testing (GitHub Actions, etc.):

```yaml
name: Test MCP Server
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
        with:
          python-version: '3.14'
      - name: Install dependencies
        run: |
          cd viewmapper-mcp-server
          pip install mcp pytest pytest-asyncio
      - name: Run tests
        run: |
          cd viewmapper-mcp-server
          pytest -v
```

## Known Issues

1. **Import errors**: Ensure virtual environment is activated and dependencies installed.
2. **Module not found**: Make sure you're running tests from the `mcp-server` directory.

## Future Test Improvements

1. **Integration tests** - Test with real JAR and dataset files
2. **Performance tests** - Measure response times for different schema sizes
3. **Error injection** - Test recovery from various Java CLI failures
4. **Context persistence** - Test session isolation when implemented

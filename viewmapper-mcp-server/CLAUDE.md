# Claude instructions for viewmapper-mcp-server

The conversational half of ViewMapper: MCP protocol handling, conversation context across turns, subprocess execution of the Java agent, and output delivered so Claude Desktop renders it.

**This module is the brain's `viewmapper/mcp/` unit, token `VMR_MCP`.** Read `../CLAUDE.md` for brain routing, the repo-wide runbook, and prose rules.

## Build Commands

```bash
# Create the virtual environment
python3.14 -m venv venv
source venv/bin/activate

# Install this module and its test dependencies
pip install -e ".[dev]"

# Run all tests
pytest -v

# Run specific test
pytest -v -k "test_name"

# Run with coverage
pytest --cov=mcp_server --cov-report=html
```

No Java, JAR, or API key required for tests. Manual testing requires Claude Desktop — see `TESTING.md` for configurations and scenarios.

## Environment Variables

**Required:**

- `VIEWMAPPER_JAR` — absolute path to the agent JAR
- `ANTHROPIC_API_KEY_FOR_VIEWMAPPER` or `ANTHROPIC_API_KEY` — passed through to the Java agent

**Optional:**

- `VIEWMAPPER_CONNECTION` — connection string (default: `test://simple_ecommerce`)
- `VIEWMAPPER_VERBOSE` — set to `1` for verbose output
- `VIEWMAPPER_TIMEOUT_SECONDS` — limit for one whole agent run (default: `600`)

## Code Style

- Python 3.14+ (inherited from MCP SDK, not from this code)
- PEP 8, 4-space indentation, line length ~88 (Black)
- Copyright: `# © 2024-2026 Rob Dickinson (robfromboulder)`
- Type hints on all function parameters and returns
- Errors return a `CallToolResult` with `is_error=True` — never raise exceptions to the client
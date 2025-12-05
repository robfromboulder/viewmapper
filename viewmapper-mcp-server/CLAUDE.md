# CLAUDE.md - ViewMapper MCP Server Module

> **IMPORTANT:** See `../ARCHITECTURE.md` for strategic decisions and overall project architecture.

## Current Status

⚠️ **Active Development** - NOT production ready, still under development and testing
- Python 3.14+ MCP server wrapping ViewMapper Java CLI
- Conversation context management (last 3 turns)
- Comprehensive error handling with clear user messages
- Unit tests with mocked subprocess calls (229 lines)
- Single tool: `explore_trino_views` with natural language queries

## Completed Features

### Live Trino Support via JDBC ✅
- Pass-through of `jdbc:trino://...` connections to Java CLI
- Schema parameter support with catalog.schema format guidance
- LLM instructed to use `catalog.schema` format (e.g., `viewzoo.example`)
- Java CLI handles catalog validation and provides clear error messages

## Future Enhancements

### Phase 1: Portable Java Execution (High Priority)
- Replace hardcoded Java path with environment variable or PATH lookup
- Support `JAVA_HOME` or `VIEWMAPPER_JAVA` configuration
- Docker compatibility (remove hardcoded `/Users/robfromboulder/...`)

### Phase 2: Multi-Session Context (Medium Priority)
- Extract session ID from MCP protocol
- Isolate conversation histories per session
- Prevent context bleeding across Claude Desktop sessions

### Phase 3: Structured Context (Low Priority - Option B)
- Add `--context` parameter to Java CLI
- Implement LangChain4j `ChatMemory` in agent
- Pass JSON context instead of text-based prompt enhancement
- Cleaner separation between current question and history

---

## Quick Reference

**What This Module Does:**
- Wraps ViewMapper Java CLI as an MCP server
- Provides natural language interface for schema exploration
- Manages conversation context (3-turn window)
- Handles subprocess execution and error formatting
- Integrates with Claude Desktop

**Key Files to Know:**
- `mcp_server.py` (235 lines) - Main MCP server implementation
- `tests/test_mcp_server.py` (229 lines) - Unit tests with mocked subprocess
- `pyproject.toml` (33 lines) - Python project configuration
- `README.md` (413 lines) - User-facing documentation and setup
- `TESTING.md` (215 lines) - Testing procedures and scenarios

**Build & Run:**
```bash
# Install dependencies
pip install -e .

# Run tests
pytest -v

# Configure in Claude Desktop (see README.md)
```

**Test:**
```bash
pytest -v                              # All unit tests (mocked)
# Manual testing requires Claude Desktop + JAR + API key
```

---

## Architecture Overview

### Data Flow

```
┌──────────────────┐
│  Claude Desktop  │  (User interface)
└────────┬─────────┘
         │ MCP Protocol (JSON-RPC over stdio)
         ↓
┌──────────────────┐
│  mcp_server.py   │  ← THIS MODULE
│                  │  - Tool registration
│  Python 3.14+    │  - Conversation context (last 3 turns)
│  MCP SDK         │  - Subprocess management
└────────┬─────────┘
         │ subprocess.run()
         ↓
┌──────────────────┐
│  Java CLI        │  ViewMapper agent (separate repo)
│  viewmapper.jar  │  - LangChain4j + Claude API
│                  │  - SQL parsing (Trino parser)
│  Java 24         │  - Mermaid diagram generation
└──────────────────┘
```

### Layer Architecture

1. **MCP Protocol Layer**
   - JSON-RPC over stdio
   - Tool registration and schema definition
   - Request/response handling

2. **Context Management Layer**
   - Session-based conversation history
   - 3-turn window with truncation
   - Prompt enhancement with history

3. **Subprocess Layer**
   - Java CLI execution
   - Environment variable passing
   - Timeout and error handling

4. **Integration Layer**
   - Output formatting
   - Error message translation
   - Response streaming

### Key Design Principles

- **Simplicity First:** Prompt-based context (no Java changes required)
- **Defensive Error Handling:** Never raise exceptions to MCP
- **Context Awareness:** Maintain conversation flow across turns
- **Fail-Safe:** Clear error messages guide user troubleshooting
- **Testability:** Mock subprocess for unit testing

---

## Key Components

### Core Functions

#### `build_prompt_with_history(history, current_query)` (mcp_server.py:56-97)

Builds enhanced prompt with conversation context.

**Input:**
- `history`: List of message dicts with `role` and `content`
- `current_query`: Current user question (string)

**Output:**
- Enhanced prompt string with format:
  ```
  Previous conversation:
  User: <question 1>
  Assistant: <response 1>
  ...
  Current question: <current_query>
  ```

**Behavior:**
- Limits to last 3 turns (6 messages)
- Truncates assistant responses > 200 chars
- Returns unchanged query if no history

**Line References:** mcp_server.py:56-97

#### `list_tools()` (mcp_server.py:100-128)

Registers MCP tool with schema definition.

**Returns:**
- List with single tool: `explore_trino_views`
- Input schema: `query` (string, required)

**Line References:** mcp_server.py:100-128

#### `call_tool(name, arguments)` (mcp_server.py:131-220)

Main tool handler - executes Java CLI and manages context.

**Workflow:**
1. Validate tool name and parameters
2. Get session history and build enhanced prompt
3. Execute Java CLI via subprocess
4. Update conversation history
5. Return response or formatted error

**Error Handling:**
- Unknown tool → `❌ Unknown tool: {name}`
- Missing parameter → `❌ Error: 'query' parameter is required`
- Java error → `❌ ViewMapper Error:\n{stderr}`
- Timeout → `⏱️ Request timed out after 60 seconds...`
- File not found → `❌ Error: Java or JAR file not found...`
- Other → `❌ Unexpected error: {str(e)}`

**Line References:** mcp_server.py:131-220

### Context Management

#### Session Storage

**Current Implementation:**
- Global `conversation_history` dict: `session_id → list[messages]`
- Single session: `get_session_id()` always returns `"default"`
- Each message: `{"role": "user"|"assistant", "content": str}`

**Limitation:**
All Claude Desktop conversations share same history (context bleeding).

#### History Window

**Parameters (in `build_prompt_with_history`):**
- `max_history_turns = 3` (line 81) - Keep last 3 turns (6 messages)
- Truncation: 200 chars for assistant responses (line 89)

**Rationale:**
- Prevents token limit exhaustion
- Maintains recent context relevance
- Reduces prompt size for Java CLI

### Subprocess Management

#### Command Structure

```bash
java -jar $VIEWMAPPER_JAR \
  run \
  "<enhanced prompt with history>" \
  --connection "$VIEWMAPPER_CONNECTION" \
  --output text \
  [--verbose]
```

**Hardcoded Java Path (mcp_server.py:165):**
```python
cmd = [
    "/Users/robfromboulder/Tools/jdk-24.0.2+12/Contents/Home/bin/java",
    "-jar", VIEWMAPPER_JAR,
    "run", enhanced_prompt,
    "--connection", VIEWMAPPER_CONNECTION,
    "--output", "text"
]
```

**TODO:** Replace with portable solution (see Future Enhancements)

#### Environment Variables

**Passed to subprocess:**
- `ANTHROPIC_API_KEY_FOR_VIEWMAPPER` - Required by Java agent

**Read by MCP server:**
- `VIEWMAPPER_JAR` - Path to JAR file (required)
- `VIEWMAPPER_CONNECTION` - Dataset selection (default: `test://simple_ecommerce`)
- `VIEWMAPPER_VERBOSE` - Enable verbose output (optional, default: false)

#### Timeout Handling

- Default: 60 seconds (mcp_server.py:182)
- On timeout: Returns user-friendly message suggesting simpler queries
- No automatic retry

---

## Configuration

### Required Environment Variables

```bash
# API key passed to Java agent
ANTHROPIC_API_KEY_FOR_VIEWMAPPER="sk-ant-..."  # or ANTHROPIC_API_KEY

# Path to ViewMapper JAR
VIEWMAPPER_JAR="/absolute/path/to/viewmapper-478.jar"
```

### Optional Environment Variables

```bash
# Connection string (default: test://simple_ecommerce)
VIEWMAPPER_CONNECTION="test://moderate_analytics"

# Enable verbose output (default: false)
VIEWMAPPER_VERBOSE="1"
```

### Claude Desktop Configuration

**Production (Docker - Recommended):**

Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:

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

**Development (Local Python):**

For developers making changes to the MCP server:

```json
{
  "mcpServers": {
    "viewmapper-mcp-server": {
      "command": "/absolute/path/to/viewmapper/viewmapper-mcp-server/venv/bin/python",
      "args": ["mcp_server.py"],
      "cwd": "/absolute/path/to/viewmapper/viewmapper-mcp-server",
      "env": {
        "ANTHROPIC_API_KEY_FOR_VIEWMAPPER": "sk-ant-your-key-here",
        "VIEWMAPPER_JAR": "/absolute/path/to/viewmapper-agent/target/viewmapper-478.jar",
        "VIEWMAPPER_CONNECTION": "test://simple_ecommerce"
      }
    }
  }
}
```

### Test Dataset Reference

Available via `VIEWMAPPER_CONNECTION`:

1. `test://simple_ecommerce` - 11 views, SIMPLE complexity (default)
2. `test://moderate_analytics` - 35 views, MODERATE complexity
3. `test://complex_enterprise` - 154 views, COMPLEX complexity
4. `test://realistic_bi_warehouse` - 86 views, COMPLEX complexity

See `../viewmapper-agent/src/main/resources/datasets/` for details.

---

## Testing Strategy

### Test Organization

**Unit Tests (tests/test_mcp_server.py - 229 lines):**

**Tool Registration Tests (3 tests):**
- Tool name and description
- Input schema validation
- Parameter requirements

**Prompt Building Tests (5 tests):**
- No history → unchanged query
- Single turn → proper formatting
- Multiple turns → 3-turn limit enforcement
- Long responses → truncation at 200 chars
- Edge cases → empty strings, special characters

**Tool Execution Tests (9 tests):**
- Successful execution → updates history
- Unknown tool → error message
- Missing parameter → validation error
- Java CLI error → formatted stderr
- Timeout → user-friendly message
- File not found → clear guidance
- Context → history passed to Java CLI

### Testing Philosophy

- **Mock subprocess.run** - No Java/JAR required for unit tests
- **Pytest fixtures** - Clean test isolation
- **Comprehensive edge cases** - Error paths well-covered
- **No live integrations** - Fast, reliable CI/CD

### Running Tests

```bash
# All tests
pytest -v

# Specific test
pytest -v -k "test_name"

# With coverage
pytest --cov=viewmapper_mcp_server --cov-report=html
```

### Manual Testing

**Requirements:**
- Claude Desktop installed
- ViewMapper JAR built
- Anthropic API key
- Configuration in `claude_desktop_config.json`

**Test Scenarios:**
1. **Simple query:** "Show me the full dependency diagram"
2. **Multi-turn:** "What are the leaf views?" → "Show me the first one"
3. **Error handling:** Invalid VIEWMAPPER_JAR path
4. **Context:** Verify history appears in Java CLI prompt
5. **Timeout:** Large dataset with complex query

See `TESTING.md` for detailed manual testing procedures.

---

## Development Guide

### Setup

```bash
# Clone repository
git clone <repo-url>
cd viewmapper-mcp-server

# Install dependencies
pip install -e .

# Run tests
pytest -v
```

### Adding a New Tool Parameter

1. **Update `inputSchema` in `list_tools()`** (mcp_server.py:104-127)
   ```python
   "properties": {
       "query": {"type": "string", "description": "..."},
       "new_param": {"type": "string", "description": "..."}  # ADD
   },
   "required": ["query", "new_param"]  # UPDATE
   ```

2. **Extract parameter in `call_tool()`** (mcp_server.py:149-156)
   ```python
   query = arguments.get("query")
   new_param = arguments.get("new_param")  # ADD
   ```

3. **Add validation**
   ```python
   if not new_param:
       return [types.TextContent(...)]  # Error message
   ```

4. **Pass to Java CLI** (mcp_server.py:165-174)
   ```python
   cmd = [..., "--new-flag", new_param]
   ```

5. **Add unit tests** (tests/test_mcp_server.py)

### Modifying Context Window

**Change history limit:**
```python
# In build_prompt_with_history() (line 81)
max_history_turns = 5  # Currently 3
```

**Change truncation length:**
```python
# In build_prompt_with_history() (line 89)
if msg["role"] == "assistant" and len(content) > 500:  # Currently 200
    content = content[:500] + "..."
```

### Debugging Subprocess Calls

**Enable verbose mode:**
```bash
export VIEWMAPPER_VERBOSE="1"
```

**Add debug logging:**
```python
# In call_tool() before subprocess.run()
import sys
print(f"DEBUG: Executing command: {cmd}", file=sys.stderr)
print(f"DEBUG: Enhanced prompt: {enhanced_prompt}", file=sys.stderr)
```

**Check Claude Desktop logs:**
- macOS: `~/Library/Logs/Claude/`
- Look for stderr output from MCP server

### Code Quality Standards

**Copyright Headers:**
```python
# © 2024-2025 Rob Dickinson (robfromboulder)
```

**Type Hints:**
- Use type hints for all function parameters and returns
- Example: `def build_prompt_with_history(history: list, current_query: str) -> str:`

**Error Handling:**
- Never raise exceptions to MCP (return `TextContent` errors)
- Provide actionable error messages
- Log errors to stderr for debugging

**Testing:**
- Test method names describe scenario: `test_successful_query_updates_history()`
- Mock external dependencies (subprocess)
- Verify both success and error paths

**Code Style:**
- PEP 8 compliance
- 4-space indentation
- Line length: prefer 88 characters (Black formatter)
- Docstrings for all public functions

---

## Design Decisions

### Why Prompt-Based Context (Option A) Over Structured Context (Option B)?

**Chosen: Option A - Prompt Enhancement**
- No changes required to Java CLI
- Simple implementation (40 lines of code)
- Works with existing agent system prompt
- History visible to agent (transparency)

**Not Chosen: Option B - Structured Context**
- Requires Java CLI changes (`--context` parameter)
- Requires LangChain4j `ChatMemory` integration
- More complex but cleaner separation
- Future enhancement opportunity

**Trade-off:**
Simplicity and backward compatibility vs. cleaner architecture.

### Why 3-Turn History Window?

**Rationale:**
- Sufficient for typical exploration workflows
- Prevents token limit issues (Claude API)
- Keeps prompts manageable for Java CLI
- Reduces memory footprint

**Tested Scenarios:**
- User: "What are the views?" → Agent: "11 views..." → User: "Show first" ✅
- Longer conversations truncate older history ✅

### Why 60-Second Timeout?

**Rationale:**
- Complex schemas (154 views) + agent reasoning ≈ 20-40 seconds
- Buffer for API latency and network issues
- User expectation: response within 1 minute

**Alternatives Considered:**
- 30 seconds - Too short for complex queries
- 120 seconds - Too long for user patience

### Why Single Session Context?

**Current State:**
- MCP protocol doesn't expose session IDs (as of implementation)
- All conversations share history

**Implication:**
- Context bleeding across Claude Desktop sessions
- Rare in practice (users typically explore one schema at a time)

**Future:**
- When MCP adds session support, extract ID from context

---

## Known Limitations

### 1. Hardcoded Java Path (mcp_server.py:165)

**Impact:** Not portable across systems/users

**Current:**
```python
cmd = ["/Users/robfromboulder/Tools/jdk-24.0.2+12/Contents/Home/bin/java", ...]
```

**Mitigation:** Document in README, add to Phase 1 enhancements

**Line:** mcp_server.py:165, 171 (comment: "todo replace reference to 'robfromboulder' above when dockerizing")

### 2. Single Session Context

**Impact:** Context bleeds across multiple Claude Desktop sessions

**Current:** `get_session_id()` always returns `"default"`

**Mitigation:** Typically not an issue (single schema exploration), future enhancement

### 3. Schema Parameter Handling

**Note:** Schema parameter added to MCP tool schema but is optional

**Behavior:**
- If provided, passed to Java CLI via `--schema` parameter
- LLM guided to use `catalog.schema` format in tool description
- Java CLI validates and provides clear error messages for mismatched formats

### 4. 60-Second Timeout

**Impact:** Large schemas may timeout on complex queries

**Current:** Fixed 60-second timeout (mcp_server.py:182)

**Mitigation:** Error message suggests simpler queries, no retry mechanism

### 5. Python 3.14+ Required

**Impact:** Older Python versions not supported

**Current:** MCP SDK constraint

**Mitigation:** Document in README, use pyenv for compatibility

---

## Performance Considerations

**Current Scale:**
- Tested with datasets up to 154 views
- Typical query: 10-40 seconds (includes Java startup + agent reasoning)
- Memory: < 50 MB (Python process), Java CLI manages own heap

**Optimization Opportunities:**
1. **Persistent Java process** - Avoid startup overhead (3-5 seconds per query)
2. **Async execution** - Non-blocking subprocess calls
3. **Response streaming** - Show progress during long operations
4. **Cache frequent queries** - Memoize identical prompts + history

**Benchmarks (Manual Testing):**
- Simple query (11 views): ~5-10 seconds
- Complex query (154 views): ~20-40 seconds
- Timeout threshold: 60 seconds

**Memory Usage:**
- MCP server: ~10 MB
- Conversation history: < 1 KB per session
- Subprocess overhead: ~200 MB (Java CLI heap)

---

## Dependencies

**Build Tool:**
- **pip** - Python package management

**Runtime:**
- **Python 3.14+** - Official tested version

**Core Libraries:**
- **mcp 1.0.0** - Model Context Protocol SDK
- **anthropic** - Anthropic API client (transitive dependency)

**Testing:**
- **pytest** - Unit testing framework
- **pytest-asyncio** - Async test support

**External Dependencies:**
- **ViewMapper JAR** - Java CLI (separate build, see ../viewmapper-agent)
- **JDK 24** - Java runtime for JAR execution
- **Anthropic API** - Claude API access (via Java agent)

**See:** `pyproject.toml` for complete dependency list with versions

---

## References

### Project Documentation
- `README.md` - User-facing documentation and quick start
- `TESTING.md` - Detailed testing procedures and scenarios
- `../ARCHITECTURE.md` - Overall project architecture (⚠️ READ THIS for strategic decisions)
- `../viewmapper-agent/CLAUDE.md` - Java agent implementation details

### External Documentation
- [Model Context Protocol](https://modelcontextprotocol.io/) - MCP specification
- [MCP Python SDK](https://github.com/anthropics/python-sdk) - SDK documentation
- [Claude Desktop](https://claude.ai/download) - Desktop app download
- [Anthropic API Docs](https://docs.anthropic.com/) - Claude API reference

### Related Resources
- [Pytest Documentation](https://docs.pytest.org/)
- [Python Type Hints](https://docs.python.org/3/library/typing.html)

---

## Integration with Java Agent

### Input Format (To Java CLI)

**MCP sends:**
```
Previous conversation:
User: What are the views?
Assistant: There are 11 views...

Current question: Show me the diagram
```

**Java agent expects:**
- Single string prompt (ViewMapperAgent.java:133)
- No special format required

✅ **Compatible** - Agent processes any text prompt

### Output Format (From Java CLI)

**Java agent produces:**
- Plain text response (`--output text`)
- May include Mermaid: ` ```mermaid\ngraph TB\n...``` `

**MCP expects:**
- Plain text from stdout
- Returns unchanged to Claude Desktop

✅ **Compatible** - Claude Desktop renders Mermaid automatically

### Agent Behavior

**System prompt (ViewMapperAgent.java:86-123):**
1. Always calls `analyzeSchema` first
2. Decides full diagram vs entry points based on complexity
3. Generates Mermaid when appropriate
4. Maintains reasoning across conversation

**Context handling:**
- Agent treats enhanced prompt as extended user input
- LangChain4j maintains tool execution flow normally

✅ **Compatible** - Prompt enhancement doesn't interfere with tool calling

### Error Flow

**Java CLI errors:**
- Non-zero exit code + stderr message
- MCP catches and formats: `❌ ViewMapper Error:\n{stderr}`

**MCP errors:**
- Never propagate to Claude Desktop (crash UX)
- Always return `TextContent` with formatted error

---

## Common Development Tasks

### Update MCP Server Version

1. Edit `pyproject.toml`:
   ```toml
   dependencies = ["mcp>=1.1.0"]  # Update version
   ```

2. Reinstall:
   ```bash
   pip install -e .
   ```

3. Test:
   ```bash
   pytest -v
   ```

4. Update Claude Desktop (restart app)

### Add New Error Type

1. **Define error detection** (in `call_tool()`)
   ```python
   except NewErrorType as e:
       return [types.TextContent(
           type="text",
           text=f"❌ New Error: {str(e)}\nGuidance: ..."
       )]
   ```

2. **Add unit test**
   ```python
   def test_new_error_returns_formatted_message(mock_subprocess):
       mock_subprocess.side_effect = NewErrorType("Test error")
       result = await call_tool("explore_trino_views", {"query": "test"})
       assert "❌ New Error" in result[0].text
   ```

3. **Document** (in Error Handling section)

### Change Default Dataset

```bash
# In environment
export VIEWMAPPER_CONNECTION="test://complex_enterprise"

# Or in Claude Desktop config
{
  "env": {
    "VIEWMAPPER_CONNECTION": "test://complex_enterprise"
  }
}
```

### Debug Context Management

```python
# Add logging to build_prompt_with_history()
import sys
print(f"DEBUG: History length: {len(history)}", file=sys.stderr)
print(f"DEBUG: Enhanced prompt:\n{result}", file=sys.stderr)
```

Check Claude Desktop logs for output.

---

## Quick Verification Checklist

When modifying this module, verify:

- [ ] Unit tests pass: `pytest -v`
- [ ] Environment variables validated at startup
- [ ] Errors return `TextContent` (don't raise exceptions)
- [ ] Conversation history updates after successful calls
- [ ] History respects 3-turn limit and truncation
- [ ] Java CLI command structure matches RunCommand expectations
- [ ] Claude Desktop config uses absolute paths
- [ ] No hardcoded paths (except TODO on line 165)
- [ ] Type hints present on all functions
- [ ] Docstrings updated for changed functions

---

## Project Structure

```
viewmapper-mcp-server/
├── pyproject.toml                   # Python project configuration
├── README.md                        # User-facing documentation
├── TESTING.md                       # Testing procedures
├── CLAUDE.md                        # This file - AI context
├── src/
│   └── viewmapper_mcp_server/
│       ├── __init__.py              # Package init
│       └── mcp_server.py            # Main MCP server (235 lines)
└── tests/
    └── test_mcp_server.py           # Unit tests (229 lines, mocked subprocess)
```

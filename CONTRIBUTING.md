# Contributing to ViewMapper

## Built with Claude Code

ViewMapper is developed and maintained exclusively with [Claude Code](https://claude.ai/code), Anthropic's official CLI for Claude. All code contributions must be made through Claude Code sessions to maintain consistency with the architecture-first development model and keep context files synchronized.

If you haven't used Claude Code yet, these resources are super helpful:
* https://code.claude.com/docs
* https://www.anthropic.com/engineering/claude-code-best-practices
* https://www.siddharthbharath.com/claude-code-the-complete-guide/

## Architecture-First Development

**Important:** Changes to this project should follow an architecture-first workflow:

1. **Think First, Code Second**
   - Review `ARCHITECTURE.md` to understand design decisions
   - Propose architectural changes in `ARCHITECTURE.md` before implementing
   - Discuss trade-offs and alternatives in the architecture document
   - Get alignment on approach before writing code

2. **Update Documentation as You Go**
   - Keep module `CLAUDE.md` files synchronized with code changes
   - Update line number references when refactoring
   - Document new components in the appropriate CLAUDE.md
   - Maintain "Current Status" and "Completed Features" sections

3. **Maintain Context Files**
   - `ARCHITECTURE.md` - Strategic decisions, overall design, technology choices
   - `viewmapper-agent/CLAUDE.md` - Java agent implementation details
   - `viewmapper-mcp-server/CLAUDE.md` - Python MCP server implementation details

## Project Structure

```
viewmapper/
├── ARCHITECTURE.md              # Start here - strategic decisions
├── CONTRIBUTING.md              # This file - development workflow
├── README.md                    # User-facing documentation
├── build.sh                     # Build local development version
├── clean.sh                     # Remove all build artifacts
├── release.sh                   # Release to DockerHub
├── viewmapper-agent/            # Java CLI with LLM agent
│   ├── CLAUDE.md                # Java module context
│   ├── CONTRIBUTING.md          # Java-specific conventions
│   └── src/...
└── viewmapper-mcp-server/       # Python MCP wrapper
    ├── CLAUDE.md                # Python module context
    └── src/...
```

## Development Workflow

### 1. Setting Up Your Environment

**Prerequisites:**
- Claude Code (required - install from https://claude.ai/code)
- JDK 24
- Maven 3.8+
- Python 3.14+
- Docker

**Initial Setup:**
```bash
# Clone repository
git clone https://github.com/robfromboulder/viewmapper.git
cd viewmapper

# Build everything
./build.sh

# Run tests
cd viewmapper-agent && mvn test
cd ../viewmapper-mcp-server && pytest -v
```

### 2. Making Changes

**For New Features (must use Claude Code):**
1. Start a Claude Code session
2. Ask Claude to read `ARCHITECTURE.md`
3. Describe the feature and discuss design approach with Claude
4. Have Claude update `ARCHITECTURE.md` with design decisions
5. Have Claude implement changes in code
6. Have Claude update relevant `CLAUDE.md` file(s) with implementation details
7. Have Claude add tests
8. **Review all changes in IntelliJ IDEA:**
   - Open changed files in IntelliJ
   - Use **Code → Reformat Code** to apply consistent formatting
   - Use **Code → Optimize Imports** to remove unused imports
   - Run **Code → Inspect Code** to identify issues
   - Apply IntelliJ suggestions for language-level improvements
   - Review logic and verify correctness
9. Verify builds and tests pass (can run manually or via Claude)

**For Bug Fixes (must use Claude Code):**
1. Start a Claude Code session
2. Ask Claude to read relevant `CLAUDE.md` file
3. Describe the bug to Claude
4. Have Claude add failing test that reproduces the bug
5. Have Claude fix the code
6. Have Claude update `CLAUDE.md` if implementation details changed
7. **Review all changes in IntelliJ IDEA:**
   - Reformat code and optimize imports
   - Run code inspections
   - Verify the fix addresses the root cause
8. Verify all tests pass

**For Documentation (must use Claude Code):**
1. Start a Claude Code session
2. Have Claude update `README.md` for user-facing changes
3. Have Claude update `CLAUDE.md` for implementation changes
4. Have Claude update `ARCHITECTURE.md` for design changes
5. **Review and verify all documentation changes**
6. Approve Claude's changes after review

### 3. Testing Your Changes

Tests can be run either by Claude Code or manually. Both approaches are supported.

**Via Claude Code (recommended):**
```
Ask Claude: "Please run all tests"
Ask Claude: "Please run the parser tests only"
Ask Claude: "Please run pytest with coverage"
```

**Manual testing (if needed):**

**Java Agent:**
```bash
cd viewmapper-agent
mvn clean test                           # All tests
mvn test -Dtest="TrinoSqlParser*"        # Parser tests only
mvn test -Dtest="DependencyAnalyzer*"    # Analyzer tests only
```

**Python MCP Server:**
```bash
cd viewmapper-mcp-server
pytest -v                                # All tests
pytest -v -k "test_name"                 # Specific test
pytest --cov=viewmapper_mcp_server       # With coverage
```

**End-to-End:**
```bash
# Test with embedded dataset
java -jar viewmapper-agent/target/viewmapper-478.jar run \
  --connection "test://simple_ecommerce" \
  "Show me the full dependency diagram"

# Test with Claude Desktop (requires configuration)
# See README.md for setup instructions
```

### 4. Building and Releasing

Builds and releases can be run via Claude Code or manually.

**Via Claude Code (recommended):**
```
Ask Claude: "Please run ./build.sh"
Ask Claude: "Please run ./clean.sh then rebuild"
Ask Claude: "Please release version 478b to DockerHub"
```

**Manual execution (if needed):**

**Local Build:**
```bash
./build.sh                               # Builds JAR and Docker image
```

**Clean Build:**
```bash
./clean.sh                               # Remove all artifacts
./build.sh                               # Fresh build
```

**Release to DockerHub:**
```bash
./release.sh 478a                        # Replace 'a' with next letter
```

The release script will:
- Verify git working directory is clean
- Run clean + build
- Build multi-platform Docker image
- Push to `robfromboulder/viewmapper-mcp-server:478a`
- Create and push git tag `v478a`

## Documentation Standards

### No Line Number References in Documentation

**Policy:** Documentation files (ARCHITECTURE.md, CLAUDE.md, README.md, CONTRIBUTING.md, TESTING.md) must NOT reference specific line numbers in source files.

**Rationale:** Line numbers change frequently as code evolves, causing documentation to drift out of sync and creating maintenance burden.

**Instead of line numbers, use:**
- Class names: `RunCommand`
- Method names with C++ style notation: `RunCommand.java::loadFromJdbc()`
- Field names: `DependencyAnalyzer.graph`
- Section descriptions: "in the schema loading logic"
- Function signatures: `build_prompt_with_history(history, current_query)`

**Examples:**

❌ **Bad:**
```
See RunCommand.java:125-172 for implementation
The timeout is set in mcp_server.py:182
Update the schema in list_tools() (mcp_server.py:104-127)
```

✅ **Good:**
```
See RunCommand.java::loadFromJdbc() for implementation
The timeout is set in the call_tool() method via subprocess.run(timeout=60)
Update the schema in list_tools()
```

**When Line References Are Acceptable:**
- Error messages when reporting bugs/issues
- Commit messages (temporary references that don't live in docs)
- Code comments within source files
- Test failure messages

## Code Conventions

### Java (viewmapper-agent)

**Style:**
- Follow IntelliJ IDEA defaults
- Max line length: 130 characters
- 4-space indentation
- No wildcard imports (except Trino AST classes)
- **Always run IntelliJ's "Reformat Code" on all changed files**
- **Always run IntelliJ's "Optimize Imports" to remove unused imports**
- **Run IntelliJ's "Inspect Code" and address warnings**

**Copyright Header:**
```java
// © 2024-2025 Rob Dickinson (robfromboulder)
```

**Documentation:**
- Class-level Javadoc for all public classes
- Method-level Javadoc for all public methods
- Include examples where helpful

**Testing:**
- Descriptive test names: `testDiamondPatternHasOneLeaf()`
- Comprehensive edge case coverage
- Use AssertJ for fluent assertions

**IntelliJ Inspections:**
After Claude generates code, use IntelliJ to:
- Remove unused variables, methods, imports
- Apply language-level suggestions (Java 24 features)
- Fix nullability warnings
- Optimize string operations
- Simplify boolean expressions

### Python (viewmapper-mcp-server)

**Style:**
- PEP 8 compliance
- Line length: 88 characters (Black formatter)
- 4-space indentation
- Type hints on all functions

**Copyright Header:**
```python
# © 2024-2025 Rob Dickinson (robfromboulder)
```

**Documentation:**
- Docstrings for all public functions
- Type hints for parameters and returns
- Clear error messages

**Testing:**
- Descriptive test names: `test_successful_query_updates_history()`
- Mock external dependencies (subprocess)
- Test both success and error paths

## Security

**Scanning for Vulnerabilities:**
```bash
# Java dependencies
cd viewmapper-agent
mvn versions:display-dependency-updates
trivy filesystem .

# Python dependencies
cd viewmapper-mcp-server
pip list --outdated
```

**API Keys:**
- Never commit API keys to git
- Use environment variables: `ANTHROPIC_API_KEY_FOR_VIEWMAPPER`
- Document required keys in README.md

## Why Claude Code is Required

**Architecture-First Development Model:**
This project was built from scratch using Claude Code, starting with `ARCHITECTURE.md` and generating all implementation files from that foundation. Maintaining this approach requires:

1. **Synchronized Context Files** - Changes must update ARCHITECTURE.md and CLAUDE.md files with correct line numbers and implementation details
2. **Design Consistency** - New features must align with architectural decisions documented in ARCHITECTURE.md
3. **Documentation Quality** - Implementation details must be captured in CLAUDE.md for future sessions

**Manual coding would break this model** because:
- Developers would need to manually update line numbers in CLAUDE.md files
- Design decisions might not be captured in ARCHITECTURE.md
- Context files would drift out of sync with implementation
- Future Claude Code sessions would have incorrect context

**Build/test tools are NOT required to use Claude Code** - you can run Maven, pytest, and shell scripts manually or have Claude run them for you. The requirement is that **code changes and documentation updates must flow through Claude Code**.

## Claude Code Workflow

**Human-in-the-Loop Development:**

This project uses Claude Code to generate code, but **requires human review and validation** of all changes. Claude generates the implementation, and you verify correctness and apply tooling.

**Typical Session Flow:**

1. **Load Context Efficiently**
   - Start by asking Claude to read `ARCHITECTURE.md`
   - For module work, have Claude read the relevant `CLAUDE.md`
   - `.claudeignore` prevents Claude from reading build artifacts

2. **Design and Generate**
   - Describe what you want to accomplish
   - Let Claude propose the approach and update ARCHITECTURE.md if needed
   - Have Claude implement changes
   - Have Claude update CLAUDE.md with new line numbers and details

3. **Review in IntelliJ IDEA** (Required)
   - **Open all changed files** in IntelliJ
   - **Code → Reformat Code** - Apply consistent formatting
   - **Code → Optimize Imports** - Remove unused imports
   - **Code → Inspect Code** - Run IntelliJ inspections
   - **Apply IntelliJ suggestions** - Language-level improvements, type safety
   - **Review logic** - Verify correctness, edge cases, error handling
   - **Check tests** - Ensure test coverage is adequate

4. **Verify Functionality**
   - Ask Claude to run tests, or run them manually
   - Review test results
   - Fix any issues found during review or testing

5. **Iterate**
   - Make small, focused changes
   - Review each change in IntelliJ
   - Test after each change
   - Keep documentation synchronized

**Why IntelliJ Review is Required:**

- **Code quality** - IntelliJ inspections catch issues Claude might miss
- **Consistency** - IntelliJ formatting ensures uniform style
- **Correctness** - Human review validates logic and edge cases
- **Learning** - You understand what Claude generated and why
- **Optimization** - IntelliJ suggests language-level improvements

**Using Permissions:**
- `.claude/settings.local.json` pre-approves common commands
- Review permissions before approving destructive operations
- You maintain control while reducing approval friction

## Getting Help

**Issues and Bugs:**
- Open an issue: https://github.com/robfromboulder/viewmapper/issues
- Include: OS, Java version, Python version, error messages
- Attach: logs from `Developer | Open MCP Log File` (if Claude Desktop issue)

**Questions:**
- Check `ARCHITECTURE.md` for design rationale
- Check module `CLAUDE.md` files for implementation details
- Check `README.md` for user-facing documentation

**Pull Requests:**
- Fork the repository
- Create a feature branch
- **Use Claude Code to make all code and documentation changes**
- Verify ARCHITECTURE.md and CLAUDE.md files are updated
- Include transcript or summary of Claude Code session in PR description
- Submit PR with clear description of changes

## License

This project is licensed under the Apache License 2.0 - see LICENSE file for details.

---

**Thank you for contributing to ViewMapper!**

**Remember:** All contributions must be made through Claude Code to maintain the architecture-first development model. This keeps ARCHITECTURE.md and CLAUDE.md files synchronized with implementation and ensures future Claude Code sessions have accurate context.

**Your role as co-developer:** Claude generates code and documentation, you review in IntelliJ, apply tooling (reformat, optimize imports, inspections), verify correctness, and approve changes. This human-in-the-loop approach combines AI efficiency with human judgment and tooling quality.

If you're new to this approach, it may feel unusual at first, but you'll quickly discover that Claude Code handles the tedious work of implementation and documentation while you focus on design decisions, code quality review, and verification.
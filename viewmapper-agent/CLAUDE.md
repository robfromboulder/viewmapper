# Claude instructions for viewmapper-agent

The analytical half of ViewMapper: Trino connectivity, SQL parsing, dependency graph construction and analysis, and the reasoning loop that decides what to show.

**This module is the brain's `viewmapper/agent/` unit, token `VMR_AGENT`.** Read `../CLAUDE.md` for brain routing, the repo-wide runbook, and prose rules.

## Build Commands

```bash
# Full build
mvn clean package

# Run all tests
mvn test

# Run subset
mvn test -Dtest="TrinoSqlParser*"     # Parser
mvn test -Dtest="DependencyAnalyzer*" # Analyzer
mvn test -Dtest="*Discovery*"         # Discovery

# Run against test dataset
java -jar target/viewmapper-479.jar run --connection "test://simple_ecommerce" "<prompt>"

# Run against live Trino (multi-catalog, recommended)
java -jar target/viewmapper-479.jar run --connection "jdbc:trino://host:port?user=username" --schema catalog.schema "<prompt>"

# Run against live Trino (single-catalog)
java -jar target/viewmapper-479.jar run --connection "jdbc:trino://host:port/catalog?user=username" --schema name "<prompt>"
```

**Options:** `--schema <name>` (required for JDBC), `--output text|json` (default: text), `--verbose`

**Test datasets:** `simple_ecommerce` (11 views), `moderate_analytics` (35), `complex_enterprise` (154), `realistic_bi_warehouse` (86)

No live API calls or Trino connections required for tests. JUnit 5 + AssertJ. Agent tests use `MockChatModel`.

## Environment Variables

**Required (runtime only):**

- `ANTHROPIC_API_KEY_FOR_VIEWMAPPER` — agent-specific key (preferred)
- `ANTHROPIC_API_KEY` — fallback if the above is unset

**Optional:**

- `ANTHROPIC_MODEL` — defaults to `claude-3-7-sonnet-20250219`
- `ANTHROPIC_TIMEOUT_SECONDS` — defaults to 60

## Code Style

- Java 25
- 4-space indentation
- Copyright: `// © 2024-2026 Rob Dickinson (robfromboulder)`
- No wildcard imports (except Trino AST classes)
- Line length: prefer 100, max 130; single-line expressions when total < 130
- Private fields and visitor methods sorted alphabetically; public API before private helpers
- Javadoc on all public classes and methods; no `@see` tags, no `{@link}` for simple class names in prose; align `@param` with single space after tag
- Method body comments start with lowercase
- Prefer shorter variable names when type is clear
- Test method names describe scenario: `testDiamondPatternHasOneLeaf()`
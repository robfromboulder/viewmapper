# CLAUDE.md - ViewMapper Agent Module

## Module Overview

This module contains the core Java components of the ViewMapper project: SQL parsing, dependency analysis, and LLM agent orchestration for intelligent Trino schema exploration.

../ARCHITECTURE.md is very important for strategic decisions, please rely on this.

## Project Structure

```
agent/
├── pom.xml                          # Maven build configuration
├── README.md                        # User-facing documentation
├── CLAUDE.md                        # This file - AI context
├── datasets/                        # Test data for manual testing
│   ├── README.md                    # Dataset documentation
│   ├── simple_ecommerce.json        # 11 views - SIMPLE complexity
│   ├── moderate_analytics.json      # 35 views - MODERATE complexity
│   ├── complex_enterprise.json      # 154 views - COMPLEX complexity
│   └── realistic_bi_warehouse.json  # 86 views - COMPLEX complexity
└── src/
    ├── main/java/com/github/robfromboulder/viewmapper/
    │   ├── Main.java                # CLI entry point
    │   ├── RunCommand.java          # Primary CLI command
    │   ├── parser/                  # SQL parsing and dependency analysis
    │   │   ├── TrinoSqlParser.java
    │   │   ├── DependencyExtractor.java
    │   │   ├── TableReference.java
    │   │   └── DependencyAnalyzer.java
    │   └── agent/                   # LLM agent orchestration
    │       ├── ViewMapperAgent.java
    │       ├── AnthropicConfig.java
    │       ├── types/               # Agent data types
    │       │   ├── ComplexityLevel.java
    │       │   ├── SchemaComplexity.java
    │       │   ├── EntryPointSuggestion.java
    │       │   └── SubgraphResult.java
    │       └── tools/               # Agent tool executors
    │           ├── AnalyzeSchemaToolExecutor.java
    │           ├── SuggestEntryPointsToolExecutor.java
    │           ├── ExtractSubgraphToolExecutor.java
    │           └── GenerateMermaidToolExecutor.java
    └── test/java/...                # 18 test files, comprehensive coverage
```

## Key Components

### 1. SQL Parsing and Dependency Analysis (`parser/`)

**TrinoSqlParser.java**
- Wraps Trino's native SQL parser for accurate dependency extraction
- Main method: `extractDependencies(String sql) -> Set<TableReference>`
- Correctly handles CTEs, subqueries, UNNEST, quoted identifiers
- Does NOT extract table names from string literals or comments

**DependencyExtractor.java**
- AST visitor that traverses parsed SQL tree
- Tracks CTE names to exclude them from dependencies
- Handles all query types: SELECT, INSERT, CREATE VIEW, MERGE, DELETE
- Methods sorted alphabetically for maintainability

**TableReference.java**
- Model class representing a table/view reference
- Properties: catalog (optional), schema (optional), table (required)
- Key methods:
  - `getCatalog()`, `getSchema()`, `getTable()`
  - `getFullyQualifiedName()` - returns "catalog.schema.table"
  - `getDependencyCount()`, `getDependentCount()`

**Important Note on Identifier Normalization:**
Trino normalizes unquoted identifiers to lowercase. This is expected behavior:
- `SELECT * FROM Users` → dependency: "users"
- `SELECT * FROM "Users"` → dependency: "Users" (quoted preserves case)
- Tests use lowercase or quoted identifiers to account for this

**DependencyAnalyzer.java**
- Builds directed dependency graph using JGraphT
- Graph structure: edges point from dependencies → dependents (upstream → downstream)
- Uses `DefaultDirectedGraph` (not DAG) to handle potential cycles gracefully

**Core Algorithms (return types from `agent.types` package):**

1. **High-Impact Views** (`findHighImpactViews(int limit)`)
   - Finds views with most dependents (highest out-degree)
   - Returns: `Map<String, Integer>` (view name → dependent count)
   - Use case: Identify foundational/core views in schema

2. **Leaf Views** (`findLeafViews()`)
   - Finds views with zero dependents (out-degree = 0)
   - Returns: `List<String>` sorted alphabetically
   - Use case: Identify final outputs/reports

3. **Central Hubs** (`findCentralHubs(int limit)`)
   - Calculates betweenness centrality using JGraphT
   - Returns: `Map<String, Double>` (view name → centrality score)
   - Use case: Identify integration points connecting many sources to many consumers

4. **Subgraph Extraction** (`findSubgraph(...)`)
   - BFS traversal in both directions (upstream dependencies, downstream dependents)
   - Parameters:
     - `focusView`: Starting point
     - `depthUpstream`: How many levels of dependencies to include
     - `depthDownstream`: How many levels of dependents to include
     - `maxNodes`: Maximum nodes in result (0 = unlimited)
   - Returns: `Set<String>` of view names
   - Use case: Extract focused context around a specific view

**Graph Methods:**
- `addView(String viewName, String sql)` - Parse SQL and add to graph
- `getViewCount()` - Total nodes in graph
- `containsView(String viewName)` - Check if view exists
- `getGraph()` - Access underlying JGraphT graph

### 2. Agent Data Types (`agent.types/`)

These classes represent the structured data returned by agent tools. They're internal to the agent implementation.

**ComplexityLevel.java**
- Enum representing schema complexity based on view count
- Four levels: SIMPLE (0-19), MODERATE (20-99), COMPLEX (100-499), VERY_COMPLEX (500+)
- Each level defines strategy: full diagram vs. guided exploration
- Key methods:
  - `fromViewCount(int viewCount)` - Determines level from count
  - `isFullDiagramFeasible()` - Returns true for SIMPLE schemas
  - `requiresEntryPoint()` - Returns true for COMPLEX/VERY_COMPLEX
  - `getGuidance()` - Returns human-readable recommendation

**SchemaComplexity.java**
- Result object from schema analysis
- Properties: schemaName, viewCount, level (ComplexityLevel)
- Delegates to ComplexityLevel for strategy decisions
- Factory method: `fromViewCount(String schemaName, int viewCount)`

**EntryPointSuggestion.java**
- Result object from entry point suggestion tool
- Contains three strategies:
  - `highImpact` - Views with most dependents (foundational)
  - `leafViews` - Views with no dependents (final outputs)
  - `centralHubs` - Views with high betweenness centrality (integration points)
- Each strategy includes view names and counts/scores

**SubgraphResult.java**
- Result object from subgraph extraction
- Properties: focusView, viewNames (Set<String>), nodeCount
- Includes feasibility check: `isFeasible()` returns true if ≤50 nodes
- Used to determine if visualization is reasonable

### 3. LLM Agent System (`agent/`)

**ViewMapperAgent.java**
- Main orchestrator using LangChain4j's AiServices
- Contains embedded `SchemaExplorer` interface with comprehensive system prompt
- Three constructors:
  - Default: `ViewMapperAgent(DependencyAnalyzer)` - Uses environment configuration (AnthropicConfig.fromEnvironment())
  - Explicit config: `ViewMapperAgent(AnthropicConfig, DependencyAnalyzer)` - For custom configuration
  - Testing: `ViewMapperAgent(ChatLanguageModel, DependencyAnalyzer)` - Accepts mock model (package-private)
- Main method: `chat(String userQuery)` - Processes natural language queries
- Registers 4 tool executors for agentic reasoning

**AnthropicConfig.java**
- Configuration for Anthropic Claude API integration
- Loads from environment variables with fallback hierarchy:
  - `ANTHROPIC_API_KEY_FOR_VIEWMAPPER` - Agent-specific API key (recommended for production)
  - `ANTHROPIC_API_KEY` - Generic API key (fallback for development)
  - `ANTHROPIC_MODEL` - Optional model name (defaults to claude-3-7-sonnet-20250219)
  - `ANTHROPIC_TIMEOUT_SECONDS` - Optional timeout (defaults to 60)
- Factory method: `fromEnvironment()` throws IllegalStateException if no API key found
- Builder pattern available for manual configuration in tests
- Production best practice: Use agent-specific key for cost tracking, rate limits, and security isolation
- Note: Default uses Claude Sonnet for reliable tool calling; Haiku may not consistently execute tools

**System Prompt Strategy:**
The agent follows a deliberate reasoning approach:
1. Always assess complexity first using analyzeSchema
2. Recommend strategy based on complexity level
3. Explain different entry point strategies when needed
4. Extract subgraphs with depth control
5. Generate diagrams only when result is reasonable size
6. Maintain conversation context across turns

**Tool Executors (`agent/tools/`)**

All tools use LangChain4j's `@Tool` annotation for automatic registration:

1. **AnalyzeSchemaToolExecutor**
   - Method: `analyzeSchema(String schemaName)`
   - Returns: `SchemaComplexity` with view count and complexity level
   - Purpose: First step in exploration - understand scale

2. **SuggestEntryPointsToolExecutor**
   - Method: `suggestEntryPoints(String schemaName, int limit)`
   - Returns: `EntryPointSuggestion` with three strategies
   - Purpose: Help user choose where to start exploration

3. **ExtractSubgraphToolExecutor**
   - Method: `extractSubgraph(String focusView, int depthUpstream, int depthDownstream, int maxNodes)`
   - Returns: `SubgraphResult` with focused view set
   - Purpose: Extract manageable context around a view

4. **GenerateMermaidToolExecutor**
   - Method: `generateMermaid(String focusView, Set<String> viewNames)`
   - Returns: String (Mermaid diagram syntax)
   - Purpose: Create visual representation of dependencies
   - Format: `graph LR` with `A[view1] --> B[view2]` syntax

### 4. CLI (`Main.java` and `RunCommand.java`)

**Main.java**
- Entry point using Picocli framework
- Defines top-level command with help/version options
- Delegates to `RunCommand` subcommand
- Usage: `java -jar viewmapper-478.jar run [options] <query>`

**RunCommand.java**
- Primary CLI command implementation
- Parameters:
  - `query` (required) - Natural language question about schema
  - `--load <file>` - Load schema from JSON file (datasets/*)
  - `--output <format>` - Output format: text (default) or json
  - `--verbose` - Show debugging information

**Data Loading:**
- JSON structure: `{"description": "...", "views": [{"name": "...", "sql": "..."}]}`
- Parses SQL for each view and builds dependency graph
- Agent still calls real Anthropic API (--load only affects data source)

**Example Usage:**
```bash
# Basic query with test data
java -jar target/viewmapper-478.jar run --load datasets/simple_ecommerce.json "Show me the full dependency diagram"

# Complex schema requiring entry points
java -jar target/viewmapper-478.jar run --load datasets/complex_enterprise.json "What are the central hub views?"

# Focused subgraph extraction
java -jar target/viewmapper-478.jar run --load datasets/realistic_bi_warehouse.json "Show me 2 levels upstream from customer_360"

# JSON output for programmatic use
java -jar target/viewmapper-478.jar run --load datasets/moderate_analytics.json --output json "Analyze this schema"
```

## Testing Strategy

### Test Organization

**Total Test Files:** 18 files with comprehensive coverage across all layers

**Parser Tests (7 files):**
- `TrinoSqlParserBasicTests.java` - Basic SQL parsing
- `TrinoSqlParserEdgeTests.java` - Edge cases, string literals, comments
- `TrinoSqlParserWithClauseTests.java` - CTE handling
- `DependencyAnalyzerCentralHubTests.java` - Betweenness centrality
- `DependencyAnalyzerHighImpactTests.java` - Out-degree analysis
- `DependencyAnalyzerLeafViewTests.java` - Zero out-degree
- `DependencyAnalyzerSubgraphTests.java` - BFS extraction

**Agent Type Tests (4 files):**
- `ComplexityLevelTest.java` - Enum logic and thresholds
- `SchemaComplexityTest.java` - Complexity analysis results
- `EntryPointSuggestionTest.java` - Entry point recommendation format
- `SubgraphResultTest.java` - Subgraph extraction results

**Tool Executor Tests (4 files):**
- `AnalyzeSchemaToolExecutorTest.java` - Schema analysis tool
- `SuggestEntryPointsToolExecutorTest.java` - Entry point suggestion tool
- `ExtractSubgraphToolExecutorTest.java` - Subgraph extraction tool
- `GenerateMermaidToolExecutorTest.java` - Mermaid diagram generation

**Agent Tests (3 files):**
- `ViewMapperAgentTest.java` - End-to-end agent behavior with MockChatLanguageModel
- `MockChatLanguageModel.java` - Test double that returns canned responses (no API calls)
- `AnthropicConfigTest.java` - Environment variable loading and builder pattern

**Testing Philosophy:**
- All tests use JUnit 5 and AssertJ for fluent assertions
- Agent tests use `MockChatLanguageModel` to avoid API charges
- Tool executor tests verify correct calls to DependencyAnalyzer
- Type tests ensure correct classification and thresholds
- No integration tests require live Trino or Anthropic connections

### Test Patterns

**Common Test Structures:**
```java
// Simple linear chain: a -> b -> c -> d
analyzer.addView("b", "SELECT * FROM a");
analyzer.addView("c", "SELECT * FROM b");
analyzer.addView("d", "SELECT * FROM c");

// Diamond pattern
//     a
//    / \
//   b   c
//    \ /
//     d
analyzer.addView("b", "SELECT * FROM a");
analyzer.addView("c", "SELECT * FROM a");
analyzer.addView("d", "SELECT * FROM b JOIN c ON b.id = c.id");
```

**Real-World Scenarios:**
Many tests include realistic view names like `customer_360`, `marketing_report`, `executive_dashboard` to demonstrate practical usage.

## Build & Run

### Build JAR
```bash
mvn clean package
```
Produces:
- `target/original-viewmapper-478.jar` - Regular JAR
- `target/viewmapper-478.jar` - Fat JAR with all dependencies

### Run Tests
```bash
mvn test                              # All tests
mvn test -Dtest="TrinoSqlParser*"     # Parser tests only
mvn test -Dtest="DependencyAnalyzer*" # Analyzer tests only
```

## Dependencies

Key libraries (see `pom.xml`):
- **Trino Parser 478** - SQL parsing and AST (fully integrated)
- **JGraphT 1.5.2** - Graph algorithms (fully integrated)
- **LangChain4j 0.35.0** - LLM agent framework with Anthropic Claude integration (fully integrated)
- **Picocli 4.7.5** - CLI framework for command-line interface (fully integrated)
- **Jackson 2.16.1** - JSON parsing for dataset loading (fully integrated)
- **JUnit 5.10.1** - Testing framework (18 test files)
- **AssertJ 3.25.1** - Fluent assertions for tests

## Design Decisions

### Why Trino Parser Over Regex?

**Critical for accuracy at scale:**
- Regex fails on CTEs, subqueries, UNNEST, quoted identifiers, string literals
- With 2,347 views, false positives/negatives compound exponentially
- Trino parser is authoritative source of truth for Trino SQL

**Example of regex failure:**
```sql
WITH temp AS (
  SELECT * FROM schema1.table1
  WHERE description LIKE '%schema2.fake_table%'
)
SELECT * FROM temp JOIN schema3.table2 ON temp.id = table2.id
```
- Regex: `[schema1.table1, schema2.fake_table, temp, schema3.table2]` ❌
- Trino Parser: `[schema1.table1, schema3.table2]` ✅

### Why JGraphT?

- Industry-standard graph library with proven algorithms
- Built-in betweenness centrality implementation
- Type-safe, well-documented API
- Excellent performance for graphs with thousands of nodes

### Why DefaultDirectedGraph vs DirectedAcyclicGraph?

- Real-world schemas may have cycles (though rare)
- `DefaultDirectedGraph` handles cycles gracefully
- Prevents `IllegalArgumentException` on edge creation
- Graph analysis algorithms work correctly with cycles

## Code Quality Standards

### Established Patterns

1. **Copyright Headers:**
   All source files include: `// © 2024-2025 Rob Dickinson (robfromboulder)`

2. **Javadoc Comments:**
   - All public classes have class-level documentation
   - All public methods have method-level documentation
   - Examples included where helpful

3. **Method Ordering:**
   - Visitor methods in `DependencyExtractor` sorted alphabetically
   - Public API methods before private helpers

4. **Error Handling:**
   - Graph operations handle edge-already-exists gracefully
   - Return empty collections instead of null
   - Validate parameters where appropriate

5. **Testing:**
   - Test method names describe scenario: `testDiamondPatternHasOneLeaf()`
   - Comprehensive coverage of edge cases
   - Real-world scenario tests with descriptive view names

## Performance Considerations

### Current Scale
- Tested with small graphs (< 100 nodes)
- Designed for schemas with 1000+ views

### Optimization Opportunities
1. **Caching:** Cache betweenness centrality calculations
2. **Lazy Initialization:** Only calculate metrics when requested
3. **Incremental Updates:** Update graph without full rebuild
4. **Parallel Processing:** Parallelize subgraph extraction for multiple focus points

## Known Limitations

1. **Identifier Case Sensitivity:**
   - Trino normalizes unquoted identifiers to lowercase
   - Tests must account for this behavior
   - Use quoted identifiers to preserve case

2. **Cycle Handling:**
   - Cycles are allowed but may indicate schema design issues
   - Betweenness centrality handles cycles correctly
   - BFS may revisit nodes in cycles (handled with visited set)

3. **Column-Level Lineage:**
   - Currently tracks table/view dependencies only
   - Does not track column-to-column lineage
   - Future enhancement opportunity

## Debugging Tips

### Enable Maven Debug Output
```bash
mvn test -X -Dtest="TestClassName"
```

### View Parsed SQL AST
```java
Statement ast = parser.parse(sql);
System.out.println(ast.toString());
```

### Inspect Graph Structure
```java
Graph<String, DefaultEdge> graph = analyzer.getGraph();
System.out.println("Nodes: " + graph.vertexSet());
System.out.println("Edges: " + graph.edgeSet().size());
graph.edgeSet().forEach(edge -> {
    String source = graph.getEdgeSource(edge);
    String target = graph.getEdgeTarget(edge);
    System.out.println(source + " -> " + target);
});
```

### Test Individual Algorithms
```java
DependencyAnalyzer analyzer = new DependencyAnalyzer();
analyzer.addView("view1", "SELECT * FROM base");
analyzer.addView("view2", "SELECT * FROM view1");

// Test high-impact
Map<String, Integer> impact = analyzer.findHighImpactViews(10);
impact.forEach((view, count) ->
    System.out.println(view + ": " + count + " dependents")
);

// Test subgraph
Set<String> subgraph = analyzer.extractSubgraph("view1", 1, 1, 0);
System.out.println("Subgraph around view1: " + subgraph);
```

## Contributing Guidelines

### Code Style
- Java 24 language features
- 4-space indentation
- No wildcard imports (except for Trino AST classes)
- Line length: prefer 100 characters, max 130

### Commit Messages
```
Add feature: Brief description

Longer explanation if needed.
```

### Test Requirements
- All new features must have tests
- Maintain existing test coverage
- Test both happy path and edge cases
- Include at least one real-world scenario test

## Dataset Files for Manual Testing

The `datasets/` directory contains 4 curated JSON test files that allow manual testing without a live Trino connection:

1. **simple_ecommerce.json** (11 views)
   - Complexity: SIMPLE
   - Use case: Full diagram generation, smoke testing
   - Pattern: Linear chains, simple joins

2. **moderate_analytics.json** (35 views)
   - Complexity: MODERATE
   - Use case: Entry point suggestions, iterative exploration
   - Pattern: Star schema, cohort analysis, customer segmentation

3. **complex_enterprise.json** (154 views)
   - Complexity: COMPLEX
   - Use case: Large schema handling, progressive disclosure
   - Pattern: Multi-layer architecture (raw→staging→dim→fact→analytics)

4. **realistic_bi_warehouse.json** (86 views)
   - Complexity: COMPLEX
   - Use case: Production-quality BI with SCD Type 2, RFM analysis
   - Pattern: Time-series rollups, KPI dashboards

**JSON Format:**
```json
{
  "description": "Optional description of the schema",
  "views": [
    {
      "name": "view_name",
      "sql": "SELECT * FROM source_table"
    }
  ]
}
```

**See `datasets/README.md` for:**
- Detailed dataset descriptions
- 26+ example commands
- Testing scenarios (complexity analysis, entry points, subgraph extraction)
- Troubleshooting guide
- Instructions for creating custom datasets

## Agent Architecture Details

**Agentic Reasoning Flow:**
1. User submits natural language query via CLI
2. `RunCommand` loads schema (from file or Trino) into `DependencyAnalyzer`
3. `ViewMapperAgent` receives query and invokes `SchemaExplorer.chat()`
4. LangChain4j's AiServices:
   - Sends system prompt + user query to Claude
   - Claude decides which tools to call based on reasoning strategy
   - Tool executors interact with DependencyAnalyzer
   - Results flow back to Claude for synthesis
5. Claude generates natural language response with recommendations
6. Response returned to user via CLI

**Key Design Principles:**
- **Progressive Disclosure:** Don't overwhelm users with massive graphs
- **Guided Exploration:** Agent suggests next steps based on complexity
- **Tool Autonomy:** Claude decides when/how to use tools (not hardcoded logic)
- **Testability:** MockChatLanguageModel allows testing without API calls
- **Separation of Concerns:** Analyzer handles graph logic, agent handles UX

**Why LangChain4j?**
- Built-in support for Anthropic Claude with function calling
- `@Tool` annotation simplifies tool registration
- AiServices abstracts LLM orchestration
- Supports both production and test ChatLanguageModel implementations

## Related Documentation

- **README.md** - User-facing documentation for this module
- **../ARCHITECTURE.md** - Overall project architecture and design decisions
- **Trino SQL Parser Docs** - https://trino.io/docs/current/develop/sql-parser.html
- **JGraphT Documentation** - https://jgrapht.org/
- **LangChain4j Guide** - https://github.com/langchain4j/langchain4j

## Contact & Support

For questions about this module:
- Check existing tests for usage examples
- Review Javadoc comments in source code
- See ARCHITECTURE.md for design rationale
- Consult Trino/JGraphT documentation for library-specific questions

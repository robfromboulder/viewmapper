# CLAUDE.md - ViewMapper Agent Module

> **IMPORTANT:** See `../ARCHITECTURE.md` for strategic decisions and overall project architecture.

## Current Status

⚠️ **Active Development** - NOT production ready, still under development and testing
- Native Trino SQL parser for accurate dependency extraction
- Directed dependency graphs with JGraphT algorithms
- LLM agent with Claude via LangChain4j
- 4 specialized tools for progressive schema exploration
- CLI tool with 4 test datasets (11-154 views)
- Comprehensive test suite: 18 test files, all passing
- Embedded JAR resources (no external file dependencies)

## Completed Features

### JDBC Connectivity ✅
- Live Trino connections via `jdbc:trino://user:pass@host:port/catalog`
- Single-query schema introspection from `information_schema.views`
- Elegant catalog handling:
  - URL with catalog → schema parameter must be simple name (catalog-bound)
  - URL without catalog → schema parameter must be `catalog.schema` format
- No connection pooling (stateless CLI design)

## Future Enhancements

### Phase 1: Column-Level Lineage (Medium Priority)
- Track column-to-column dependencies
- Extend `TableReference` to include column info
- New tool: `traceColumnLineage(view, column)`

### Phase 2: Performance Optimization (Low Priority)
- Cache betweenness centrality calculations
- Lazy initialization for metrics
- Incremental graph updates
- Parallel subgraph extraction

### Phase 3: Enhanced Analysis (Future)
- Detect circular dependencies explicitly
- Impact analysis: "What breaks if I change view X?"
- Schema diff: Compare two schema versions
- Export to Neo4j or other graph databases

---

## Quick Reference

**What This Module Does:**
- Parses Trino SQL views using native Trino parser (not regex)
- Builds directed dependency graphs using JGraphT
- Provides LLM agent (via LangChain4j + Anthropic Claude) for natural language schema exploration
- CLI tool for interactive dependency mapping

**Key Files to Know:**
- `RunCommand.java` - CLI entry point, loads data and calls agent
- `ViewMapperAgent.java` - LLM orchestration with 4 tools
- `DependencyAnalyzer.java` - Graph algorithms (high-impact, leaf views, central hubs, subgraph extraction)
- `TrinoSqlParser.java` - SQL parsing wrapper
- Test datasets: `src/main/resources/datasets/*.json` (4 files, embedded in JAR)

**Build & Run:**
```bash
mvn clean package
java -jar target/viewmapper-478.jar run --connection "test://simple_ecommerce" "<prompt>"
```

**Test:**
```bash
mvn test                              # All tests (18 files)
mvn test -Dtest="TrinoSqlParser*"     # Parser tests only
```

---

## Architecture Overview

### Data Flow

```
User Prompt
    ↓
RunCommand (CLI)
    ↓
Load Schema → DependencyAnalyzer (builds graph)
    ↓
ViewMapperAgent (LangChain4j + Claude)
    ↓
Tool Executors (4 tools: analyze, suggest, extract, generate)
    ↓
DependencyAnalyzer (graph queries)
    ↓
Results → Claude → Natural Language Response
```

### Layer Architecture

1. **CLI Layer** (`Main.java`, `RunCommand.java`)
   - Picocli-based command-line interface
   - Loads test data or connects to live Trino via JDBC
   - Formats output (text or JSON)

2. **Agent Layer** (`agent/`)
   - `ViewMapperAgent` - LangChain4j orchestrator
   - `AnthropicConfig` - API configuration
   - 4 tool executors - Agent capabilities
   - Data types - Structured results

3. **Analysis Layer** (`parser/`)
   - `TrinoSqlParser` - SQL parsing
   - `DependencyExtractor` - AST traversal
   - `DependencyAnalyzer` - Graph algorithms
   - `TableReference` - Data model

### Key Design Principles

- **Progressive Disclosure:** Don't overwhelm users with massive graphs
- **Guided Exploration:** Agent suggests next steps based on complexity
- **Tool Autonomy:** Claude decides when/how to use tools (not hardcoded logic)
- **Accuracy First:** Use Trino parser, not regex (see Design Decisions)
- **Testability:** All components have comprehensive test coverage

---

## Key Components

### CLI Reference

#### RunCommand.java

**Usage:**
```bash
java -jar viewmapper-478.jar run --connection <string> [options] <prompt>
```

**Parameters:**
- `<prompt>` (required) - Natural language question about schema
- `--connection <string>` (required) - Connection string
  - `test://<dataset_name>` - Load from embedded JSON
  - `jdbc:trino://user:pass@host:port/catalog` - Live Trino connection
- `--schema <name>` - Trino schema name (required for JDBC connections)
  - Simple name if URL has catalog: `--schema analytics`
  - Qualified name if URL has no catalog: `--schema viewzoo.example`
- `--output <format>` - Output format: `text` (default) or `json`
- `--verbose` - Show debugging information

**Available Test Datasets:**
- `test://simple_ecommerce` - 11 views (SIMPLE)
- `test://moderate_analytics` - 35 views (MODERATE)
- `test://complex_enterprise` - 154 views (COMPLEX)
- `test://realistic_bi_warehouse` - 86 views (COMPLEX)

**Examples:**
```bash
# Test dataset
java -jar target/viewmapper-478.jar run --connection "test://simple_ecommerce" "Show me the full dependency diagram"

# JDBC with catalog in URL
java -jar target/viewmapper-478.jar run \
  --connection "jdbc:trino://user:pass@localhost:8080/production" \
  --schema analytics \
  "What are the high-impact views?"

# JDBC without catalog in URL
java -jar target/viewmapper-478.jar run \
  --connection "jdbc:trino://user:pass@localhost:8080" \
  --schema viewzoo.example \
  "Show me the dependency diagram"

# JSON output
java -jar target/viewmapper-478.jar run --connection "test://moderate_analytics" --output json "Analyze this schema"

# Verbose debugging
java -jar target/viewmapper-478.jar run \
  --connection "jdbc:trino://user:pass@localhost:8080/prod" \
  --schema analytics \
  --verbose "What are the leaf views?"
```

### Agent System

#### ViewMapperAgent.java

**Constructors:**
```java
// Default - uses environment config
ViewMapperAgent(DependencyAnalyzer analyzer)

// Custom config
ViewMapperAgent(AnthropicConfig config, DependencyAnalyzer analyzer)

// Testing - accepts mock model (package-private)
ViewMapperAgent(ChatLanguageModel model, DependencyAnalyzer analyzer)
```

**Main Method:**
```java
String chat(String userPrompt)
```

**System Prompt Strategy:**
1. Always assess complexity first using `analyzeSchema`
2. Recommend strategy based on complexity level
3. Explain different entry point strategies when needed
4. Extract subgraphs with depth control
5. Generate diagrams only when result is reasonable size
6. Maintain conversation context across turns

#### Agent Tools

All tools use LangChain4j's `@Tool` annotation for automatic registration:

**1. AnalyzeSchemaToolExecutor**
```java
SchemaComplexity analyzeSchema(String schemaName)
```
- Returns: View count and complexity level (SIMPLE/MODERATE/COMPLEX/VERY_COMPLEX)
- Purpose: First step in exploration - understand scale

**2. SuggestEntryPointsToolExecutor**
```java
EntryPointSuggestion suggestEntryPoints(String schemaName, int limit)
```
- Returns: Three strategies (high-impact, leaf views, central hubs)
- Purpose: Help user choose where to start exploration

**3. ExtractSubgraphToolExecutor**
```java
SubgraphResult extractSubgraph(String focusView, int depthUpstream, int depthDownstream, int maxNodes)
```
- Returns: Set of view names around focus view
- Purpose: Extract manageable context

**4. GenerateMermaidToolExecutor**
```java
String generateMermaid(String focusView, Set<String> viewNames)
```
- Returns: Mermaid diagram syntax (`graph LR` format)
- Purpose: Create visual representation

#### Agent Data Types (`agent/types/`)

**ComplexityLevel (Enum)**
- `SIMPLE` (0-19 views) - Full diagram feasible
- `MODERATE` (20-99 views) - Suggest grouping or iterative exploration
- `COMPLEX` (100-499 views) - Require entry point selection
- `VERY_COMPLEX` (500+ views) - Guided step-by-step exploration

Key Methods:
```java
ComplexityLevel.fromViewCount(int count)
boolean isFullDiagramFeasible()
boolean requiresEntryPoint()
String getGuidance()
```

**SchemaComplexity**
```java
class SchemaComplexity {
    String schemaName;
    int viewCount;
    ComplexityLevel level;
}

// Factory
SchemaComplexity.fromViewCount(String schemaName, int viewCount)
```

**EntryPointSuggestion**
```java
class EntryPointSuggestion {
    Map<String, Integer> highImpact;    // View → dependent count
    List<String> leafViews;             // Views with no dependents
    Map<String, Double> centralHubs;    // View → centrality score
}
```

**SubgraphResult**
```java
class SubgraphResult {
    String focusView;
    Set<String> viewNames;
    int nodeCount;

    boolean isFeasible()  // Returns true if ≤50 nodes
}
```

### Analysis Layer

#### DependencyAnalyzer.java

**Core Data Structure:**
- `DefaultDirectedGraph<String, DefaultEdge>` from JGraphT
- Edges point from dependencies → dependents (upstream → downstream)
- Uses `DefaultDirectedGraph` (not DAG) to handle cycles gracefully

**Graph Building:**
```java
void addView(String viewName, String sql)
int getViewCount()
boolean containsView(String viewName)
Graph<String, DefaultEdge> getGraph()
```

**Core Algorithms:**

**1. High-Impact Views**
```java
Map<String, Integer> findHighImpactViews(int limit)
```
- Finds views with most dependents (highest out-degree)
- Use case: Identify foundational/core views

**2. Leaf Views**
```java
List<String> findLeafViews()
```
- Finds views with zero dependents (out-degree = 0)
- Returns: Sorted alphabetically
- Use case: Identify final outputs/reports

**3. Central Hubs**
```java
Map<String, Double> findCentralHubs(int limit)
```
- Calculates betweenness centrality using JGraphT
- Use case: Identify integration points

**4. Subgraph Extraction**
```java
Set<String> extractSubgraph(String focusView, int depthUpstream, int depthDownstream, int maxNodes)
```
- BFS traversal in both directions
- Use case: Extract focused context

#### TrinoSqlParser.java

**Main Method:**
```java
Set<TableReference> extractDependencies(String sql)
```

**Key Features:**
- Wraps Trino's native SQL parser (authoritative for Trino SQL)
- Correctly handles: CTEs, subqueries, UNNEST, quoted identifiers
- Does NOT extract from: string literals, comments
- Uses `DependencyExtractor` AST visitor

**Important: Identifier Normalization**
Trino normalizes unquoted identifiers to lowercase:
- `SELECT * FROM Users` → dependency: `"users"`
- `SELECT * FROM "Users"` → dependency: `"Users"` (quoted preserves case)

#### DependencyExtractor.java

**Type:** AST visitor (extends Trino's `AstVisitor`)

**Responsibilities:**
- Traverses parsed SQL tree
- Tracks CTE names to exclude from dependencies
- Handles: SELECT, INSERT, CREATE VIEW, MERGE, DELETE
- Methods sorted alphabetically for maintainability

#### TableReference.java

**Properties:**
```java
class TableReference {
    String catalog;  // optional
    String schema;   // optional
    String table;    // required
}
```

**Key Methods:**
```java
String getCatalog()
String getSchema()
String getTable()
String getFullyQualifiedName()  // Returns "catalog.schema.table"
```

### Test Datasets

Located in: `src/main/resources/datasets/` (embedded in JAR)

**1. simple_ecommerce.json (11 views)**
- **Complexity:** SIMPLE
- **Patterns:** Linear chains, simple joins
- **Use case:** Full diagram generation, smoke testing
- **Key views:** `dim_customers`, `dim_products`, `fact_orders`, `customer_lifetime_value`, `executive_dashboard`

**2. moderate_analytics.json (35 views)**
- **Complexity:** MODERATE
- **Patterns:** Star schema, cohort analysis, customer segmentation
- **Use case:** Entry point suggestions, iterative exploration
- **Key views:** `dim_users`, `dim_products`, `dim_dates`, `fact_orders`, `customer_360`, `user_lifetime_value`

**3. complex_enterprise.json (154 views)**
- **Complexity:** COMPLEX
- **Patterns:** Multi-layer architecture (raw→staging→dim→fact→analytics)
- **Use case:** Large schema handling, progressive disclosure
- **Layers:** Raw (10), Staging (10), Dimensional (6), Fact (6), Business logic (100+), Dashboards (7)
- **Domains:** Customer, Product, Store, Employee, Supply chain, Financial

**4. realistic_bi_warehouse.json (86 views)**
- **Complexity:** COMPLEX
- **Patterns:** Star schema, SCD Type 2, time-series rollups
- **Use case:** Production BI, complex KPIs
- **Features:** SCD Type 2 dimensions, RFM analysis, fiscal calendar, promotion ROI, basket analysis

**JSON Format:**
```json
{
  "description": "Optional description",
  "views": [
    {"name": "view_name", "sql": "SELECT * FROM source_table"}
  ]
}
```

---

## Configuration

### AnthropicConfig.java

**Environment Variables (fallback hierarchy):**
- `ANTHROPIC_API_KEY_FOR_VIEWMAPPER` - Agent-specific key (recommended for production)
- `ANTHROPIC_API_KEY` - Generic key (fallback)
- `ANTHROPIC_MODEL` - Optional (defaults to `claude-3-7-sonnet-20250219`)
- `ANTHROPIC_TIMEOUT_SECONDS` - Optional (defaults to 60)

**Factory Method:**
```java
AnthropicConfig.fromEnvironment() // throws IllegalStateException if no key found
```

**Production Best Practice:**
Use agent-specific key for cost tracking, rate limits, and security isolation.

**Note:**
Default uses Claude Sonnet for reliable tool calling; Haiku may not consistently execute tools.

---

## Testing Strategy

### Test Organization (18 files)

**Parser Tests (7 files):**
- `TrinoSqlParserBasicTests` - Basic SQL parsing
- `TrinoSqlParserEdgeTests` - Edge cases, string literals, comments
- `TrinoSqlParserWithClauseTests` - CTE handling
- `DependencyAnalyzerCentralHubTests` - Betweenness centrality
- `DependencyAnalyzerHighImpactTests` - Out-degree analysis
- `DependencyAnalyzerLeafViewTests` - Zero out-degree
- `DependencyAnalyzerSubgraphTests` - BFS extraction

**Agent Type Tests (4 files):**
- `ComplexityLevelTest` - Enum logic and thresholds
- `SchemaComplexityTest` - Complexity analysis
- `EntryPointSuggestionTest` - Entry point format
- `SubgraphResultTest` - Subgraph results

**Tool Executor Tests (4 files):**
- `AnalyzeSchemaToolExecutorTest`
- `SuggestEntryPointsToolExecutorTest`
- `ExtractSubgraphToolExecutorTest`
- `GenerateMermaidToolExecutorTest`

**Agent Tests (3 files):**
- `ViewMapperAgentTest` - End-to-end with MockChatLanguageModel
- `MockChatLanguageModel` - Test double (no API calls)
- `AnthropicConfigTest` - Environment variable loading

### Testing Philosophy

- JUnit 5 + AssertJ for fluent assertions
- No live API calls or Trino connections required
- `MockChatLanguageModel` for agent testing
- Comprehensive edge case coverage
- Real-world scenario tests with descriptive view names

### Common Test Patterns

```java
// Linear chain: a -> b -> c -> d
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

---

## Development Guide

### Building

```bash
mvn clean package
```

**Produces:**
- `target/original-viewmapper-478.jar` - Regular JAR
- `target/viewmapper-478.jar` - Fat JAR with all dependencies

### Running Tests

```bash
mvn test                              # All tests
mvn test -Dtest="TrinoSqlParser*"     # Parser tests only
mvn test -Dtest="DependencyAnalyzer*" # Analyzer tests only
mvn test -X -Dtest="TestClassName"    # Debug mode
```

### Debugging

**View Parsed SQL AST:**
```java
Statement ast = parser.parse(sql);
System.out.println(ast.toString());
```

**Inspect Graph:**
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

**Test Individual Algorithms:**
```java
DependencyAnalyzer analyzer = new DependencyAnalyzer();
analyzer.addView("view1", "SELECT * FROM base");
analyzer.addView("view2", "SELECT * FROM view1");

Map<String, Integer> impact = analyzer.findHighImpactViews(10);
impact.forEach((view, count) ->
    System.out.println(view + ": " + count + " dependents")
);

Set<String> subgraph = analyzer.extractSubgraph("view1", 1, 1, 0);
System.out.println("Subgraph: " + subgraph);
```

### Code Quality Standards

**Copyright Headers:**
```java
// © 2024-2025 Rob Dickinson (robfromboulder)
```

**Javadoc:**
- All public classes have class-level documentation
- All public methods have method-level documentation
- Examples included where helpful

**Method Ordering:**
- Visitor methods in `DependencyExtractor` sorted alphabetically
- Public API methods before private helpers

**Error Handling:**
- Graph operations handle edge-already-exists gracefully
- Return empty collections instead of null
- Validate parameters where appropriate

**Testing:**
- Test method names describe scenario: `testDiamondPatternHasOneLeaf()`
- Comprehensive edge case coverage
- Real-world scenario tests

**Code Style:**
- Java 24 language features
- 4-space indentation
- No wildcard imports (except Trino AST classes)
- Line length: prefer 100 characters, max 130

---

## Design Decisions

### Why Trino Parser Over Regex?

**Critical for accuracy at scale:**
- Regex fails on: CTEs, subqueries, UNNEST, quoted identifiers, string literals
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
- **Regex:** `[schema1.table1, schema2.fake_table, temp, schema3.table2]` ❌
- **Trino Parser:** `[schema1.table1, schema3.table2]` ✅

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

### Why LangChain4j?

- Built-in support for Anthropic Claude with function calling
- `@Tool` annotation simplifies tool registration
- AiServices abstracts LLM orchestration
- Supports both production and test ChatLanguageModel implementations

---

## Known Limitations

### 1. Identifier Case Sensitivity
- Trino normalizes unquoted identifiers to lowercase
- Tests must account for this behavior
- Use quoted identifiers to preserve case

### 2. Cycle Handling
- Cycles allowed but may indicate schema design issues
- Betweenness centrality handles cycles correctly
- BFS uses visited set to prevent infinite loops

### 3. Column-Level Lineage
- Currently tracks table/view dependencies only
- Does not track column-to-column lineage
- Future enhancement opportunity

### 4. Catalog Handling
- JDBC URLs with catalog bind conversation to that catalog
- JDBC URLs without catalog allow querying any catalog.schema
- Schema parameter parsing uses first period as separator (`split("\\.", 2)`)

---

## Performance Considerations

**Current Scale:**
- Tested with datasets up to 154 views
- Designed for schemas with 1000+ views
- All algorithms run in reasonable time for test datasets

**Optimization Opportunities:**
1. **Cache betweenness centrality calculations** - Expensive operation (O(V*E))
2. **Lazy initialization for metrics** - Compute only when requested
3. **Incremental graph updates** - Add single view without rebuilding
4. **Parallel subgraph extraction** - Multiple BFS searches concurrently

**Memory Usage:**
- Graph structure: ~100 bytes per view
- Test datasets: < 1 MB total
- Scales linearly with view count

**Benchmarks (Test Datasets):**
- Parse 154 views: < 500ms
- Build full graph: < 100ms
- Betweenness centrality (154 nodes): < 200ms
- Subgraph extraction: < 10ms

---

## Dependencies

**Build Tool:**
- **Maven 3.8+** - Build and dependency management

**Runtime:**
- **JDK 24** - Java compiler and runtime

**Core Libraries:**
- **Trino Parser 478** - SQL parsing and AST traversal
- **JGraphT 1.5.2** - Graph algorithms and data structures
- **LangChain4j 0.35.0** - LLM agent framework with Anthropic support
- **Picocli 4.7.5** - CLI framework with annotations
- **Jackson 2.16.1** - JSON parsing for test datasets

**Testing:**
- **JUnit 5.10.1** - Unit testing framework
- **AssertJ 3.25.1** - Fluent assertion library

**See:** `pom.xml` for complete dependency list with versions

---

## References

### Project Documentation
- `README.md` - User-facing documentation and quick start
- `CONTRIBUTING.md` - Development workflow and contribution guidelines
- `../ARCHITECTURE.md` - Overall project architecture (⚠️ READ THIS for strategic decisions)
- `../viewmapper-mcp-server/CLAUDE.md` - MCP server implementation details

### External Documentation
- [Trino SQL Parser Docs](https://trino.io/docs/current/develop/sql-parser.html)
- [JGraphT Documentation](https://jgrapht.org/)
- [LangChain4j Guide](https://github.com/langchain4j/langchain4j)
- [Picocli Documentation](https://picocli.info/)

### Related Resources
- [Anthropic Claude API](https://docs.anthropic.com/)
- [Mermaid Diagram Syntax](https://mermaid.js.org/)

---

## Project Structure

```
viewmapper-agent/
├── pom.xml                          # Maven build configuration
├── README.md                        # User-facing documentation
├── CONTRIBUTING.md                  # Development workflow
├── CLAUDE.md                        # This file - AI context
└── src/
    ├── main/
    │   ├── java/com/github/robfromboulder/viewmapper/
    │   │   ├── Main.java                # CLI entry point
    │   │   ├── RunCommand.java          # Primary CLI command
    │   │   ├── parser/                  # SQL parsing and dependency analysis
    │   │   │   ├── TrinoSqlParser.java
    │   │   │   ├── DependencyExtractor.java
    │   │   │   ├── TableReference.java
    │   │   │   └── DependencyAnalyzer.java
    │   │   └── agent/                   # LLM agent orchestration
    │   │       ├── ViewMapperAgent.java
    │   │       ├── AnthropicConfig.java
    │   │       ├── types/               # Agent data types
    │   │       │   ├── ComplexityLevel.java
    │   │       │   ├── SchemaComplexity.java
    │   │       │   ├── EntryPointSuggestion.java
    │   │       │   └── SubgraphResult.java
    │   │       └── tools/               # Agent tool executors
    │   │           ├── AnalyzeSchemaToolExecutor.java
    │   │           ├── SuggestEntryPointsToolExecutorjava
    │   │           ├── ExtractSubgraphToolExecutor.java
    │   │           └── GenerateMermaidToolExecutor.java
    │   └── resources/
    │       └── datasets/                # Test data embedded in JAR
    │           ├── simple_ecommerce.json        # 11 views - SIMPLE
    │           ├── moderate_analytics.json      # 35 views - MODERATE
    │           ├── complex_enterprise.json      # 154 views - COMPLEX
    │           └── realistic_bi_warehouse.json  # 86 views - COMPLEX
    └── test/java/...                # 18 test files, comprehensive coverage
```

# ViewMapper Test Data

This directory contains JSON test data files for manual testing of the ViewMapper agent without requiring a live Trino connection.

## Test Data Files

### 1. simple_ecommerce.json
**Complexity:** SIMPLE (11 views)
**Description:** Basic e-commerce schema with customer, product, and order tracking.

**Use Cases:**
- Testing full diagram generation (complexity < 20 views)
- Quick smoke testing of agent functionality
- Demonstrating simple dependency chains

**Key Views:**
- `dim_customers`, `dim_products`, `fact_orders` - Foundation tables
- `customer_lifetime_value` - Intermediate aggregation
- `executive_dashboard` - Final reporting view

**Dependency Patterns:**
- Linear chains: `a → b → c → d`
- Simple joins: Customer + Orders
- Basic aggregations

**Example Test Query:**
```bash
java -jar target/viewmapper-478.jar run --load datasets/simple_ecommerce.json "Show me the full dependency diagram"
```

---

### 2. moderate_analytics.json
**Complexity:** MODERATE (35 views)
**Description:** Analytics platform with user engagement, orders, payments, and support tracking.

**Use Cases:**
- Testing iterative exploration strategy
- Domain grouping recommendations
- Entry point suggestion algorithms

**Key Views:**
- Dimensional: `dim_users`, `dim_products`, `dim_dates`
- Fact tables: `fact_orders`, `fact_payments`, `fact_page_views`, `fact_support_tickets`
- Analytics: `customer_360`, `user_lifetime_value`, `retention_analysis`
- Dashboards: `executive_dashboard`, `marketing_report`, `operations_report`

**Dependency Patterns:**
- Star schema joins
- Multiple fact tables
- Cohort analysis
- Customer segmentation

**Example Test Queries:**
```bash
# Test entry point suggestions
java -jar target/viewmapper-478.jar run --load datasets/moderate_analytics.json "What are the high-impact views in this schema?"

# Test subgraph extraction
java -jar target/viewmapper-478.jar run --load datasets/moderate_analytics.json "Show me everything that depends on user_lifetime_value"
```

---

### 3. complex_enterprise.json
**Complexity:** COMPLEX (154 views)
**Description:** Large enterprise data warehouse with staging layers, multiple business domains, and extensive analytics.

**Use Cases:**
- Testing complex schema handling
- Entry point requirement enforcement
- Progressive disclosure strategy
- Large subgraph extraction with depth control

**Key Views:**
- Raw layer: `raw_customers`, `raw_orders`, `raw_products`, etc. (10 views)
- Staging layer: `stg_*` views (10 views)
- Dimensional layer: `dim_*` views (6 views)
- Fact tables: `fact_*` views (6 views)
- Business logic: 100+ aggregation and analytics views
- 360-degree views: `customer_360`, `product_360`, `store_360`
- Dashboards: 7 domain-specific dashboards

**Business Domains:**
- Customer analytics (segments, cohorts, retention, churn)
- Product analytics (sales, margins, inventory)
- Store operations (performance, regions, productivity)
- Employee management (sales, productivity, departments)
- Supply chain (suppliers, inventory, shipments)
- Financial analysis (revenue, refunds, ROI)

**Dependency Patterns:**
- Multi-layer architecture (raw → staging → dimensional → fact → analytics)
- Diamond patterns (multiple paths to same view)
- Hub nodes with high betweenness centrality
- Deep dependency chains (5+ levels)

**Example Test Queries:**
```bash
# Test complexity analysis
java -jar target/viewmapper-478.jar run --load datasets/complex_enterprise.json "Analyze this schema"

# Test entry point suggestions
java -jar target/viewmapper-478.jar run --load datasets/complex_enterprise.json "What are the central hub views?"

# Test subgraph with depth control
java -jar target/viewmapper-478.jar run --load datasets/complex_enterprise.json "Show me 2 levels upstream and 1 level downstream from customer_360"
```

---

### 4. realistic_bi_warehouse.json
**Complexity:** COMPLEX (86 views)
**Description:** Production-quality business intelligence data warehouse with star schema design and comprehensive analytics.

**Use Cases:**
- Demonstrating real-world BI architecture
- Testing slowly changing dimensions (SCD Type 2)
- Complex KPI calculations
- Cross-functional dashboard dependencies

**Key Views:**
- Dimensions with SCD Type 2: `dim_customer`, `dim_product`, `dim_employee`, `dim_store`, `dim_supplier`, `dim_promotion`
- Date dimension: `dim_date` (with fiscal calendar support)
- Fact tables: `fact_sales`, `fact_inventory`, `fact_sales_target`
- Time-series aggregations: daily/weekly/monthly/quarterly/yearly views
- Customer analytics: RFM analysis, segmentation, cohorts, retention, churn risk
- Product analytics: sales, margins, turnover, slow-movers
- Store analytics: performance, productivity, same-store sales
- Employee analytics: sales attribution, productivity
- Marketing: promotion ROI, basket analysis, cross-sell
- KPI dashboards: executive, sales ops, customer insights, inventory, marketing

**Dependency Patterns:**
- Classic star schema (fact → dimensions)
- Time-series rollups (daily → weekly → monthly → quarterly → yearly)
- Multi-level aggregations (detail → summary → KPI)
- Cross-functional analytics (combining multiple domains)

**Example Test Queries:**
```bash
# Test high-impact view identification
java -jar target/viewmapper-478.jar run --load datasets/realistic_bi_warehouse.json "Which views have the most dependents?"

# Test leaf view identification
java -jar target/viewmapper-478.jar run --load datasets/realistic_bi_warehouse.json "What are the final output views?"

# Test focused exploration
java -jar target/viewmapper-478.jar run --load datasets/realistic_bi_warehouse.json "Show me the dependency chain for executive_kpi_dashboard"
```

---

## Usage

### Basic Usage

```bash
# Run the agent with test data
java -jar target/viewmapper-478.jar run --load datasets/<file>.json "<your query>"
```

### Loading Test Data

The test data files follow this JSON structure:

```json
{
  "description": "Optional description",
  "views": [
    {
      "name": "view_name",
      "sql": "SELECT * FROM source_table"
    }
  ]
}
```

### Output Formats

```bash
# Default text output
java -jar target/viewmapper-478.jar run --load datasets/simple_ecommerce.json "Analyze this schema"

# JSON output
java -jar target/viewmapper-478.jar run --load datasets/simple_ecommerce.json --output json "Analyze this schema"

# Verbose mode (shows agent reasoning)
java -jar target/viewmapper-478.jar run --load datasets/simple_ecommerce.json --verbose "What are the leaf views?"
```

---

## Complexity Levels

ViewMapper categorizes schemas into four complexity levels:

| Level | View Count | Strategy | Test File |
|-------|-----------|----------|-----------|
| **SIMPLE** | 0-19 | Full diagram feasible | simple_ecommerce.json |
| **MODERATE** | 20-99 | Suggest grouping or iterative exploration | moderate_analytics.json |
| **COMPLEX** | 100-499 | Require entry point selection | complex_enterprise.json, realistic_bi_warehouse.json |
| **VERY_COMPLEX** | 500+ | Guided step-by-step exploration | _(not provided)_ |

---

## Testing Scenarios

### 1. Schema Analysis
Test the agent's ability to assess complexity and recommend strategies.

```bash
# Simple schema - should suggest full diagram
java -jar target/viewmapper-478.jar run --load datasets/simple_ecommerce.json "Analyze this schema"

# Complex schema - should require entry point
java -jar target/viewmapper-478.jar run --load datasets/complex_enterprise.json "Analyze this schema"
```

### 2. Entry Point Suggestions

```bash
# High-impact views (foundational views with most dependents)
java -jar target/viewmapper-478.jar run --load datasets/moderate_analytics.json "What are the high-impact views?"

# Leaf views (final outputs with no dependents)
java -jar target/viewmapper-478.jar run --load datasets/moderate_analytics.json "What are the leaf views?"

# Central hubs (integration points with high betweenness centrality)
java -jar target/viewmapper-478.jar run --load datasets/complex_enterprise.json "What are the central hub views?"
```

### 3. Subgraph Extraction

```bash
# Default depth (2 upstream, 1 downstream)
java -jar target/viewmapper-478.jar run --load datasets/realistic_bi_warehouse.json "Show me the dependencies around customer_lifetime_value"

# Custom depth
java -jar target/viewmapper-478.jar run --load datasets/complex_enterprise.json "Extract 3 levels upstream and 2 levels downstream from executive_dashboard"
```

### 4. Diagram Generation

```bash
# Simple schema - full diagram
java -jar target/viewmapper-478.jar run --load datasets/simple_ecommerce.json "Generate a Mermaid diagram"

# Complex schema - focused diagram with entry point
java -jar target/viewmapper-478.jar run --load datasets/complex_enterprise.json "Generate a diagram starting from customer_360"
```

### 5. Progressive Exploration

```bash
# Start with analysis
java -jar target/viewmapper-478.jar run --load datasets/moderate_analytics.json "Analyze this schema"

# Get suggestions
java -jar target/viewmapper-478.jar run --load datasets/moderate_analytics.json "Suggest entry points"

# Explore from entry point
java -jar target/viewmapper-478.jar run --load datasets/moderate_analytics.json "Show me everything related to user_lifetime_value"
```

---

## Validation

All test data files have been validated to ensure:
- Valid JSON syntax
- Parseable SQL statements (Trino-compatible)
- Realistic dependency patterns
- Proper view naming conventions
- Representative complexity levels

You can validate the files programmatically:

```bash
# Run unit tests that use these files
mvn test -Dtest="*ToolExecutor*"
```

---

## Creating Custom Test Data

To create your own test data file:

1. **Define your schema:**
   ```json
   {
     "description": "Optional description of your test scenario",
     "views": []
   }
   ```

2. **Add views with dependencies:**
   ```json
   {
     "name": "base_table",
     "sql": "SELECT * FROM raw.source"
   },
   {
     "name": "derived_view",
     "sql": "SELECT * FROM base_table WHERE active = true"
   }
   ```

3. **Follow naming conventions:**
   - Use lowercase or quoted identifiers (Trino normalizes unquoted to lowercase)
   - Use qualified names for clarity: `schema.table` or `catalog.schema.table`
   - Follow domain patterns: `dim_*`, `fact_*`, `stg_*`, `raw_*`

4. **Test your file:**
   ```bash
   java -jar target/viewmapper-478.jar run --load datasets/your_file.json "Analyze this schema"
   ```

---

## Troubleshooting

### Issue: "Failed to parse SQL"
- Check that your SQL is Trino-compatible
- Ensure proper quoting of identifiers with special characters
- Validate table references exist in the schema

### Issue: "No views found in schema"
- Verify JSON structure matches expected format
- Check that `views` array is not empty
- Ensure each view has both `name` and `sql` fields

### Issue: "Circular dependency detected"
- This is expected behavior for some schemas
- ViewMapper handles cycles gracefully using DefaultDirectedGraph
- Circular dependencies indicate potential schema design issues

---

## License

© 2024-2025 Rob Dickinson (robfromboulder)

---

## Additional Resources

- **Main README:** ../README.md
- **Architecture:** ../ARCHITECTURE.md
- **Claude Context:** ../CLAUDE.md
- **Test Suite:** src/test/java/com/github/robfromboulder/viewmapper/

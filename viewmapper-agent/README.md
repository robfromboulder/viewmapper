# viewmapper-agent
This module contains the core agent for the ViewMapper project, based on LangChain4j, and using Trino's native SQL parser to accurately extract view-to-view and view-to-table dependencies.

## Dependencies

- **JDK 24** - Java compiler and runtime 
- **Trino-Parser** - SQL parsing and AST
- **Jackson** - JSON processing
- **LangChain4j** - LLM agent framework
- **Picocli** - CLI framework
- **JUnit 5** - Testing framework
- **AssertJ** - Fluent assertions

## Usage

Build binary package:
```bash
mvn clean package
```

### Running with Test Datasets

```bash
java -jar target/viewmapper-478.jar run --connection "test://simple_ecommerce" "Show me the dependency diagram"
```

### Running with Live Trino Connection

**Option 1: Catalog in URL (catalog-bound conversation)**
```bash
java -jar target/viewmapper-478.jar run \
  --connection "jdbc:trino://localhost:8080/production?user=youruser" \
  --schema analytics \
  "What are the high-impact views?"
```

**Option 2: No catalog in URL (multi-catalog queries)**
```bash
java -jar target/viewmapper-478.jar run \
  --connection "jdbc:trino://localhost:8080?user=youruser" \
  --schema viewzoo.example \
  "Show me the dependency diagram"
```

### Other Options

```bash
# JSON output
java -jar target/viewmapper-478.jar run --connection "test://simple_ecommerce" --output json "Analyze this schema"

# Verbose mode (shows agent reasoning and connection details)
java -jar target/viewmapper-478.jar run \
  --connection "jdbc:trino://localhost:8080/prod?user=youruser" \
  --schema analytics \
  --verbose "What are the leaf views?"
```

## Using Test Datasets

ViewMapper includes test data files embedded in the JAR, for testing the agent without requiring a live Trino connection.

### 1. simple_ecommerce

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
java -jar target/viewmapper-478.jar run --connection "test://simple_ecommerce" "Show me the full dependency diagram"
```

### 2. moderate_analytics

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
java -jar target/viewmapper-478.jar run --connection "test://moderate_analytics" "What are the high-impact views in this schema?"

# Test subgraph extraction
java -jar target/viewmapper-478.jar run --connection "test://moderate_analytics" "Show me everything that depends on user_lifetime_value"
```

### 3. realistic_bi_warehouse

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
java -jar target/viewmapper-478.jar run --connection "test://realistic_bi_warehouse" "Which views have the most dependents?"

# Test leaf view identification
java -jar target/viewmapper-478.jar run --connection "test://realistic_bi_warehouse" "What are the final output views?"

# Test focused exploration
java -jar target/viewmapper-478.jar run --connection "test://realistic_bi_warehouse" "Show me the dependency chain for executive_kpi_dashboard"
```

### 4. complex_enterprise

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
java -jar target/viewmapper-478.jar run --connection "test://complex_enterprise" "Analyze this schema"

# Test entry point suggestions
java -jar target/viewmapper-478.jar run --connection "test://complex_enterprise" "What are the central hub views?"

# Test subgraph with depth control
java -jar target/viewmapper-478.jar run --connection "test://complex_enterprise" "Show me 2 levels upstream and 1 level downstream from customer_360"
```

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

## Additional Documentation

- **../ARCHITECTURE.md** - Overall project architecture and design decisions
- **CLAUDE.md** - AI assistant context and development guidelines
- **TESTING.md** - Comprehensive testing scenarios and examples

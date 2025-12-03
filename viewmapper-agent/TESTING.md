# TESTING.md

ViewMapper categorizes all schemas into four complexity levels:

| Level | View Count | Strategy | Test File |
|-------|-----------|----------|-----------|
| **SIMPLE** | 0-19 | Full diagram feasible | simple_ecommerce |
| **MODERATE** | 20-99 | Suggest grouping or iterative exploration | moderate_analytics |
| **COMPLEX** | 100-499 | Require entry point selection | complex_enterprise, realistic_bi_warehouse |
| **VERY_COMPLEX** | 500+ | Guided step-by-step exploration | _(not provided)_ |

## 1. Schema Analysis
Test the agent's ability to assess complexity and recommend strategies.

```bash
# Simple schema - should suggest full diagram
java -jar target/viewmapper-478.jar run --connection "test://simple_ecommerce" "Analyze this schema"

# Complex schema - should require entry point
java -jar target/viewmapper-478.jar run --connection "test://complex_enterprise" "Analyze this schema"
```

## 2. Entry Point Suggestions

```bash
# High-impact views (foundational views with most dependents)
java -jar target/viewmapper-478.jar run --connection "test://moderate_analytics" "What are the high-impact views?"

# Leaf views (final outputs with no dependents)
java -jar target/viewmapper-478.jar run --connection "test://moderate_analytics" "What are the leaf views?"

# Central hubs (integration points with high betweenness centrality)
java -jar target/viewmapper-478.jar run --connection "test://complex_enterprise" "What are the central hub views?"
```

## 3. Subgraph Extraction

```bash
# Default depth (2 upstream, 1 downstream)
java -jar target/viewmapper-478.jar run --connection "test://realistic_bi_warehouse" "Show me the dependencies around customer_lifetime_value"

# Custom depth
java -jar target/viewmapper-478.jar run --connection "test://complex_enterprise" "Extract 3 levels upstream and 2 levels downstream from executive_dashboard"
```

## 4. Diagram Generation

```bash
# Simple schema - full diagram
java -jar target/viewmapper-478.jar run --connection "test://simple_ecommerce" "Generate a Mermaid diagram"

# Complex schema - focused diagram with entry point
java -jar target/viewmapper-478.jar run --connection "test://complex_enterprise" "Generate a diagram starting from customer_360"
```

## 5. Progressive Exploration

```bash
# Start with analysis
java -jar target/viewmapper-478.jar run --connection "test://moderate_analytics" "Analyze this schema"

# Get suggestions
java -jar target/viewmapper-478.jar run --connection "test://moderate_analytics" "Suggest entry points"

# Explore from entry point
java -jar target/viewmapper-478.jar run --connection "test://moderate_analytics" "Show me everything related to user_lifetime_value"
```
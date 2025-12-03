# viewmapper
This Claude-based agent helps you discover and map relationships between Trino views, even for schemas with thousands of views or deeply nested hierarchies.

[![Claude Code](https://img.shields.io/badge/Built%20with%20Claude%20Code-6366f1?logo=claude&logoColor=white)](https://claude.ai/code)
[![Contributing](https://img.shields.io/badge/contributions-welcome-green.svg)](https://github.com/robfromboulder/viewmapper/blob/main/CONTRIBUTING.md)
[![License](https://img.shields.io/github/license/robfromboulder/viewmapper)](https://github.com/robfromboulder/viewmapper/blob/main/LICENSE)

Let's say you have a simple view hierarchy like this:
```sql
create or replace view example.b as select * from example.a
create or replace view example.c as select * from example.b
```

That's a lot of reading as the schema grows to hundreds of views. And because these views aren't related by foreign keys, many ERD tools don't properly report these relationships. ☹️

ViewMapper uses the Trino parser to properly report on all view relationships, even when using complex JOIN, UNION, or WITH clauses in your view definitions.
With its understanding of view relationships, ViewMapper can easily display this hierarchy into a Mermaid diagram.
```mermaid
graph TD
    node1["example.c"]
    node2["example.b"]
    node3["example.a"]

    node1 --> node2
    node2 --> node3
```

If the hierarchy gets too complex to be displayed as a single diagram, ViewMapper helps you map dependencies around a view of particular interest, like `customer_360` in this example.
```mermaid
graph TB
    node1["customer_segments"]
    node2["customer_360"]
    node3["stg_customers"]
    node4["dim_customers"]
    node5["at_risk_customers"]
    node6["churn_risk_score"]
    node7["fact_orders"]
    node8["fact_payments"]
    node9["customer_lifetime_value"]

    node1 --> node2
    node3 --> node4
    node4 --> node9
    node4 --> node5
    node4 --> node6
    node4 --> node2
    node5 --> node6
    node5 --> node2
    node6 --> node2
    node7 --> node9
    node7 --> node5
    node8 --> node9
    node9 --> node1
    node9 --> node6
    node9 --> node2

    style node2 fill:#FF6B6B,stroke:#D32F2F,stroke-width:3px
    style node1 fill:#6366f1,stroke:#1976D2
    style node4 fill:#6366f1,stroke:#1976D2
    style node5 fill:#6366f1,stroke:#1976D2
    style node6 fill:#6366f1,stroke:#1976D2
    style node9 fill:#6366f1,stroke:#1976D2
```

If you don't know anything about the schema or where to start, that's ok too. ViewMapper will suggest views that are interesting entry points, including **high-impact views** (with highest number of dependencies), **central-hub views** (with highest centrality among consumers), or **leaf views** (final views with no dependents) to help you get your bearings.

You can use ViewMapper from the command line, from other agentic systems, or directly from Claude Desktop (for interactive use and native rendering of Mermaid diagram files)

<img width="2171" height="1395" alt="Screenshot 2025-12-02 at 5 36 11 PM" src="https://github.com/user-attachments/assets/8356e721-6926-4083-86d4-d1ea2bcdf642" />


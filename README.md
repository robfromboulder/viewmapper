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
With its understanding of view relationships, ViewMapper can easily render this hierarchy into a Mermaid diagram:
```mermaid
graph TD
    node1["example.a"]
    node2["example.b"]
    node3["example.c"]

    node1 --> node2
    node2 --> node3
```

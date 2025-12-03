# Contributing to viewmapper-agent

## Using Claude Code

This project uses (but does not require) [Claude Code](https://claude.ai/code) for help with development, testing, and maintenance. The `CLAUDE.md` file provides project context for Claude Code sessions.

If you haven't used Claude Code yet, these resources are super helpful:
* https://code.claude.com/docs
* https://www.anthropic.com/engineering/claude-code-best-practices
* https://www.siddharthbharath.com/claude-code-the-complete-guide/

## Coding Conventions

Our code style is whatever IntelliJ IDEA does by default, with the exception of allowing lines up to 130 characters. If you don't use IDEA, that's ok, but your code may get reformatted later.

All source files should use this copyright statement:
```
© 2024-2025 Rob Dickinson (robfromboulder)
```

## Applying Security Updates

Scan for newer library versions:
```bash
mvn versions:display-dependency-updates
```

Scan for known vulnerabilities:
```bash
trivy filesystem .
```

## Creating Test Datasets

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

4. **Place the file in:** `src/main/resources/datasets/`

5. **Test your file:**
   ```bash
   mvn package && java -jar target/viewmapper-478.jar run --connection "test://your_file" "Analyze this schema"
   ```

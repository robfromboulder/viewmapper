# Contributing to viewmapper-mcp-server

## Using Claude Code

This project requires [Claude Code](https://claude.ai/code) for all development, testing, and maintenance. All code changes must be made through Claude Code sessions to maintain the architecture-first development model. The `CLAUDE.md` file provides project context for Claude Code sessions.

## Coding Conventions

Our code style is whatever IntelliJ IDEA does by default, with the exception of allowing lines up to 130 characters. If you don't use IDEA, that's ok, but your code may get reformatted later.

All source files should use this copyright statement: (followed by a blank line)
```
© 2024-2025 Rob Dickinson (robfromboulder)
```

## Applying Security Updates

Scan for newer Python package versions:
```bash
pip list --outdated
```

Scan for known vulnerabilities:
```bash
pip-audit
```

Or use safety (alternative):
```bash
safety check
```

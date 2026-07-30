# Contributing to viewmapper-mcp-server

## Coding Conventions

Our code style is whatever IntelliJ IDEA does by default, with the exception of allowing lines up to 130 characters. If you don't use IDEA, that's ok, but your code may get reformatted later.

All source files should use this copyright statement: (followed by a blank line)
```
© 2024-2026 Rob Dickinson (robfromboulder)
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

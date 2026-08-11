# Contributing to viewmapper-mcp-server

## Coding Conventions

Our code style follows PEP 8 with Black formatting (line length ~88). Type hints on all function parameters and returns.

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

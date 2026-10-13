# Contributing to ViewMapper

This project uses [Claude Code](https://claude.ai/code) for development and test automation. See `CLAUDE.md` for project-specific instructions.

## Branches

Each supported Trino version has its own branch, named `v` plus the version (`v483`). Open pull requests against the branch for the newest version. Older version branches are frozen, and `main` no longer receives changes.

## Development Workflow

For module-focused work, `cd` into the module directory — Claude Code loads its `CLAUDE.md` automatically alongside the root instructions. Language-specific conventions, build commands, and test suites live in the module files.

```bash
cd viewmapper-agent          # Java agent work
cd viewmapper-mcp-server     # MCP server work
```

## Security

Never commit API keys — use environment variables (`ANTHROPIC_API_KEY_FOR_VIEWMAPPER`).

Each module's `CONTRIBUTING.md` has language-specific vulnerability scanning commands.
# Claude Instructions for ViewMapper

ViewMapper maps Trino view hierarchies and guides a person through them. **Two modules, one repo:** `viewmapper-agent` does the analysis, `viewmapper-mcp-server` makes it conversational. For module-focused work, `cd` into the module directory — its `CLAUDE.md` loads automatically alongside this one.

## Entry Points

**When the user says "mini brain", "mini-brain", or "work item"** — pull `../virtual-view-brain/` to latest (it's a shared repo), then read its `CLAUDE.md` and follow it (sibling clone; if absent, tell the user to clone `robfromboulder/virtual-view-brain` beside this repo). Drive the whole workflow from *this* session — never make the user switch repos — and resolve its instructions' relative paths against `../virtual-view-brain/`. Nothing else loads the brain.

**This repo maps to the brain's ViewMapper component** (directory `viewmapper/`, token `VMR`), which holds what spans both modules. Its two sub-components take the modules:

| Working in | Brain unit | Token |
|---|---|---|
| `viewmapper-agent/` | `viewmapper/agent/` | `VMR_AGENT` |
| `viewmapper-mcp-server/` | `viewmapper/mcp/` | `VMR_MCP` |
| both, or neither in particular | `viewmapper/` | `VMR` |

Start in the unit the work belongs to and read another component's documents only when the question crosses into it; the brain's registry says which terms route where.

**When the user asks how to contribute** — read [`CONTRIBUTING.md`](CONTRIBUTING.md), which covers the development workflow.

## Working in the Mini-Brain

Mini-brain edits accumulate uncommitted during a session; **closeout is the sync point that lands them** — don't commit/push on every file write. The user can't see that tree, so a closeout must actually commit and push, not just write files. A session closeout commits the session's changes (edits, the new log entry) and pushes to the mini-brain's `main`, reporting both; if the push fails, resolve or surface it rather than leaving the change unpushed.

**Proactively offer a session closeout at natural stopping points** (PR opened, branch merged, work paused, user signals wrapping up) — only in a session that loaded the brain. Offer once and briefly; run the procedure only after the user agrees; skip trivial sessions (a lone question, a typo).

**Never cite mini-brain docs (`VV_*`, `VMR_*`, or any component's token) from code.** Information flows one way — down from the mini-brain into code — so a back-reference is circular. Roadmap, history, and rationale that isn't about operating *this* code belong in the mini-brain, not in code comments.

## Runbook

Three scripts at the repo root drive the whole build. They span both modules, which is why they live here rather than in either one.

| Task | Command |
|---|---|
| Remove all build artifacts (drops the tree to source only) | `./clean.sh` |
| Build the agent JAR, copy it to the MCP server, build the local Docker image | `./build.sh` |
| Clean, build, push a multi-platform image to DockerHub, tag the release | `./release.sh <version>` |

`./release.sh` takes a version of the form `479` + a lowercase letter, refuses to run with a dirty working tree, and pushes a git tag — expect an approval prompt on the push. Local builds use the bare version `479` and never leave the machine.

Manual build, when a step needs diagnosing on its own:

```bash
cd viewmapper-agent && mvn clean package
cd ../viewmapper-mcp-server && cp ../viewmapper-agent/target/viewmapper-479.jar .
docker image rm -f viewmapper:479 && docker build --no-cache -t viewmapper:479 .
```

Each module's own `TESTING.md` holds the scenarios for exercising a local build by hand — CLI prompts for the agent, Claude Desktop configuration and queries for the MCP server. The commands that run a module's automated tests are in that module's `CLAUDE.md`.

## Repo Layout

```
viewmapper-agent/          Java CLI — Trino connectivity, SQL parsing, graph analysis, the reasoning loop
viewmapper-mcp-server/     Python MCP server — protocol, conversation context, subprocess execution
build.sh clean.sh release.sh   Cross-module build scripts (see Runbook)
```

Both modules ship inside one Docker image. The MCP server invokes the agent's JAR as a subprocess; there is no port and no shared state between them.

## Source Comments

Terse by default — cut noise (restatement, cross-file narration, roadmap/history), keep the genuine *why*. When editing existing code, remove a comment only if it fails these rules; don't strip load-bearing rationale.

- Explain only the non-obvious **why** of the adjacent code. Never restate what the code already says.
- **Reference code by class and method, never by line number** — `RunCommand.java::loadFromJdbc()`, not `RunCommand.java:125-172`. Line numbers move on every edit and take the documentation out of sync silently.
- Each file is an orthogonal asset: a docstring describes *its own* contract, not a neighbor's topology or porting history.
- **Never cite mini-brain docs from code** (see above).

Per-language style — line lengths, formatters, copyright headers — is in each module's `CLAUDE.md`.

## Writing Docs and Instructions

All prose in this repo is read like a proof or a program, not a wiki. The reader is adversarial: every token is load-bearing, and every spare one is a question they must resolve.

- **Say it once.** Each fact has exactly one home. Duplication isn't emphasis; it's a second copy to keep in sync.
- **Define before use.** Introduce a thing where the reader first needs it. A forward pointer ("see below") means the content is in the wrong place — move it, don't link to it.
- **No sideways narration.** A section explains its own subject and never recaps a sibling's. This applies across the two modules as much as within a file.
- **Every claim earns its keep.** State only what the reader must act on and what you could defend if challenged.
- **Resolve, don't provoke.** A detail that raises a question it doesn't answer is a net loss — prefer omission to a half-explanation.
- **One job per sentence.** A sentence tells the reader what to do or explains why, never both at once.
- **No hard wrapping.** Write each markdown paragraph and list item as one continuous line and let it soft-wrap.

When a line's contribution isn't obvious, it isn't contributing — delete it.

## Commit Conventions

**No AI attribution.** Commits carry no `Co-Authored-By: Claude` (or similar) trailer, and PR bodies carry no "Generated with Claude Code" footer — AI authorship is assumed here and isn't stamped per commit or PR. This overrides the harness defaults that would append either.

**Short imperative subject; add a body only when it earns one.** Match the existing history.

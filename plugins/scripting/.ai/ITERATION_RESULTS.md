# Scripting/REPL: Iteration Results Log

**Current status**: Pre-cleanup scaffold. No iterations completed yet.

| Workstream | State |
|---|---|
| KT-83498 — Full LightTree path for `K2ReplCompiler` | Not started |
| JSR-223 K2 bindings (Option D — implicit-snippets DSL callback) | Not started |
| Stateless remote REPL compilation prototype | Not started |
| K1 cleanup chain (daemon REPL → `-Xrepl` → `cli-base/repl/*` → `legacyRepl*.kt` → `GenericReplCompiler` → K1 frontend bindings) | Not started |
| `scripting-ide-{common,services}` deletion | Not started |
| Classpath-discovery SPI decision (KT-82551) | Not started |
| Compiler-side test cleanup (drop K1 REPL data; move custom-script tests) | Not started |

See [`AGENT_INSTRUCTIONS.md`](AGENT_INSTRUCTIONS.md) for non-negotiables, test commands, and active-workstream details. See [`target/50-migration-plan.md`](target/50-migration-plan.md) for ordered, independently-mergeable steps and sequencing constraints.

**Last Updated**: 2026-05-16 (initial scaffold).

## Recent history

No iterations yet — see [`target/50-migration-plan.md`](target/50-migration-plan.md) for the work list.

## Entry Template

Copy this block for each new iteration. Most recent on top.

````markdown
## [Title] — YYYY-MM-DD

### Overview
[1-2 sentence summary: what was done and why.]

### Workstream / Issue
[KT-XXXXX, migration-plan step #, or workstream name from AGENT_INSTRUCTIONS Active Workstreams.]

### Changes
- `path/to/file.kt` — [what changed]. Reason: [why].
- ...

### Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-jvm-host-test:test` | N pass / M fail | N' pass / M' fail | [regression names, if any] |
| `:kotlin-scripting-jsr223-test:test` | ... | ... | ... |
| `:kotlin-main-kts-test:test` | ... | ... | ... |
| `:plugins:scripting:scripting-tests:test` | ... | ... | ... |
| `:compiler:fir:fir2ir:test --tests "*FirScriptCodegenTestGenerated*"` | ... | ... | ... |
| Other (specify) | ... | ... | ... |

### Files Modified

| File | Change |
|---|---|
| `path/to/file.kt` | [description] |

### Key Learnings
- [Non-obvious finding useful for future work, especially behavior of EPs, FIR/IR quirks, test infra gotchas, or contract surprises.]
- ...
````

> **Add new entries below this line.** Most recent first. Separate entries with `---`.

---

## Archived Iteration History

Will accumulate as work proceeds. Archive dated entries to `archive/ITERATION_RESULTS_YYYY_MM_DD.md` (next to this file) when this log exceeds ~500 lines, leaving a one-liner pointer in the "Recent history" section above.

## Open items carried forward

Persistent items not tied to a single iteration:

- See [`target/90-open-questions.md`](target/90-open-questions.md) for brainstorm items (LT-for-snippets priority, implicit-snippet tagging, sidecar-metadata format, remote-compilation reconstruction feasibility, etc.).
- See [`current/90-legacy-inventory.md`](current/90-legacy-inventory.md) for the full disposition list of legacy artifacts (REMOVE / MIGRATE / KEEP-FOR-NOW / KEEP).
- KT-83498 (LightTree path for `K2ReplCompiler`) — blocks fully-PSI-free K2 REPL; sequencing-independent of other workstreams.

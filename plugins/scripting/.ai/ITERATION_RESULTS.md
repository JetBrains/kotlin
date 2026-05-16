# Scripting/REPL: Iteration Results Log

> **When to consult**: at iteration start (review recent work) and at iteration end (append entry). Template is in [`ITERATION_TEMPLATE.md`](ITERATION_TEMPLATE.md). Per-entry detail lives in [`iterations/`](iterations/).
> **Cache lifetime**: stable prefix (status table + workstream table). The index below is append-only — new entries add lines, no existing line is rewritten.
> **Last verified**: 2026-05-16

**Current status**: Pre-cleanup scaffold. No iterations completed yet.

## Workstream state

| Workstream | State |
|---|---|
| KT-83498 — Full LightTree path for `K2ReplCompiler` (migration step 2) | Not started |
| JSR-223 K2 bindings (Option D — implicit-snippets DSL callback, migration step 1) | Not started |
| Stateless remote REPL compilation prototype (migration step 3) | Not started |
| K1 cleanup chain (steps 4 → 5 → 6 → 7 → 8 → 11) | Not started |
| `scripting-ide-{services,common}` deletion (steps 9, 10) | Not started |
| Classpath-discovery SPI decision (KT-82551, step 13) | Not started |
| Compiler-side test cleanup (step 12) | Not started |

See [`AGENT_INSTRUCTIONS.md`](AGENT_INSTRUCTIONS.md) for non-negotiables, dispatch matrix, and post-iteration checklist. See [`target/50-migration-plan.md`](target/50-migration-plan.md) for ordered, independently-mergeable steps.

## Iteration index (most recent on top)

Append one line per iteration: `- YYYY-MM-DD — [Title](iterations/YYYY-MM-DD_slug.md) — workstream / KT-XXXXX — one-line summary`.

(No iterations yet.)

## Archive cadence

Archive this log when **any** of these triggers:
- 20+ iteration entries since last archive
- 500+ lines in this file
- 30+ days since last archive

Procedure:
1. Move dated iteration files older than the cutoff from `iterations/` → `archive/iterations_YYYY_MM_DD/`.
2. Strike their lines in this index; leave a one-line pointer `- (Archive YYYY-MM-DD) → archive/iterations_YYYY_MM_DD/`.
3. The latest 5 iteration index lines always stay inline for context continuity.

## tmp/ retention

Scratch files under `.ai/tmp/` are not git-tracked beyond the current iteration. Files older than 7 days are deletable without review. Anything worth keeping moves into a numbered doc under `current/` or `target/`.

## Open items carried forward

- See [`target/90-open-questions.md`](target/90-open-questions.md) for Q* with triage fields (Status / Owner / YT / Target doc / Last touched).
- See [`current/90-legacy-inventory.md`](current/90-legacy-inventory.md) for the full disposition list of legacy artifacts (REMOVE / MIGRATE / KEEP-FOR-NOW / KEEP).
- KT-83498 (LightTree path for `K2ReplCompiler`) — blocks fully-PSI-free K2 REPL; sequencing-independent of other workstreams.

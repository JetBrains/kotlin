# Scripting/REPL: Iteration Results Log

> **When to consult**: at iteration start (review recent work) and at iteration end (append entry). Template is in [`ITERATION_TEMPLATE.md`](ITERATION_TEMPLATE.md). Per-entry detail lives in [`iterations/`](iterations/).
> **Cache lifetime**: stable prefix (status table + workstream table). The index below is append-only — new entries add lines, no existing line is rewritten.
> **Last verified**: 2026-05-16

**Current status**: Step 1 (JSR-223 K2 bindings) partially landed 2026-05-17, follow-ups 2026-05-18 — 12/21 `KotlinJsr223ScriptEngineIT` passing. testEvalWithError fixed; lastScriptContext threading + per-eval bindings field + eval() helpers in place. 4 step-1 follow-ups blocked by pre-existing K2 REPL `ReplState.put/kotlin.let [fake_override]` codegen bug.

## Workstream state

| Workstream | State |
|---|---|
| KT-83498 — Full LightTree path for `K2ReplCompiler` (migration step 2) | Not started |
| JSR-223 K2 bindings (Option D — synthetic-snippets DSL callback, migration step 1) | In progress (partial — 2026-05-17; 5 step-1 follow-ups remain) |
| Stateless remote REPL compilation prototype (migration step 3) | Not started |
| K1 cleanup chain (steps 4 → 5 → 6 → 7 → 8 → 11) | Not started |
| `scripting-ide-{services,common}` deletion (steps 9, 10) | Not started |
| Classpath-discovery SPI decision (KT-82551, step 13) | Not started |
| Compiler-side test cleanup (step 12) | Not started |

See [`AGENT_INSTRUCTIONS.md`](AGENT_INSTRUCTIONS.md) for non-negotiables, dispatch matrix, and post-iteration checklist. See [`target/50-migration-plan.md`](target/50-migration-plan.md) for ordered, independently-mergeable steps.

## Iteration index (most recent on top)

Append one line per iteration: `- YYYY-MM-DD — [Title](iterations/YYYY-MM-DD_slug.md) — workstream / KT-XXXXX — one-line summary`.

- 2026-05-17 — [process-audit] [First baseline audit](iterations/audit_2026-05-17.md) — 4 broken cross-ref paths fixed; Q10a resolved; PROCESS_AUDIT.md Section 2.10 grep fixed; loadout matrix footnotes added for Opus/subagent rules.
- 2026-05-18 — [JSR-223 K2 bindings — step 1 follow-ups round 1](iterations/2026-05-18_jsr223-followup-1.md) — migration step 1 (JSR-223 K2 bindings) — testEvalWithError fixed; lastScriptContext threading + eval() helpers + per-eval bindings field; 12/21 passing, 4 step-1 follow-ups blocked by pre-existing K2 REPL codegen bug.
- 2026-05-17 — [JSR-223 K2 bindings — partial landing](iterations/2026-05-17_bindings-partial.md) — migration step 1 (JSR-223 K2 bindings) — chain-walk eval + classloader-dep extraction + `@InlineOnly` workaround; 11/21 passing, 5 step-1 follow-ups + 4 pre-existing K2 codegen bugs remain.

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

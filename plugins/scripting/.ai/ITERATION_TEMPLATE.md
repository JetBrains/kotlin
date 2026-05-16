# Iteration Entry Template

> **When to consult**: when starting a new iteration entry. Copy this template to `iterations/YYYY-MM-DD_slug.md`, fill it out, then append a one-line index entry to `ITERATION_RESULTS.md`.
> **Cache lifetime**: stable (template body does not change frequently)
> **Last verified**: 2026-05-16

Required sections — leave none empty.

````markdown
# [Title] — YYYY-MM-DD

## Overview

1-2 sentence summary: what was done and why.

## Workstream / Issue

KT-XXXXX, migration-plan step #, or workstream name from `AGENT_INSTRUCTIONS.md` Active Workstreams.

## Changes

- `path/to/file.kt` — [what changed]. Reason: [why].
- ...

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-jvm-host-test:test` | N pass / M fail | N' pass / M' fail | [regression names, if any] |
| `:kotlin-scripting-jsr223-test:test` | ... | ... | ... |
| `:kotlin-main-kts-test:test` | ... | ... | ... |
| `:plugins:scripting:scripting-tests:test` | ... | ... | ... |
| `:compiler:fir:fir2ir:test --tests "*FirScriptCodegenTestGenerated*"` | ... | ... | ... |
| Other (specify) | ... | ... | ... |

## Files Modified

| File | Change |
|---|---|
| `path/to/file.kt` | [description] |

## Key Learnings

- [Non-obvious finding useful for future work — EP behaviour, FIR/IR quirks, test infra gotchas, contract surprises.]
- ...

## Resources & Cost

Run `.claude/scripts/iter-metrics.sh` (no args — auto-picks the most recent session) and paste its output below. Fill the "Loadout-vs-actual" block manually after pasting.

If the script fails or sessions are split across runs, pass paths explicitly: `iter-metrics.sh ~/.claude/projects/<dir>/sessions/a.jsonl b.jsonl ...`

| Metric | Value |
|---|---|
| Sessions aggregated | ... |
| Time span | ... → ... |
| Cost (USD, model-aware) | $... |
| Cache hit rate | ...% |
| Input tokens (non-cached) | ... |
| Output tokens | ... |
| Cache-creation tokens | ... |
| Cache-read tokens | ... |
| Model mix | ... |
| Subagent calls (total) | ... |
| Gradle wall-time (sum across suites) | ... min |

### Subagent breakdown

  - ...

### Loadout-vs-actual

- Loadout matrix row used: _(e.g. "Migration-step execution, ~7k budget, Sonnet")_
- Actual model: _(from Model mix above)_
- Budget hit / over / under: _(compare actual cost vs the row's budget tier; +/- 30% = "hit")_
- Subagent dispatch followed: _(yes / no — were cavecrew rules respected?)_
- If "no" or "over": one-line cause + intervention suggestion. Carries forward into the next PROCESS_AUDIT.

## Post-iteration checklist

(See `AGENT_INSTRUCTIONS.md` "Post-iteration checklist" — confirm each.)

- [ ] Resources & Cost section populated (script run, Loadout-vs-actual filled)
- [ ] Migration-plan step strike-through (`### N. ~~Title~~ — landed YYYY-MM-DD`)
- [ ] Active Workstreams updated in `AGENT_INSTRUCTIONS.md` if workstream completed
- [ ] `current/90-legacy-inventory.md` disposition rows updated for any deleted artifact
- [ ] `current/40-embedding-cli.md` / `current/45-embedding-daemon-legacy.md` / `current/70-tests.md` updated if surface changed
- [ ] Any resolved Q* in `target/90-open-questions.md` flipped to `resolved` with link here
- [ ] One-line index entry appended to `ITERATION_RESULTS.md`
````

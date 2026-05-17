# Scripting/REPL agentic workflow — README

> **What this is**: the agent infrastructure under `plugins/scripting/.ai/` + `.claude/` that drives the K2-migration cleanup. Read this once. Reference the commands below daily.

---

## File map

```
plugins/scripting/.ai/
├── AGENT_INSTRUCTIONS.md   ← stable prefix (non-negotiables, dispatch matrix, post-iter checklist, loadout)
├── ITERATION_TEMPLATE.md    ← copy this for each iteration entry
├── ITERATION_RESULTS.md     ← workstream state + 1-line index (append-only)
├── PROCESS_AUDIT.md         ← self-audit playbook (run periodically)
├── README.md                ← this file
├── current/                 ← state-of-the-world docs (00..90, all with `When to consult` headers)
├── target/                  ← cleanup target docs + migration plan (00..90)
├── iterations/              ← per-iteration entries (YYYY-MM-DD_slug.md, audit_YYYY-MM-DD.md)
└── tmp/                     ← scratch; 7-day retention

.claude/
├── settings.json            ← statusLine + hooks (test-data guard, gradle-tee guard, loadout hints)
├── commands/                ← slash commands (auto-registered)
│   ├── scripting-step.md            /scripting-step <N>
│   ├── scripting-doc.md             /scripting-doc <topic>
│   ├── scripting-q.md               /scripting-q <id>
│   ├── scripting-iter-start.md      /scripting-iter-start
│   ├── scripting-iter-close.md      /scripting-iter-close <slug>
│   └── scripting-audit.md           /scripting-audit
├── skills/                  ← skills (manual-invoke)
│   ├── scripting-iteration/         entry-gate + suite-run + archive
│   ├── scripting-step-runner/       investigator → builder → reviewer pipeline
│   └── scripting-q-resolver/        Q* status flip with required fields
└── scripts/
    ├── status.sh                    status line: model, cost, cache, subs, step, Q-count
    └── iter-metrics.sh              session JSONL → Resources & Cost markdown block
```

---

## Quick decision guide

| I want to… | Use |
|---|---|
| Execute a planned migration step | `/scripting-iter-start` → work → `/scripting-iter-close` |
| Investigate a topic or file (read-only) | `/scripting-doc <topic>` or direct prompt with file path |
| Resolve an open design question (Q*) | `/scripting-q <id>` |
| Run one step interactively | `/scripting-step <N>` |
| Periodic health check | `/scripting-audit` |

---

## Iteration workflow

_(Use this when executing a full migration step)_

```
session start          ←  /scripting-iter-start         (export SCRIPTING_TMP, load prefix + last 3 iters)
                       ↓
pick task              ←  /scripting-step <N>           OR /scripting-doc <topic>  OR /scripting-q <id>
                       ↓
do work                ←  cavecrew-investigator → cavecrew-builder per file → cavecrew-reviewer on diff
                       ↓
run tests              ←  ./gradlew … 2>&1 | tee "$SCRIPTING_TMP/<suite>.txt"
                       ↓
session close          ←  /scripting-iter-close <slug>  (writes iter file, runs iter-metrics, walks checklist)
```

Periodic (every ~10 iterations / 4 weeks / on trigger):

```
/scripting-audit         ←  walks PROCESS_AUDIT.md sections 0–6, produces iterations/audit_<DATE>.md
```

---

## Commands reference

| Command | When | What it does |
|---|---|---|
| `/scripting-iter-start` | session start | exports `SCRIPTING_TMP`, loads `AGENT_INSTRUCTIONS.md`, `ITERATION_RESULTS.md`, last 3 iterations |
| `/scripting-step <N>` | starting a migration step | loads `target/50` step N body + its Touch files + matching loadout-matrix row |
| `/scripting-doc <topic>` | need topic context | loads relevant `current/*` + `target/*` by keyword (bindings/daemon/compiler/test/cli/...) |
| `/scripting-q <id>` | resolving an open question | loads only Q-block from `target/90-open-questions.md` (Q1..Q12, Q5a..Q10f) |
| `/scripting-iter-close <slug>` | iteration done | writes `iterations/<DATE>_<slug>.md` from template, runs `iter-metrics.sh`, walks post-iter checklist |
| `/scripting-audit` | periodic | runs `PROCESS_AUDIT.md` recipes, writes `iterations/audit_<DATE>.md` with metrics + decisions |

---

## Effective prompting

### Patterns that work

1. **Name the step / Q-id / topic explicitly.**
   > "Execute step 4 from target/50."
   > "Resolve Q10a."
   > "Audit the JSR-223 bindings design."

   Concrete identifiers route the `UserPromptSubmit` hook to print loadout hints + let agents skip irrelevant docs.

2. **State scope upfront.**
   > "Read-only — locate every caller of `KotlinRemoteReplService`."
   > "Edit only `K2ReplCompiler.kt:351-359`; do not touch other files."

   Bounds the agent. Saves tokens. Avoids scope creep that violates Non-Negotiable Rules.

3. **Pass migration-step text verbatim to subagents.**
   When delegating to `cavecrew-builder` / `cavecrew-investigator`, copy the step's Goal + Touch list straight from `target/50-migration-plan.md`. Don't paraphrase — subagents have no session memory and you lose precision.

4. **Reference docs, don't paste.**
   > "Per `target/00-principles.md` P4, the daemon REPL goes."

   Cheap reference, no token bloat.

5. **Demand the loadout matrix row.**
   > "What loadout row does this task match? List minimal core docs before starting."

   Forces the agent to declare its plan. Catches over-loading early.

### Patterns that DON'T work

- **Too vague** (e.g., "Help me with scripting") or **too broad** (e.g., "Fix all K1 stuff"). Always include task type + concrete identifier (step N, Q-id, file path) and a Done-when sentence.
- **Mixing two steps in one prompt.** Each step has separate sequencing. Pick one.

### Model + loadout

Model selection and per-task loadout matrix: `AGENT_INSTRUCTIONS.md` → Per-Task Agent Loadout.

---

## Non-negotiables (top 3)

1. **Never** run `-Pkotlin.test.update.test.data=true` (`PreToolUse` hook blocks it).
2. **Never** create a git commit without explicit user review.
3. **Never** skip the Resources & Cost section in iteration entries — it blinds the periodic audit.

Full list: `AGENT_INSTRUCTIONS.md` → Non-Negotiable Rules.  
Subagent dispatch rules: `AGENT_INSTRUCTIONS.md` → Agent Dispatch section.

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| Status line empty / wrong | Restart Claude Code session; verify `.claude/settings.json` `statusLine` block; run `./.claude/scripts/status.sh` standalone to debug |
| `iter-metrics.sh` says "no session dir" | Pass session path explicitly: `iter-metrics.sh ~/.claude/projects/.../sessions/*.jsonl` |
| Gradle command blocked | Hook insists on `tee "$SCRIPTING_TMP/<tag>.txt"`. Add it. Or unset `SCRIPTING_TMP` and re-run `/scripting-iter-start`. |
| Slash command not found | Restart session; verify `.claude/commands/<name>.md` exists and frontmatter has `description:` |
| Loadout hint not printed for relevant prompt | Adjust regex in `UserPromptSubmit` hook (in `settings.json`); patterns are case-handled but not exhaustive |
| Audit recipe glob error | Recipes use `find -maxdepth` to avoid zsh glob fails; check no manual `iterations/*.md` literals leaked |
| `ccusage` columns missing from status line | Install with `npm i -g ccusage` (optional — falls back to model-aware pricing in `iter-metrics.sh`) |

---

## First-time setup checklist

- [ ] `git status` clean OR all dirty files belong to current work
- [ ] `jq` installed (`brew install jq`)
- [ ] `.claude/settings.json` has `statusLine` + hooks (already wired this repo)
- [ ] `~/.claude/projects/<repo-encoded>/sessions/*.jsonl` accessible (for `iter-metrics.sh`)
- [ ] (optional) `ccusage` installed for richer status-line cost columns

Run `/scripting-iter-start` to verify everything wires up. You should see `📁 SCRIPTING_TMP=/tmp/scr_…` and the workstream state table.

---

## Where to start tomorrow

1. `/scripting-iter-start`
2. Pick a step from `target/50-migration-plan.md`. Steps 1–3 are independent — start with whichever you have the most context for (recommended: step 2 KT-83498 since the design is most settled).
3. `/scripting-step 2`
4. Work the step.
5. `/scripting-iter-close <slug>` when done.

Every 10 iterations or 4 weeks → `/scripting-audit`.

# Scripting/REPL — Junie overrides

> **Source of truth**: [`AGENT_INSTRUCTIONS.md`](AGENT_INSTRUCTIONS.md). This file lists the per-tool deltas that apply when the host agent is **Junie** rather than Claude Code. Apply these overrides without rewriting AGENT_INSTRUCTIONS.md.

## Tool family

Use Junie's native tools in place of `mcp__jetbrains__*`:

| Need | Junie tool |
|---|---|
| Text / regex search across files | `search_project` |
| Symbol lookup | `search_project` (matches symbol names too) |
| Read a file (whole or a window) | `open` / `open_entire_file` |
| File structure overview before editing | `get_file_structure` |
| Targeted edit | `search_replace` (single occurrence) or `multi_edit` (multiple edits in one file) |
| Create a new file | `create` |
| Rename a symbol everywhere | `rename_element` |
| Compile / verify | `build` (only if necessary; otherwise `run_test`) |
| Run tests | `run_test` (preferred over invoking `./gradlew` directly when running JUnit/Kotlin tests) |
| Shell | `bash` (each call spawns a fresh shell — env does not persist) |

`search_project` covers `search_in_files_by_text`, `search_in_files_by_regex`, and `search_symbol`
collectively. Prefer it to opening whole files (per AGENT_INSTRUCTIONS "Search before reading").

## Session tmp directory

Junie's `bash` does **not** persist environment variables across tool calls — `export
SCRIPTING_TMP=…` in one call is invisible in the next. Replace `$SCRIPTING_TMP` with a
deterministic in-tree path that each call recomputes:

```bash
TMP_DIR="plugins/scripting/.ai/tmp/junie/$(date +%Y-%m-%d)"
mkdir -p "$TMP_DIR"
./gradlew :kotlin-scripting-jvm-host-test:test -q 2>&1 | tee "$TMP_DIR/jvm_host.txt"
```

`tmp/` retention from `ITERATION_RESULTS.md` still applies (>7-day files are deletable).
Add `plugins/scripting/.ai/tmp/junie/` to `.gitignore` if it isn't covered already.

When Gradle suites would re-run anyway, prefer `run_test` with a fully-qualified test class —
it returns parsed pass/fail without manual `tee` plumbing.

## Hooks → self-enforced rules

None of `.claude/settings.json`'s `SessionStart` / `PreToolUse` / `UserPromptSubmit` hooks fire
under Junie. The rules survive only as text:

- **Non-Negotiable #8** — Never run `git add` / `git commit` / `git push`. No hook backstop. List
  changed files and write "Ready for commit review." Stop.
- **Non-Negotiable #9** — Never run `-Pkotlin.test.update.test.data=true`. No hook backstop.
- **Shell Discipline** — Always `tee` Gradle output. No hook backstop.
- **Loadout hint** — The `UserPromptSubmit` regex that prints `💡 loadout: …` does not fire.
  First action of any scripting task: declare which Per-Task Agent Loadout row this task matches,
  then load only that row's core docs.

## Subagent roles → Junie modes

`cavecrew-investigator` / `-builder` / `-reviewer` and the `cavecrew` skill are unavailable. Map to
Junie's interaction modes:

| Cave-crew role | Junie equivalent |
|---|---|
| `cavecrew-investigator` (read-only locator, file:line table) | `[ADVANCED_CHAT]` mode, or the read-only investigation start of `[CODE]` mode. Use `search_project` + `get_file_structure` + targeted `open`. |
| `cavecrew-builder` (surgical 1–2 file edit) | `[CODE]` mode with `search_replace` / `multi_edit`. For trivial 1–3-step changes use `[FAST_CODE]`. |
| `cavecrew-reviewer` (diff review before commit) | Final self-review pass: re-read each edited region, run `build` / `run_test`, then `submit`. Output the same `path:line: <emoji> <severity>: <problem>. <fix>.` line shape when summarising. |

The hard rule "tasks crossing `plugins/scripting/` and `compiler/fir/` or `libraries/scripting/`
MUST go through `cavecrew-investigator` first" becomes: **start such tasks in `[ADVANCED_CHAT]`
or with a read-only `search_project` + `get_file_structure` sweep, before opening `[CODE]` edits.**

## Per-Task Agent Loadout — Junie deltas

The matrix in AGENT_INSTRUCTIONS.md still selects the **core docs** for each task type. The other
columns map as follows under Junie:

- **Budget** — advisory only. Junie session pricing is fixed; no per-task model switch happens.
- **Model** — Claude-only advisory. Junie's model is session-fixed (see the IDE setting). Do not
  surface "consider `/model opus`" messages.
- **Subagent** — substitute the Junie mode per the table above. The "MUST run `cavecrew-investigator`
  first" rule becomes "MUST do a read-only investigation sweep first" (see above).

## Iteration close — resources & cost block

`/.claude/scripts/iter-metrics.sh` reads `~/.claude/projects/<repo-encoded>/sessions/*.jsonl` which
Junie does not produce. Under Junie, fill the "Resources & Cost" section as follows (either is
acceptable; pick one and be consistent across a workstream):

- **Minimal**: write `n/a — Junie session, no JSONL` in the metrics table.
- **Substitute metrics** (preferred when comparable to other Junie iterations): record
  - number of `bash` / `run_test` calls,
  - files touched (list),
  - test suites run + pass/fail counts,
  - one-line note on whether the Loadout-row's "Core docs" were sufficient.

The Loadout-vs-actual sub-block still applies — fill it manually using the row matched at
session start.

## Slash commands → plain prompts

Junie has no slash-command surface. Trigger the same procedure by referencing the body file:

| Claude slash | Junie prompt |
|---|---|
| `/scripting-iter-start` | "execute the `scripting-iter-start` procedure (`.claude/commands/scripting-iter-start.md`)" |
| `/scripting-step <N>` | "execute `scripting-step` for step `<N>`" |
| `/scripting-q <id>` | "execute `scripting-q` for `<id>`" |
| `/scripting-doc <topic>` | "execute `scripting-doc` for `<topic>`" |
| `/scripting-iter-close <slug>` | "execute `scripting-iter-close` for slug `<slug>`" |
| `/scripting-audit` | "execute the `scripting-audit` procedure" |

Junie reads the same procedural body and executes the listed steps using its native tools, with
the substitutions above (tmp path, no hooks, no subagent dispatch, no model switch).

## Quick checklist when starting a Junie scripting task

1. State which Per-Task Agent Loadout row matches; load only that row's core docs (plus this file).
2. Recompute `TMP_DIR="plugins/scripting/.ai/tmp/junie/$(date +%Y-%m-%d)"` in every `bash` call.
3. Read-only investigation sweep with `search_project` / `get_file_structure` before any edit.
4. Make edits with `search_replace` / `multi_edit` / `rename_element`.
5. Verify with `run_test` (preferred) or `build`; for plain Gradle runs `tee` to `$TMP_DIR`.
6. On close, fill Resources & Cost per the section above. Never `git add/commit/push`. Never
   `-Pkotlin.test.update.test.data=true`.

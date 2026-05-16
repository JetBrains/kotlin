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

## Investigation budget

Under Claude Code the "Loadout matrix" budget column targeted Sonnet-token spend, enforceable via
the `cavecrew-*` subagents (each subagent has its own context window, so the main thread doesn't
pay for full-file re-reads). Under Junie, neither lever exists: the model is session-fixed and
subagents are unavailable, so cost discipline has to come from raw tool-call caps.

The following caps apply per workstream switch (i.e. once you're inside a single Effective Issue
working on one migration step / one Q*). If you find yourself about to exceed a cap, **stop and
write a `file:line:role` table to `$TMP_DIR/loc-table.md`** before doing anything else — the table
replaces the missing `cavecrew-investigator` output.

| Tool / pattern | Soft cap | Note |
|---|---|---|
| `open_entire_file` calls | 2 | Use `get_file_structure` + windowed `open` instead for any file > ~300 lines. |
| `open` calls on the same file | 3 | After 3 windows, save the relevant fragment to `$TMP_DIR/<name>.md` and grep that. |
| `search_project` calls in a row without a follow-up action | 5 | If you've issued 5 searches and still haven't opened a single line range, your search terms are wrong — change strategy. |
| `bash` calls that re-execute `./gradlew :kotlin-scripting-jsr223-test:test` per iteration | 2 | One initial run + one verification run after the fix; if you need a third, the fix is wrong. |
| Full-suite re-runs across other modules | 1 per modified module | Per AGENT_INSTRUCTIONS "Search before reading" — never rerun for a different output slice. |

Loadout-vs-actual audit at iteration close should record the actual counts (`bash`/`run_test`
calls, files touched). Exceeding a cap is not a failure on its own, but two consecutive
overruns on the same row trigger a `PROCESS_AUDIT.md` row in the next audit cycle.

## Test triage workflow

`./gradlew -q` swallows per-test names; only `BUILD FAILED` is printed on stdout. Per-test
information lives in `libraries/scripting/jsr223-test/build/test-results/test/TEST-*.xml`.
Default recipe after every `:kotlin-scripting-jsr223-test:test` run:

```bash
TMP_DIR="plugins/scripting/.ai/tmp/junie/$(date +%Y-%m-%d)"
mkdir -p "$TMP_DIR"
./gradlew :kotlin-scripting-jsr223-test:test -q 2>&1 | tee "$TMP_DIR/jsr223.txt"
```

Then parse the XML for a per-test status table. A reusable Python one-liner is acceptable for
quick triage; if you write a longer helper, save it under `plugins/scripting/.ai/scripts/`
(not `tmp/`) so the next iteration can reuse it.

```bash
TMP_DIR="plugins/scripting/.ai/tmp/junie/$(date +%Y-%m-%d)"
python3 -c "
import xml.etree.ElementTree as ET, glob
for path in sorted(glob.glob('libraries/scripting/jsr223-test/build/test-results/test/TEST-*.xml')):
    for tc in ET.parse(path).getroot().iter('testcase'):
        f = next(iter(tc.findall('failure')), None)
        s = next(iter(tc.findall('skipped')), None)
        st = 'FAIL' if f is not None else 'SKIP' if s is not None else 'PASS'
        msg = (f.get('message') or '').splitlines()[0] if f is not None else (s.get('message') or '') if s is not None else ''
        print(f'{st:4} {tc.get(\"name\")}  {msg[:120]}')
" | tee "$TMP_DIR/jsr223_summary.txt"
```

Triage rules:

1. **Before chasing any new failure**, cross-check the test's row in
   [`current/70-tests.md`](current/70-tests.md#jsr-223-per-test-status-blocked-by-matrix) and
   [`current/80-known-gotchas.md`](current/80-known-gotchas.md). `BLOCKED-*` rows should be
   `@Disabled` already — if they're not, *that* is the bug to fix (the disable was
   removed/regressed), not the underlying test.
2. **Tests blocked by `Q13` (codegen)** should never be analysed inline during a step 1
   follow-up. They are step **1b** scope only.
3. The bash tool truncates long outputs. The full failure stack (with snippet line/col) goes
   into the `.output.txt` sibling that Gradle generates next to the `TEST-*.xml`. Read the
   `.output.txt` file directly rather than re-running with `--info`.

## run_test vs ./gradlew

Junie's `run_test` is the preferred path *when it works*, but the JSR-223 test runner relies on
Gradle-injected system properties (`testCompilationClasspath`, `testJsr223RuntimeClasspath`)
which `run_test` does not always honour. Symptom: `NullPointerException` on
`System.getProperty("testCompilationClasspath")!!` or empty/null classpath in test setup.

Decision tree:

- `:kotlin-scripting-jvm-host-test:test` → try `run_test` first; falls back to `./gradlew` on
  property-not-set.
- `:kotlin-scripting-jsr223-test:test` → use `./gradlew` directly; `run_test` is not reliable
  for this suite as of 2026-05-18.
- `:plugins:scripting:scripting-tests:test` → either; prefer `run_test`.

## Quick checklist when starting a Junie scripting task

1. State which Per-Task Agent Loadout row matches; load only that row's core docs (plus this file).
2. Recompute `TMP_DIR="plugins/scripting/.ai/tmp/junie/$(date +%Y-%m-%d)"` in every `bash` call.
3. Read-only investigation sweep with `search_project` / `get_file_structure` before any edit.
   Respect the "Investigation budget" caps above.
4. Make edits with `search_replace` / `multi_edit` / `rename_element`.
5. Verify with `run_test` (preferred where it works — see `run_test vs ./gradlew` above) or
   `build`; for plain Gradle runs `tee` to `$TMP_DIR` and parse with the "Test triage workflow"
   recipe.
6. On close, fill Resources & Cost per the section above. Never `git add/commit/push`. Never
   `-Pkotlin.test.update.test.data=true`.

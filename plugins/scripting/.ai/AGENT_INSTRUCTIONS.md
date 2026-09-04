# Scripting/REPL — Agent Instructions

**Current status**: K2 path is the active path for scripts (LightTree-based, parser-agnostic core), and K2 REPL is parser-agnostic too (**KT-83498** landed 2026-09-04 — LightTree for REPL snippets). The legacy K1 REPL is gone as of 2026-09-04: no daemon REPL service, no `-Xrepl`, no `GenericReplCompiler` / `KJvmReplCompilerBase` / terminal REPL, no `scripting-ide-services`. The daemon's REPL RMI methods survive and report an error. K1 frontend retirement is still in progress. Two open workstreams: **JSR-223 K2 bindings** (Option D recommended), **stateless remote REPL compilation** prototype.

**Scope**: `plugins/scripting/*`, `libraries/scripting/*`, `libraries/tools/kotlin-main-kts*`, the scripting-related parts of `compiler/cli/`, `compiler/daemon/`, `compiler/fir/`, `compiler/ir/`, `compiler/build-tools/`, and `libraries/tools/kotlin-gradle-plugin/.../scripting/`.

**Doc set**: lives under `plugins/scripting/.ai/`. `current/` maps today's state; `target/` describes the cleanup target. Full key-file list lives in [`current/10-compiler-representation.md`](current/10-compiler-representation.md) — don't duplicate it here.

---

## Glossary

| Term | Meaning |
|---|---|
| **Script** | Whole `.kts` file compiled to a class. FIR repr: `FirScript` (statements + params + receivers). |
| **REPL snippet** | One input chunk in an interactive session. FIR repr: `FirReplSnippet` — embeds `FirRegularClass` + `$$eval` function. **Different shape from script.** |
| **K1 / FE 1.0** | Legacy frontend (descriptor-based, PSI-tied). |
| **K2 / FIR** | Current frontend. |
| **Configurator extension** | Plugin seam to mutate FIR during build / resolve / FIR-to-IR for scripts and snippets. 6 EPs total (3 for script, 3 for snippet) — full enumeration in [`current/10-compiler-representation.md`](current/10-compiler-representation.md). |
| **Script definition** | `@KotlinScript`-annotated class declaring script shape (base class, default imports, refinement handlers, file extension). |
| **Refinement** | User-supplied callbacks that mutate `ScriptCompilationConfiguration` before parsing / on annotations / before compilation / before evaluation. Public customization surface. |
| **Implicit snippet** | (Planned) Synthetic snippet emitted by a refinement-DSL callback to run before the user's snippet — e.g. JSR-223 binding cell. See [`target/40-jsr223-target.md`](target/40-jsr223-target.md) Option D. |

---

## ⚠ Non-Negotiable Rules (stop immediately if violated)

1. **No new K1 paths.** Modules tagged REMOVE in [`current/90-legacy-inventory.md`](current/90-legacy-inventory.md) are slated for deletion — don't extend them, don't add new callers.
2. **No new public extension points without ratification.** Compiler-internal EPs are documented in [`current/10-compiler-representation.md`](current/10-compiler-representation.md). User customizations go through the `ScriptCompilationConfiguration` refinement DSL — see [`current/20-customization.md`](current/20-customization.md).
3. **No reviving daemon REPL / `-Xrepl` / `cli-base/repl/*`.** These are removed (2026-09-04). What remains is `ReplApi.kt` + `ReplState.kt`, kept only to type the daemon RMI methods, which return an error; don't build on them (see [`target/30-embedding-target.md`](target/30-embedding-target.md)).
4. **Don't add a PSI-only K2 path.** `ScriptJvmK2CompilerImpl`'s `convertToFir` lambda is the seam. LT is the only wired converter today. If you need a non-LT path for a real reason, discuss before coding.
5. **Don't re-introduce PSI into `K2ReplCompiler`.** **KT-83498** landed (2026-09-04): every `SourceCode` goes through the `convertToFir` lambda (LT default) and the snippet id travels via `repl.currentLineId` in the refined configuration. Need PSI? Inject a converter, don't add branches. Line anchors in [`current/10-compiler-representation.md`](current/10-compiler-representation.md); design record in [`target/50-migration-plan.md`](target/50-migration-plan.md) step 2.
6. **No `intellij-community` plugin dependencies in `plugins/scripting/*`.** `scripting-ide-common` (copied from IntelliJ monorepo) is REMOVE.
7. **`libraries/scripting/intellij` is public surface.** It's used by IntelliJ plugin authors wiring custom-scripts support. Don't break compatibility; don't move/rename.
8. **NEVER initiate any git commit workflow.** No `git add`, `git commit`, `git push`, or staging of any kind. When a step is complete, list the changed files and write "Ready for commit review." Stop there. The user commits. Under Claude Code the PreToolUse hook blocks `git add/commit/push`; under Junie there is no hook backstop — this rule is self-enforced (see [`JUNIE_NOTES.md`](JUNIE_NOTES.md)).
9. **Test data**: NEVER run `-Pkotlin.test.update.test.data=true` unless the user explicitly asks. Test data is shared across runners; bulk updates corrupt the dataset. After adding new test data fixtures: `./gradlew generateTests`. (Canonical statement — Repo Conventions section refers here.) **Never hand-edit shared test data to make a scripting test pass either** — a diverging result usually means the scripting-side implementation is wrong; fix it, or record the divergence with investigation evidence in the iteration entry.

10. **Only the main agent runs Gradle.** Subagents MUST NOT invoke `./gradlew` — parallel builds corrupt each other's test results and saturate CPU and disk. A subagent that needs a test run reports what to run; the main agent runs it.

11. **Comments are opt-in, not opt-out.** When an edit would add a comment or KDoc, first write the edit without it. Add the comment back only if you can name which of the three justifications from the Source Comment Conventions section (why / API contract / real trap) it satisfies, in one clause, in the edit's rationale. No named justification = no comment.

12. **Before reporting any change that touched source files, reread the diff's comment lines alone** against the Source Comment Conventions below, and delete everything that fails rule 11:
    ```bash
    git diff -U0 | grep '^+' | grep -E '//|\*'
    ```
    This is a step of the change, not a review afterthought: unlike code, comments have no red/green signal, so nothing else catches them. Rule 11 is the cheap one (not writing something); rule 12 asks you to delete text you have already justified, so don't rely on it alone.

---

## Shell Discipline

### Session temp directory

At session start:
```bash
export SCRIPTING_TMP="/tmp/scr_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$SCRIPTING_TMP"
```
All Gradle output and saved diffs go under `$SCRIPTING_TMP`. Never write directly to `/tmp/`.

**Junie host**: each `bash` call spawns a fresh shell, so `export` does not persist across calls. Instead, recompute a deterministic in-tree path in every call: `TMP_DIR="plugins/scripting/.ai/tmp/junie/$(date +%Y-%m-%d)" && mkdir -p "$TMP_DIR"`. See [`JUNIE_NOTES.md`](JUNIE_NOTES.md) §Session tmp directory.

`tmp/` under `.ai/` is for scratch only; files older than 7 days are deletable without review. Not git-tracked beyond the current iteration. See [`ITERATION_RESULTS.md`](ITERATION_RESULTS.md) "tmp/ retention" for details.

### One command per Bash call

The permission system matches the **first token only**. With `cmd1 && cmd2`, only `cmd1` is checked. Run sequential commands as separate tool calls. `|` (piping) is fine; `&&`, `||`, `;` are not.

### Gradle runs: save output, run once

Every Gradle invocation MUST `tee` to `$SCRIPTING_TMP`. If you forgot `tee`: do NOT rerun Gradle — grep whatever output you have, or ask the user. After a run, grep the saved file — never rerun Gradle just to see a different slice. Include `--stacktrace` for suite and single-test runs. Don't use `--info`/`--debug` unless needed, and don't pass `--rerun-tasks` / `--no-build-cache` on routine runs; use `--rerun` (test-task-only) to re-execute tests whose inputs did not change.

---

## Ground Rules

- **Use the host agent's project tools.** Under Claude Code: JetBrains IDE MCP per repo `CLAUDE.md` (`search_in_files_by_text`, `replace_text_in_file`, `get_file_text_by_path`, `get_file_problems`, `rename_refactoring`). Under Junie: native tools (`search_project`, `open` / `get_file_structure`, `search_replace` / `multi_edit`, `rename_element`, `build` / `run_test`) — see [`JUNIE_NOTES.md`](JUNIE_NOTES.md) §Tool family. Fall back to standard CLI only when neither is available.
- **Search before reading** — prefer `search_in_files_by_text`/`search_in_files_by_regex` (Claude) or `search_project` (Junie) over loading whole files.
- **`get_file_problems` after edits** (Claude) or `build` / `run_test` (Junie). Fix warnings related to your changes.
- **Check `git diff` for unintended changes** after every test run.
- **Read the relevant `current/*` doc first** when touching a subsystem. Use the **Per-Task Agent Loadout** matrix below to pick the minimal set — don't load everything.

---

## Test Commands

```bash
# Host + REPL (covers ResolveDependencies, ImplicitsFromScriptResult, ConfigurationDsl, Caching)
./gradlew :kotlin-scripting-jvm-host-test:test -q 2>&1 | tee "$SCRIPTING_TMP/jvm_host.txt"

# JSR-223 integration
./gradlew :kotlin-scripting-jsr223-test:test -q 2>&1 | tee "$SCRIPTING_TMP/jsr223.txt"

# main-kts canonical script def
./gradlew :kotlin-main-kts-test:test -q 2>&1 | tee "$SCRIPTING_TMP/main_kts.txt"

# Scripting plugin integration
./gradlew :plugins:scripting:scripting-tests:test -q 2>&1 | tee "$SCRIPTING_TMP/scripting_tests.txt"

# K2 script codegen suite (generated, ~40+ tests)
./gradlew :compiler:fir:fir2ir:test --tests "*FirScriptCodegenTestGenerated*" -q 2>&1 | tee "$SCRIPTING_TMP/fir_script_codegen.txt"

# Custom-script codegen (K2 LT and PSI variants)
./gradlew :compiler:tests-integration:test --tests "*FirLightTreeCustomScriptCodegenTest*" --tests "*FirPsiCustomScriptCodegenTest*" -q 2>&1 | tee "$SCRIPTING_TMP/custom_script.txt"

# Single test
./gradlew :kotlin-scripting-jvm-host-test:test --tests "*ReplTest.testSomeName*" -q --rerun 2>&1 | tee "$SCRIPTING_TMP/single.txt"
```

### Extracting failures

```bash
grep "FAILED" "$SCRIPTING_TMP/jvm_host.txt" | sort -u
grep -A5 "FAILED" "$SCRIPTING_TMP/jvm_host.txt" | grep -E "Exception|Error:|UNRESOLVED|Expected" | head -60
```

### Test inventory

Full per-module test placement, plus compiler-side test inventory (with disposition tags REMOVE / KEEP / MOVE / AUDIT): see [`current/70-tests.md`](current/70-tests.md). When editing compiler-side scripting code, run the relevant K2 codegen suite **and** the plugin-side tests.

---

## Shared Files (modify with caution)

| File / path | Why caution | Procedure |
|---|---|---|
| `compiler/fir/tree/gen/.../FirScript.kt`, `FirReplSnippet.kt`, `compiler/ir/ir.tree/gen/.../IrScript.kt`, `IrReplSnippet.kt` | Generated; schema lives in tree generators | Regenerate — don't hand-edit |
| `compiler/fir/raw-fir/raw-fir.common/.../FirScriptConfiguratorExtension.kt`, `FirReplSnippetConfiguratorExtension.kt` | Public EP contracts | Compare with upstream; run scripts + REPL suites before/after |
| `compiler/fir/providers/.../FirReplSnippetResolveExtension.kt` | Public EP contract; storage-defined-by-impl | Same |
| `compiler/fir/fir2ir/.../Fir2IrScriptConfiguratorExtension.kt`, `Fir2IrReplSnippetConfiguratorExtension.kt` | Public EP contracts | Same |
| `compiler/cli/cli-jvm/src/.../pipeline/jvm/JvmScriptPipelinePhase.kt` | CLI entry; user-visible behavior | Run `LauncherScriptTest` + integration smoke fixtures |
| `compiler/arguments/.../CommonCompilerArguments.kt` | CLI argument table; deprecation discipline applies | Coordinate any flag change separately |
| `libraries/scripting/common/api/scriptCompilation.kt`, `scriptEvaluation.kt` | Public API; binary-compat matters | Treat as `@SinceKotlin`-stable; no breaking changes without deprecation cycle |
| `libraries/scripting/intellij/*` | Public surface for IntelliJ plugin authors | No breaking changes |

---

## Critical Patterns (do not break)

- **Parser-agnostic seam.** `ScriptJvmK2CompilerImpl` takes a `convertToFir` lambda; only `convertToFirViaLightTree` is wired. New K2 entry points should mirror this shape — pass the converter in, don't bind it inside.
- **REPL history is storage-defined by impl.** `FirReplHistoryProvider` has 4 abstract methods. Current impl is in-memory; a class-file-backed impl is the seam for the stateless remote-compilation work (migration step 3). Don't bind in-memory assumptions into callers.
- **`$$eval` / `$$result` constants** live in `ReplSnippetsToClassesLowering` (`REPL_SNIPPET_EVAL_FUN_NAME = "$$eval"`, `REPL_SNIPPET_RESULT_PROP_NAME = "$$result"`). Don't shadow or rename.
- **Configurator EPs are PSI-agnostic by contract.** They take abstract `KtSourceFile` / `KtSourceElement`. Don't add `as? KtScript` casts (the last one was removed by KT-83498; snippet-specific data reaches the EP impls through the refined `ScriptCompilationConfiguration`, e.g. `repl.currentLineId`).
- **`KtScript.isReplSnippet`** is the snippet marker for PSI sources. K2 + PSI path relies on this.
- **Scripts vs snippets in FIR.** Different shape, different EPs. Don't unify them at the FIR level.
- **K2 REPL inliner gap on `@InlineOnly` / `[fake_override]`.** Until migration step **1b** ([`target/50-migration-plan.md`](target/50-migration-plan.md#1b-fix-k2-repl-ir_external_declaration_stub)) lands, code emitted into synthetic snippets must avoid `?.let`, `?.also`, `?.apply`, `?.takeIf`, `bindings[k] = v` (the `MutableMap.set` `@InlineOnly`), and `joinToString$default` — they hit `IR_EXTERNAL_DECLARATION_STUB`. Use `bindings.put(k, v)` and explicit null-checks instead. See [`current/80-known-gotchas.md`](current/80-known-gotchas.md) G1 / G2.
- **Lambdas in JSR-223 bindings under `-Xlambdas=indy` have non-parseable `qualifiedName`.** Generators that emit Kotlin type references for binding accessors must filter via `isParseableKotlinQualifiedName(qn)`; unfiltered values produce parse-errors on the generated `var foo: <className>` declaration. See [`current/80-known-gotchas.md`](current/80-known-gotchas.md) G9.

---

## Active Workstreams

Priority TBD — the list below is unordered.

- ~~**KT-83498** — Full LightTree path in `K2ReplCompiler`~~ — **landed 2026-09-04**. G15 resolved 2026-09-04 (imports as preceding snippets). See [`target/50-migration-plan.md`](target/50-migration-plan.md) step 2.
- **JSR-223 K2 bindings** — Option D — synthetic-snippets refinement-DSL callback (`prependSyntheticSnippets`). Partial landing 2026-05-17. See [`target/40-jsr223-target.md`](target/40-jsr223-target.md) and [`target/50-migration-plan.md`](target/50-migration-plan.md) step 1.
- **Stateless remote REPL compilation** prototype — See [`target/40-jsr223-target.md`](target/40-jsr223-target.md) and [`target/50-migration-plan.md`](target/50-migration-plan.md) step 3.
- ~~**K1 cleanup chain** — Daemon REPL → `-Xrepl` → `legacyRepl*.kt` → `GenericReplCompiler`~~ — **landed 2026-09-04** (steps 4, 5, 7, 8). What's left of the chain: the last of `cli-base/repl/*` (step 6, blocked while the daemon RMI methods stay) and the K1 frontend bindings (step 11, gated on whole-compiler K1 retirement).
- **`scripting-ide-common` deletion** — [`target/50-migration-plan.md`](target/50-migration-plan.md) step 10. (`scripting-ide-services` — step 9 — landed 2026-09-04.)
- **Classpath-discovery SPI decision** (KT-82551) — un-deprecate + document or design successor. [`target/50-migration-plan.md`](target/50-migration-plan.md) step 13.
- **Compiler-side test cleanup** — [`target/50-migration-plan.md`](target/50-migration-plan.md) step 12.

## Post-iteration checklist

After landing a migration-plan step:

1. **Resources & Cost metrics**: under Claude Code, run `.claude/scripts/iter-metrics.sh` and paste output into the entry's "Resources & Cost" section. Under Junie, the script has no JSONL to read — record `n/a — Junie session, no JSONL` or substitute metrics per [`JUNIE_NOTES.md`](JUNIE_NOTES.md) §Iteration close. Fill the Loadout-vs-actual sub-block manually in both cases — this is the audit signal.
2. Create iteration file at `iterations/YYYY-MM-DD_slug.md` from [`ITERATION_TEMPLATE.md`](ITERATION_TEMPLATE.md). Append one-line index entry to [`ITERATION_RESULTS.md`](ITERATION_RESULTS.md).
3. Strike the step in `target/50-migration-plan.md`: `### N. ~~Title~~ — landed YYYY-MM-DD`.
4. Update **Active Workstreams** list in this file if a workstream completed.
5. Update `current/90-legacy-inventory.md` disposition rows for any deleted artifact.
6. Update `current/40-embedding-cli.md` / `current/45-embedding-daemon-legacy.md` / `current/70-tests.md` if surface changed.
7. If a Q* in `target/90-open-questions.md` resolved → flip status to `resolved` and link the landing iteration in the Target-doc field.
8. Bump **Last verified** date in any doc whose body text materially changed.

**Why resource logging matters**: The periodic [`PROCESS_AUDIT.md`](PROCESS_AUDIT.md) pulls cost / cache hit / model mix / subagent breakdown from these per-iteration entries. Skipping the Resources & Cost section blinds the audit. If the script fails (no jq, no session JSONL accessible), record "n/a — reason" rather than leaving the section blank.

---

## Agent Dispatch

Available subagent types and when to use them. (Hard rule: tasks crossing `plugins/scripting/` and `compiler/fir/` or `libraries/scripting/` MUST go through `cavecrew-investigator` first.)

- **`caveman:cavecrew-investigator`** — read-only code locator. Use before any edit touching >1 module or unknown call-sites. Returns file:line table, caveman-compressed (~60% fewer tokens than vanilla Explore).
- **`caveman:cavecrew-builder`** — surgical 1–2 file edit where the change is fully specified (e.g., step 4: delete `KotlinRemoteReplService.kt`). Hard refuses 3+ file scope. Pass the migration-step text verbatim.
- **`caveman:cavecrew-reviewer`** — diff/branch/file reviewer on every diff before commit prep. Output: `path:line: <emoji> <severity>: <problem>. <fix>.` Keep in main context for the commit message draft.
- **`Explore`** — broad codebase searches with >3 file lookups. Returns excerpts only; do not use for cross-file consistency or code-review.
- **`Plan`** — redesign questions or anything routed to a Q* in [`target/90-open-questions.md`](target/90-open-questions.md).
- **`general-purpose`** — last resort when no specialized agent fits. Log why in the iteration entry.

If `core docs > 8k tokens` for your task, summarise into scratch context (`$SCRIPTING_TMP/notes.md`) before invoking the subagent — pass the summary, not the raw docs.

Subagents never run Gradle (Non-Negotiable Rule 10) and are bound by the Source Comment Conventions like the main agent — pass the relevant rules into the task text, and run the Rule 12 comment pass over the subagent's diff yourself.

## Per-Task Agent Loadout

Use the minimal core-doc set for your task. Skip the rest unless explicitly needed. **Budget column = expected session cost order-of-magnitude (input tokens for context + reasonable interaction).** When closing the iteration, compare actual cost from `iter-metrics.sh` against this row's budget — record over/under in the Loadout-vs-actual block. Repeated overruns surface in `PROCESS_AUDIT.md` and trigger a matrix revision.

> **Model column is advisory for the user, not an agent action.** The agent cannot switch its own model. Default is Sonnet (project setting). For Opus-recommended tasks, inform the user: "This task is loadout Opus — consider `/model opus`." For Haiku tasks, inform: "This task is loadout Haiku — consider `/model haiku`." Resume work at current model if user doesn't switch.
>
> **Under Junie**, the Model and Subagent columns are Claude-only advisory and do not apply — Junie's model is session-fixed in the IDE setting, and `cavecrew-*` are unavailable. The Core docs and Optional columns still apply unchanged. See [`JUNIE_NOTES.md`](JUNIE_NOTES.md) §Per-Task Agent Loadout.

| Task type | Core docs (always load) | Optional (load on demand) | Budget | Model | Subagent |
|---|---|---|---|---|---|
| K2 compiler edit (FIR/IR/lowerings) | this file + `current/10-compiler-representation.md` + `target/10-compiler-target.md` | `current/00-overview.md`, `target/00-principles.md` | ~6k | Sonnet (Opus for cross-EP design) | `cavecrew-investigator` → `cavecrew-builder` |
| Legacy K1 audit / deletion | this file + `current/90-legacy-inventory.md` + `target/50-migration-plan.md` (one step) | `current/40-embedding-cli.md`, `current/45-embedding-daemon-legacy.md`, `current/30-api-layer.md` | ~5k | Haiku → Sonnet | `cavecrew-investigator` |
| Migration-step execution (one numbered step) | this file + `target/50-migration-plan.md` (one step + sequencing tail) | step's "Touch" files | ~7k | Sonnet¹ | `cavecrew-builder` per file² |
| JSR-223 / bindings design | this file + `target/40-jsr223-target.md` + `current/60-jsr223.md` | `target/90-open-questions.md` Q10, `target/20-api-target.md` | ~9k | Opus | `Plan` → `cavecrew-builder` |
| Stateless remote REPL design | this file + `target/40-jsr223-target.md` (remote section) + `current/30-api-layer.md` | `target/90-open-questions.md` Q5 sub-table | ~8k | Opus | `Plan` |
| Test triage | this file (Test Commands) + `current/70-tests.md` | `current/90-legacy-inventory.md`, `target/50-migration-plan.md` step 12 | ~4k | Haiku → Sonnet on cluster | `Explore` for failure-text search |
| Doc maintenance | this file + `ITERATION_RESULTS.md` | the one doc being edited | ~3k | Haiku | none |
| Cross-module change (>3 files) | task core + `current/00-overview.md` | as above | ~10k | Opus | `cavecrew-investigator` MUST run first |

> ¹ **Migration-step model:** Drop to Sonnet once the failure mode is localized. Opus only for the up-front design call (which for most steps is already decided). If the step surfaces K2 REPL bugs across >2 modules, treat as "Cross-module change" row instead.
> ² **Migration-step subagent:** If the step surfaces bugs crossing `plugins/scripting/` and any `libraries/scripting/` module during diagnosis, dispatch `cavecrew-investigator` first even if the step text names only one file. The investigator call localizes unknown call-sites before `cavecrew-builder` edits.

## Caching strategy — file load order

Load stable → mutable so the prefix cache survives across iterations:

1. `AGENT_INSTRUCTIONS.md` (this file — stable prefix; pin to cache).
2. `current/00-overview.md`, `current/10-compiler-representation.md`, `current/80-known-gotchas.md`, `target/00-principles.md` (stable).
3. Task-specific `current/*` + `target/{10,20,30}-*.md`.
4. Mutable tail: `target/40-jsr223-target.md` (if relevant), `target/50-migration-plan.md`, `target/90-open-questions.md`, `current/70-tests.md`, `current/90-legacy-inventory.md`.
5. Last: `ITERATION_RESULTS.md`.

When `target/40-jsr223-target.md` or `target/90-open-questions.md` is rewritten during prototyping, the prefix (1–3) stays cached.

---

## Source Comment Conventions

These rules apply to **every** source comment or KDoc you add or edit — in `plugins/scripting/*` and `libraries/scripting/*`, and with extra strictness in the scripting-related parts of `compiler/fir/`, `compiler/ir/`, `compiler/cli/` shared with non-scripting code. Comments are reviewed alongside the code; write them for a future reader of the **merged** module (an experienced compiler developer), not as a development journal. Enforcement is Non-Negotiable Rules 11 (name the justification at edit time) and 12 (reread the diff's comment lines before reporting).

### At edit time (the whole rule in five lines)

- Default: no comment. Deleting beats shortening.
- Allowed: why (non-obvious decision) / API contract / real trap + KT-issue or gotcha-ID reference.
- Banned: counterfactuals and filler comparisons, restatement of the code, peer/phase-order justification, caller lists, migration history, `.ai/` doc references.
- Form: plain declarative sentences, 1–3 lines. No "so that" / "because otherwise" / "note that".
- Same fact twice = delete the copy at the use site.

### The gate

**The default is no comment.** Human-maintained compiler code averages ~3% comment lines — treat an LLM-authored diff that lands noticeably above that as needing a cleanup pass before review. Before writing a comment, pass this gate — a comment is justified only when it:

1. explains **why** a non-obvious decision was made, or how a genuinely difficult piece works when words do it better or shorter than the code itself (e.g. why a configurator EP takes an abstract `KtSourceFile`/`KtSourceElement` instead of `KtScript`, why `$$eval` / `$$result` are fixed names); or
2. briefly states an **API contract** that saves the reader a detour into the implementation (e.g. refinement-callback ordering in `ScriptCompilationConfiguration`, what a `FirReplHistoryProvider` implementation must guarantee); or
3. records a **real trap** (the K2 REPL inliner gap, a cycle hazard, a parser-agnostic seam that must stay generic), ideally with a KT-issue (e.g. `KT-83498`) or gotcha ID (e.g. `G9`) reference into [`current/80-known-gotchas.md`](current/80-known-gotchas.md).

Everything else — delete. When in doubt, delete.

### Write facts, not narratives

The most repeated review complaint on LLM-authored diffs is narrative, justificatory tone. Human comments in this codebase state facts; match them.

- State what a thing **is**, not the story of why it ended up that way: `// Fixed name; the lowering and the evaluator agree on it.` — not `// We use a fixed name here because the lowering needs to find this function later, so it cannot be mangled.`
- Drop justification clauses (`so that`, `because otherwise`, `required because`, `note that`, `it is worth mentioning`) unless the justification *is* the real trap being recorded.
- Reference specs, issues and gotchas briefly: `// Snippet inliner gap, KT-83498.` — not a prose retelling of the issue.
- Prefer minimal punctuation: few em-dashes, few parenthetical asides.
- **After writing an analysis, a design doc, an iteration entry or a reply to the user, do not carry that register into code comments.** Explanatory prose, justification and comparison with the rejected option are correct there and a violation here; this transition is the most reliable predictor of a failed comment pass. Concrete rule: a sentence that appears in your analysis, iteration entry or reply MUST NOT be pasted or paraphrased into a source comment. If the comment says the same thing as the iteration entry, the comment is the redundant copy — delete it.

This is about *content*, not about writing in fragments: see "Compact for clarity, not for brevity" below — a comment that survives the gate is still written as readable sentences.

### Rejected comments and their replacements

Recurring shapes:

| Shape | Rejected | Replacement |
|---|---|---|
| Narrative | `// The converter is passed in rather than constructed here, because we want the K2 entry point to stay usable from tests that have no LightTree available, and because a future PSI converter could be plugged in the same way.` | `// Parser-agnostic seam: the caller supplies the converter.` |
| Counterfactual | `// …so the snippet keeps its own scope, instead of degrading to the script shape like the old K1 REPL did.` | *(deleted)* |
| Restatement | `// Returns null for snippets that carry no result property.` above `return snippet.resultProperty ?: return null` | *(deleted)* |
| Fact in two places | the `FirReplHistoryProvider` storage contract repeated at each call site | *(deleted at the call sites; kept on the interface)* |
| Peer/phase-order justification | `// Resolved here, as the script configurator does — snippet configuration runs before body resolution, so the symbol is already available.` | *(deleted; the phase-order finding belongs in the iteration entry)* |
| KDoc on a trivial accessor | `/** The compilation configuration this snippet was compiled with. */` above a one-line delegating property | *(deleted)* |

### Specific prohibitions

- **Don't comment the obvious.** If the code says it, or an experienced compiler developer sees it at a glance, no comment. This includes restating a function's body in prose, `@param`/`@property` entries that paraphrase the parameter or field name/type (document only non-obvious contracts, or none), and spelling out a default value that's already visible right there in the signature.
- **No counterfactuals or filler comparisons.** Don't describe rejected designs, an earlier or alternative implementation, or how the current code relates to some other component ("unlike X", "just like Y", "behaves exactly as Z would") — unless the comparison reveals a real trap a maintainer is likely to fall into, in which case state it in one terse sentence, not a paragraph. A comparison whose conclusion is already obvious from the surrounding design is filler, not a trap, and should be cut.
- **No caller inventories.** Don't enumerate which configurator EPs, refinement callbacks, or call sites use a given class/function ("used by X, Y and Z").
- **One fact, one place.** State a fact at the declaration site only (e.g. on `FirScriptConfiguratorExtension` itself, not repeated at each override or call site).
- **No references to `.ai/` docs.** Never cite `current/*.md`, `target/*.md`, `iterations/*.md`, or a migration-plan step name/number (`step 2`, `step 4–11`) in source comments — inline the (brief) explanation itself instead.
- **Describe the current state only.** No narration of K1→K2 migration history, no "this used to live in the daemon REPL", no dated history.
- **Don't reintroduce REMOVE-tagged concepts as if current.** Comments on live code shouldn't frame behavior in terms of `-Xrepl`, `cli-base/repl/*`, `GenericReplCompiler`, or `scripting-ide-common` unless the comment is specifically about the removal/migration seam itself (per [`current/90-legacy-inventory.md`](current/90-legacy-inventory.md)).
- **Don't blur scripts and snippets.** A comment on `FirScript`-side code shouldn't describe `FirReplSnippet` behavior or vice versa — they're different shapes; cross-reference instead of merging the explanation.
- **Use the codebase's own vocabulary.** Reuse the term a concept already has elsewhere — a property name, a neighboring KDoc in the same subsystem, established compiler terminology — instead of coining a fresh synonym for it. A comment that introduces new wording for something that already has a name forces the reader to mentally translate between two vocabularies for no benefit.
- **Compact for clarity, not for brevity.** The gate above decides *what* survives; it isn't license to squeeze whatever survives into the fewest possible characters. Once a comment has earned its place, write it as natural, complete sentences — the way an experienced compiler developer would actually phrase it out loud — rather than a telegraphic run of noun-phrase fragments strung together with dashes or semicolons. A short, plainly readable sentence beats a denser one that has to be decoded; a little redundancy in service of readability is fine.
- **No peer or phase-order justification.** Don't explain a line by what a neighbouring implementation does or by where the code sits in the pipeline ("the script configurator does the same", "snippet configuration runs before body resolution") — that belongs in the iteration entry or a `current/*` doc, not in the source.
- **Keep it short.** 1–3 lines is the norm. A short paragraph of full sentences is fine — and preferable to a cramped one-liner — for a genuinely tricky invariant that can't be compressed further (e.g. the K2 REPL inliner gap's list of unsafe constructs); even then, cut filler ("Note that", "It is worth mentioning"). KDoc on internal declarations is the exception, not the rule — human-maintained compiler modules leave most internal functions undocumented. No numbered algorithm walkthroughs or "Scenario A/B/C" breakdowns in KDoc; if the algorithm needs that, it belongs in a `current/*` doc.

### Keep comments in sync with the code

Stale comments are a recurring review find. Whenever you delete or rename a symbol, or change behavior, **grep for comments mentioning it** — across the whole subsystem, not just the edited file — and fix or delete them in the same change. This matters more here than in most modules: the K1 cleanup chain deletes symbols that live comments still name. A comment that justifies complexity must be re-verified against the *current* code, not carried forward from an earlier round.

### Self-check

Non-Negotiable Rule 12 is the self-check: reread the diff's comment lines alone and ask (a) does each one survive the gate above, or a reviewer asking "what does this tell me that the code doesn't?"; (b) does each use the terms this codebase already uses for the concept, rather than a new synonym; (c) does each state a fact rather than narrate a rationale; (d) does each read as natural, complete sentences rather than compressed fragments. If a comment fails any of these, fix or remove it.

---

## Explanation & Writing Style (iteration entries, docs, review replies)

The opposite register from source comments: here, explanation is the point. Reviewers repeatedly had to ask "what does this mean / when is this reached?" about explanations that were formally correct but too compressed or too abstract.

- **Lead with the current behavior** in one plain sentence; put the rationale after it, not interleaved with it.
- **Ground every guard, fallback or special case in a concrete trigger**: name the input, code path or test that reaches it (`a snippet whose previous cell declared an @InlineOnly extension`), not just the abstract condition. If you cannot name one, that is a signal the code may be unnecessary — see Simplification & Review Discipline.
- **Prefer short declarative sentences** over long noun phrases and nested subordinate clauses. One idea per sentence.
- **No contrast with the unimplemented**: describe what the code does, not what it does instead of some alternative. Rejected options live in [`target/40-jsr223-options-archive.md`](target/40-jsr223-options-archive.md) and the [`target/90-open-questions.md`](target/90-open-questions.md) triage fields, not in running prose.

---

## Simplification & Review Discipline

The goal is to reach the simplified end state in one pass, and to catch small gaps before review does.

- **Default to one generic path.** When the same operation is implemented separately per representation or host (script vs snippet, LT vs PSI, CLI vs host embedding), treat the split as a hypothesis to disprove: look for the one existing generic mechanism the specialized arms could route through. (Scripts and snippets stay distinct *in FIR* — see Critical Patterns — but their surrounding plumbing usually shouldn't fork.)
- **Search for an existing helper before writing one.** Grep for the operation's key ingredient first; `libraries/scripting/*` already has utilities for most classpath, configuration-merge and evaluation plumbing.
- **Check the peer implementation at every decision point.** When a reference implementation of the same behavior exists (the script path for a snippet question, the K2 CLI path for a host question, an upstream file), compare before designing — most "missed small details" found in review were places where a peer already handled the case.
- **"A unit test injects a fake here" is not a production justification.** If a parameter, overload or lambda exists only for a test's convenience, change the test: write an end-to-end test against the real wiring, or add a narrow test double at the correct architectural boundary. (The `convertToFir` seam is a design decision, not a test hook — don't cite it as precedent.)
- **Parallel caches, lists or maps need a demonstrated distinct answer.** Two structures that differ only in a filter are one structure until a test shows the answers diverge.
- **Re-derive, don't recite.** Any claim that complexity is "necessary" must be re-verified against the current code on every review pass — trace the actual call sites and data flow again; earlier reasoning goes stale fast in a branch that is deleting modules.
- **Answer capability questions with evidence, not assumption.** "Is this tested?", "is this reachable?", "is this parameter used?" are answered by grepping call sites or running the scenario, not by recalling a doc.
- **Treat a repeated "are we sure?" from the user as a cue to re-investigate from scratch**, with a concrete test or reachable call path as the outcome — not to restate the previous answer. Healthy resistance is still expected, but only backed by a failing regression test or a specific reachable call path produced in the same pass; otherwise implement the simplification.

---

## Docs Maintenance

Keep the working doc set small — this file and the iteration index are read into context every session.

- **One fact, one doc.** When the same information (status, rule, file map) is needed in two documents, one owns it and the other links to it. The canonical homes are listed in Reference Documents.
- **[`ITERATION_RESULTS.md`](ITERATION_RESULTS.md) is an append-only index**: one line per iteration, newest on top. Detail, traces and measurement tables go in the `iterations/YYYY-MM-DD_slug.md` entry — never inlined into the index, and never pasted logs or diffs.
- **Archive a doc once its work has fully landed or been superseded** (as [`target/40-jsr223-options-archive.md`](target/40-jsr223-options-archive.md) already is): move it, add a banner with the archive date and a staleness warning, and repoint references. Keep only living docs for *active* work outside the archive.
- **Bump "Last verified"** in any doc whose body text materially changed — see the Post-iteration checklist.

---

## Reference Documents

Each doc has a "When to consult / Cache lifetime / Last verified" header — check it before loading.

| Document | When to consult |
|---|---|
| [`README.md`](README.md) | First-time orientation — file map, daily workflow, command reference, prompting patterns, troubleshooting. Read once. |
| [`ITERATION_RESULTS.md`](ITERATION_RESULTS.md) | At iteration start (review) and end (append one-line index entry). Per-entry detail goes in `iterations/`. |
| [`ITERATION_TEMPLATE.md`](ITERATION_TEMPLATE.md) | Copy this when creating an iteration entry under `iterations/`. |
| [`PROCESS_AUDIT.md`](PROCESS_AUDIT.md) | Periodic self-audit playbook — run when a trigger fires (every ~10 iterations / 4 weeks / regression streak / cost spike / log overflow). Findings → `iterations/audit_YYYY-MM-DD.md`. |
| [`current/00-overview.md`](current/00-overview.md) | First read — layer × module map + K2 pipeline diagram. |
| [`current/10-compiler-representation.md`](current/10-compiler-representation.md) | **Canonical home for KT-83498 line anchors and the 6 configurator EPs enumeration.** Read first for any compiler-side edit. |
| [`current/20-customization.md`](current/20-customization.md) | Refinement DSL + how it wires into FIR. Read before touching extension impls. |
| [`current/30-api-layer.md`](current/30-api-layer.md) | `libraries/scripting/*` catalog. K2 compilation core wrappers. |
| [`current/40-embedding-cli.md`](current/40-embedding-cli.md) | `-script` / plugin autoload / CLI K2 entry chain (ACTIVE surface). |
| [`current/45-embedding-daemon-legacy.md`](current/45-embedding-daemon-legacy.md) | Historical: what the daemon REPL / `-Xrepl` / cli-base/repl/* surface was, and what survives of it. **Consult ONLY when finishing migration step 6.** |
| [`current/41-embedding-build.md`](current/41-embedding-build.md) | Gradle subplugin + BTA discovery op. |
| [`current/50-script-definitions.md`](current/50-script-definitions.md) | Definition discovery + main-kts canonical example. |
| [`current/60-jsr223.md`](current/60-jsr223.md) | K2 engine state; bindings design → `target/40-jsr223-target.md` Option D. |
| [`current/70-tests.md`](current/70-tests.md) | Per-module + compiler-side test inventory with disposition. Includes JSR-223 per-test `BLOCKED-BY` matrix. |
| [`current/80-known-gotchas.md`](current/80-known-gotchas.md) | **Stable-prefix catalog** of K2 REPL / JSR-223 pitfalls (G1–G10). Load early — promoted from iteration `Key Learnings`. |
| [`current/90-legacy-inventory.md`](current/90-legacy-inventory.md) | **Authoritative**: K1/PSI/IDE-coupled/duplicated artifacts with disposition. |
| [`target/00-principles.md`](target/00-principles.md) | P1–P9 + P4a. Read before any architectural decision. |
| [`target/10-compiler-target.md`](target/10-compiler-target.md) | Keep / remove / refactor per compiler subsystem. |
| [`target/20-api-target.md`](target/20-api-target.md) | `libraries/scripting/*` post-cleanup shape. |
| [`target/30-embedding-target.md`](target/30-embedding-target.md) | CLI / daemon / Gradle / BTA post-cleanup shape. |
| [`target/40-jsr223-target.md`](target/40-jsr223-target.md) | **Canonical home for bindings (Option D) + stateless remote compilation.** |
| [`target/40-jsr223-options-archive.md`](target/40-jsr223-options-archive.md) | Historic rejection rationale for options A/B/C — only when reopening the design. |
| [`target/50-migration-plan.md`](target/50-migration-plan.md) | **Step 1–14 = task IDs.** Step 2 is the canonical KT-83498 design home. |
| [`target/90-open-questions.md`](target/90-open-questions.md) | Q1–Q16 with triage fields. Sub-questions Q5a–e, Q10a–f, Q13a–b delegate-able. |

## Repo-wide references

See repo `CLAUDE.md` for commit guidelines, code-review conventions, and Build Tools API docs. See [`../../../.ai/guidelines.md`](../../../.ai/guidelines.md), [`../../../compiler/AGENTS.md`](../../../compiler/AGENTS.md), [`../../../docs/fir/fir-basics.md`](../../../docs/fir/fir-basics.md) for compiler-side conventions. Test-data discipline: see Non-Negotiable Rule #9 above.

---

*Last updated: 2026-09-04 (synchronised the general working rules with
`compiler/java-direct/AGENT_INSTRUCTIONS_COMMON.md`, which had grown the module-independent
half of the rule set. Comment discipline is now enforced rather than advisory: new
Non-Negotiable Rules 11–12 make comments opt-in (name the why / API-contract / trap
justification at edit time) and make the comment-lines-only diff reread
(`git diff -U0 | grep '^+' | grep -E '//|\*'`) a step of the change rather than a review
afterthought. Source Comment Conventions gained an "at edit time" five-line summary, a
"write facts, not narratives" section — including the ban on carrying analysis / iteration-entry
prose into source comments, which is the strongest predictor of a failed comment pass — a
rejected-comment/replacement table, prohibitions on peer/phase-order justification and on
KDoc algorithm walkthroughs, and a comment-staleness rule (grep for comments naming a symbol
you delete or rename — acute here because of the K1 cleanup chain). Added the sibling file's
Explanation & Writing Style, Simplification & Review Discipline and Docs Maintenance sections,
plus Rule 10 (only the main agent runs Gradle), the no-hand-editing-shared-test-data half of
Rule 9, and the `--stacktrace` / no-`--rerun-tasks` / forgot-`tee` Gradle rules.)*

*Previously: 2026-08-05 (refined the Source Comment Conventions section with two more general
rules found while dogfooding it: match the codebase's existing terminology for a concept instead
of coining a new synonym, and compact for clarity rather than raw brevity, so a kept comment reads
as natural sentences rather than a telegraphic, dash-chained fragment; also broadened the
counterfactual-comparison ban to cover filler comparisons in general, not only rejected designs.)*

*Previously: 2026-08-05 (added the Source Comment Conventions section, adapted from
`compiler/java-direct/AGENT_INSTRUCTIONS.md`: the "default is no comment" gate (why-decisions,
API contracts, real traps only, with KT-issue/gotcha-ID references), bans on counterfactual
"rather than the old K1 path" phrasing, caller inventories, and `.ai/` doc citations in source
comments, plus a scripting-specific rule against blurring `FirScript` and `FirReplSnippet`
comments.)*

*Previously: 2026-05-18.*

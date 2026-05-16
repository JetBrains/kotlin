# Scripting/REPL — Agent Instructions

**Current status**: Pre-cleanup snapshot. K2 path is the active path for scripts (LightTree-based, parser-agnostic core). K1 frontend retirement in progress. Multiple parallel impls still live for back-compat. Three open workstreams: **KT-83498** (LightTree for REPL snippets), **JSR-223 K2 bindings** (Option D recommended), **stateless remote REPL compilation** prototype.

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
3. **No reviving daemon REPL / `-Xrepl` / `cli-base/repl/*`.** Goal is to delete these entirely (see [`target/30-embedding-target.md`](target/30-embedding-target.md)).
4. **Don't add a PSI-only K2 path.** `ScriptJvmK2CompilerImpl`'s `convertToFir` lambda is the seam. LT is the only wired converter today. If you need a non-LT path for a real reason, discuss before coding.
5. **Don't tighten `K2ReplCompiler`'s PSI special-casing.** **KT-83498** removes the split — help unify, don't add new PSI-only branches. Line anchors in [`current/10-compiler-representation.md`](current/10-compiler-representation.md); design in [`target/50-migration-plan.md`](target/50-migration-plan.md) step 2.
6. **No `intellij-community` plugin dependencies in `plugins/scripting/*`.** `scripting-ide-common` (copied from IntelliJ monorepo) is REMOVE.
7. **`libraries/scripting/intellij` is public surface.** It's used by IntelliJ plugin authors wiring custom-scripts support. Don't break compatibility; don't move/rename.
8. **NEVER create git commits without explicit user review.** All changes pass through the user first.
9. **Test data**: NEVER run `-Pkotlin.test.update.test.data=true` unless the user explicitly asks. Test data is shared across runners; bulk updates corrupt the dataset. After adding new test data fixtures: `./gradlew generateTests`. (Canonical statement — Repo Conventions section refers here.)

---

## Shell Discipline

### Session temp directory

At session start:
```bash
export SCRIPTING_TMP="/tmp/scr_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$SCRIPTING_TMP"
```
All Gradle output and saved diffs go under `$SCRIPTING_TMP`. Never write directly to `/tmp/`.

`tmp/` under `.ai/` is for scratch only; files older than 7 days are deletable without review. Not git-tracked beyond the current iteration. See [`ITERATION_RESULTS.md`](ITERATION_RESULTS.md) "tmp/ retention" for details.

### One command per Bash call

The permission system matches the **first token only**. With `cmd1 && cmd2`, only `cmd1` is checked. Run sequential commands as separate tool calls. `|` (piping) is fine; `&&`, `||`, `;` are not.

### Gradle runs: save output, run once

Every Gradle invocation MUST `tee` to `$SCRIPTING_TMP`. After a run, grep the saved file — never rerun Gradle just to see a different slice. Don't use `--info`/`--debug` unless needed.

---

## Ground Rules

- **Use JetBrains IDE MCP** for project file operations per repo `CLAUDE.md` (`search_in_files_by_text`, `replace_text_in_file`, `get_file_text_by_path`, `get_file_problems`, `rename_refactoring`). Fall back to standard tools only when MCP isn't available.
- **Search before reading** — prefer `search_in_files_by_text`/`search_in_files_by_regex` over loading whole files.
- **`get_file_problems` after edits** with `errorsOnly=false`. Fix warnings related to your changes.
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
- **Configurator EPs are PSI-agnostic by contract.** They take abstract `KtSourceFile` / `KtSourceElement`. Don't add `as? KtScript` casts. The residual one is being removed by KT-83498 — don't extend that pattern.
- **`KtScript.isReplSnippet`** is the snippet marker for PSI sources. K2 + PSI path relies on this.
- **Scripts vs snippets in FIR.** Different shape, different EPs. Don't unify them at the FIR level.

---

## Active Workstreams

Priority TBD — the list below is unordered.

- **KT-83498** — Full LightTree path in `K2ReplCompiler`. See [`target/50-migration-plan.md`](target/50-migration-plan.md) step 2 (canonical home).
- **JSR-223 K2 bindings** — Option D — implicit-snippets refinement-DSL callback. See [`target/40-jsr223-target.md`](target/40-jsr223-target.md) and [`target/50-migration-plan.md`](target/50-migration-plan.md) step 1.
- **Stateless remote REPL compilation** prototype — See [`target/40-jsr223-target.md`](target/40-jsr223-target.md) and [`target/50-migration-plan.md`](target/50-migration-plan.md) step 3.
- **K1 cleanup chain** — Daemon REPL → `-Xrepl` → `cli-base/repl/*` → `legacyRepl*.kt` → `GenericReplCompiler` → K1 frontend bindings. Sequenced in [`target/50-migration-plan.md`](target/50-migration-plan.md) steps 4–11.
- **`scripting-ide-{common,services}` deletion** — [`target/50-migration-plan.md`](target/50-migration-plan.md) steps 9–10.
- **Classpath-discovery SPI decision** (KT-82551) — un-deprecate + document or design successor. [`target/50-migration-plan.md`](target/50-migration-plan.md) step 13.
- **Compiler-side test cleanup** — [`target/50-migration-plan.md`](target/50-migration-plan.md) step 12.

## Post-iteration checklist

After landing a migration-plan step:

1. Create iteration file at `iterations/YYYY-MM-DD_slug.md` from [`ITERATION_TEMPLATE.md`](ITERATION_TEMPLATE.md). Append one-line index entry to [`ITERATION_RESULTS.md`](ITERATION_RESULTS.md).
2. Strike the step in `target/50-migration-plan.md`: `### N. ~~Title~~ — landed YYYY-MM-DD`.
3. Update **Active Workstreams** list in this file if a workstream completed.
4. Update `current/90-legacy-inventory.md` disposition rows for any deleted artifact.
5. Update `current/40-embedding-cli.md` / `current/45-embedding-daemon-legacy.md` / `current/70-tests.md` if surface changed.
6. If a Q* in `target/90-open-questions.md` resolved → flip status to `resolved` and link the landing iteration in the Target-doc field.
7. Bump **Last verified** date in any doc whose body text materially changed.

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

## Per-Task Agent Loadout

Use the minimal core-doc set for your task. Skip the rest unless explicitly needed.

| Task type | Core docs (always load) | Optional (load on demand) | Budget | Model | Subagent |
|---|---|---|---|---|---|
| K2 compiler edit (FIR/IR/lowerings) | this file + `current/10-compiler-representation.md` + `target/10-compiler-target.md` | `current/00-overview.md`, `target/00-principles.md` | ~6k | Sonnet (Opus for cross-EP design) | `cavecrew-investigator` → `cavecrew-builder` |
| Legacy K1 audit / deletion | this file + `current/90-legacy-inventory.md` + `target/50-migration-plan.md` (one step) | `current/40-embedding-cli.md`, `current/45-embedding-daemon-legacy.md`, `current/30-api-layer.md` | ~5k | Haiku → Sonnet | `cavecrew-investigator` |
| Migration-step execution (one numbered step) | this file + `target/50-migration-plan.md` (one step + sequencing tail) | step's "Touch" files | ~7k | Sonnet | `cavecrew-builder` per file |
| JSR-223 / bindings design | this file + `target/40-jsr223-target.md` + `current/60-jsr223.md` | `target/90-open-questions.md` Q10, `target/20-api-target.md` | ~9k | Opus | `Plan` → `cavecrew-builder` |
| Stateless remote REPL design | this file + `target/40-jsr223-target.md` (remote section) + `current/30-api-layer.md` | `target/90-open-questions.md` Q5 sub-table | ~8k | Opus | `Plan` |
| Test triage | this file (Test Commands) + `current/70-tests.md` | `current/90-legacy-inventory.md`, `target/50-migration-plan.md` step 12 | ~4k | Haiku → Sonnet on cluster | `Explore` for failure-text search |
| Doc maintenance | this file + `ITERATION_RESULTS.md` | the one doc being edited | ~3k | Haiku | none |
| Cross-module change (>3 files) | task core + `current/00-overview.md` | as above | ~10k | Opus | `cavecrew-investigator` MUST run first |

## Caching strategy — file load order

Load stable → mutable so the prefix cache survives across iterations:

1. `AGENT_INSTRUCTIONS.md` (this file — stable prefix; pin to cache).
2. `current/00-overview.md`, `current/10-compiler-representation.md`, `target/00-principles.md` (stable).
3. Task-specific `current/*` + `target/{10,20,30}-*.md`.
4. Mutable tail: `target/40-jsr223-target.md` (if relevant), `target/50-migration-plan.md`, `target/90-open-questions.md`, `current/70-tests.md`, `current/90-legacy-inventory.md`.
5. Last: `ITERATION_RESULTS.md`.

When `target/40-jsr223-target.md` or `target/90-open-questions.md` is rewritten during prototyping, the prefix (1–3) stays cached.

---

## Reference Documents

Each doc has a "When to consult / Cache lifetime / Last verified" header — check it before loading.

| Document | When to consult |
|---|---|
| [`ITERATION_RESULTS.md`](ITERATION_RESULTS.md) | At iteration start (review) and end (append one-line index entry). Per-entry detail goes in `iterations/`. |
| [`ITERATION_TEMPLATE.md`](ITERATION_TEMPLATE.md) | Copy this when creating an iteration entry under `iterations/`. |
| [`current/00-overview.md`](current/00-overview.md) | First read — layer × module map + K2 pipeline diagram. |
| [`current/10-compiler-representation.md`](current/10-compiler-representation.md) | **Canonical home for KT-83498 line anchors and the 6 configurator EPs enumeration.** Read first for any compiler-side edit. |
| [`current/20-customization.md`](current/20-customization.md) | Refinement DSL + how it wires into FIR. Read before touching extension impls. |
| [`current/30-api-layer.md`](current/30-api-layer.md) | `libraries/scripting/*` catalog. K2 compilation core wrappers. |
| [`current/40-embedding-cli.md`](current/40-embedding-cli.md) | `-script` / plugin autoload / CLI K2 entry chain (ACTIVE surface). |
| [`current/45-embedding-daemon-legacy.md`](current/45-embedding-daemon-legacy.md) | Daemon REPL + `-Xrepl` + cli-base/repl/* (ALL REMOVE). **Consult ONLY when executing migration step 4, 5, or 6.** |
| [`current/41-embedding-build.md`](current/41-embedding-build.md) | Gradle subplugin + BTA discovery op. |
| [`current/50-script-definitions.md`](current/50-script-definitions.md) | Definition discovery + main-kts canonical example. |
| [`current/60-jsr223.md`](current/60-jsr223.md) | K2 engine state; bindings design → `target/40-jsr223-target.md` Option D. |
| [`current/70-tests.md`](current/70-tests.md) | Per-module + compiler-side test inventory with disposition. |
| [`current/90-legacy-inventory.md`](current/90-legacy-inventory.md) | **Authoritative**: K1/PSI/IDE-coupled/duplicated artifacts with disposition. |
| [`target/00-principles.md`](target/00-principles.md) | P1–P9 + P4a. Read before any architectural decision. |
| [`target/10-compiler-target.md`](target/10-compiler-target.md) | Keep / remove / refactor per compiler subsystem. |
| [`target/20-api-target.md`](target/20-api-target.md) | `libraries/scripting/*` post-cleanup shape. |
| [`target/30-embedding-target.md`](target/30-embedding-target.md) | CLI / daemon / Gradle / BTA post-cleanup shape. |
| [`target/40-jsr223-target.md`](target/40-jsr223-target.md) | **Canonical home for bindings (Option D) + stateless remote compilation.** |
| [`target/40-jsr223-options-archive.md`](target/40-jsr223-options-archive.md) | Historic rejection rationale for options A/B/C — only when reopening the design. |
| [`target/50-migration-plan.md`](target/50-migration-plan.md) | **Step 1–14 = task IDs.** Step 2 is the canonical KT-83498 design home. |
| [`target/90-open-questions.md`](target/90-open-questions.md) | Q1–Q12 with triage fields. Sub-questions Q5a–e, Q10a–f delegate-able. |

## Repo-wide references

See repo `CLAUDE.md` for commit guidelines, code-review conventions, and Build Tools API docs. See [`../../.ai/guidelines.md`](../../.ai/guidelines.md), [`../../compiler/AGENTS.md`](../../compiler/AGENTS.md), [`../../docs/fir/fir-basics.md`](../../docs/fir/fir-basics.md) for compiler-side conventions. Test-data discipline: see Non-Negotiable Rule #9 above.

---

*Last updated: 2026-05-16.*

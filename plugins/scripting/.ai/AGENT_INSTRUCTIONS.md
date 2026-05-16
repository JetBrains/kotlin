# Scripting/REPL — Agent Instructions

**Current status**: Pre-cleanup snapshot. K2 path is the active path for scripts (LightTree-based, parser-agnostic core). K1 frontend retirement in progress. Multiple parallel impls still live for back-compat. Three open workstreams: **KT-83498** (LightTree for REPL snippets), **JSR-223 K2 bindings** (option D recommended), **stateless remote REPL compilation** prototype.

**Scope**: `plugins/scripting/*`, `libraries/scripting/*`, `libraries/tools/kotlin-main-kts*`, the scripting-related parts of `compiler/cli/`, `compiler/daemon/`, `compiler/fir/`, `compiler/ir/`, `compiler/build-tools/`, and `libraries/tools/kotlin-gradle-plugin/.../scripting/`.

**Doc set**: lives under `plugins/scripting/.ai/`. `current/` maps today's state; `target/` describes the cleanup target. After brainstorm, these notes are distilled into compact `AGENTS.md` files at relevant module roots.

**Key files** (full map: [`current/10-compiler-representation.md`](current/10-compiler-representation.md)):
- `plugins/scripting/scripting-compiler/src/.../impl/ScriptJvmK2CompilerImpl.kt` — parser-agnostic K2 script compiler core
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` — K2 REPL orchestrator (hybrid PSI/LT today; KT-83498 unifies)
- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplEvaluator.kt`
- `plugins/scripting/scripting-compiler/src/.../services/FirReplSnippetConfiguratorExtensionImpl.kt`
- `plugins/scripting/scripting-compiler/src/.../services/FirReplSnippetResolveExtensionImpl.kt`
- `plugins/scripting/scripting-compiler/src/.../services/Fir2IrReplSnippetConfiguratorExtensionImpl.kt`
- `plugins/scripting/scripting-compiler/src/.../irLowerings/{ScriptLowering,ReplSnippetLowering}.kt`
- `plugins/scripting/scripting-compiler/src/.../JvmCliScriptEvaluationExtension.kt` — CLI entry
- `libraries/scripting/jvm-host/.../JvmScriptCompiler.kt`, `.../KotlinJsr223ScriptEngineImpl.kt`
- Generated FIR: `compiler/fir/tree/gen/.../{FirScript,FirReplSnippet}.kt` (don't hand-edit)
- Generated IR: `compiler/ir/ir.tree/gen/.../{IrScript,IrReplSnippet}.kt` (don't hand-edit)

---

## Glossary

| Term | Meaning |
|---|---|
| **Script** | Whole `.kts` file compiled to a class. FIR repr: `FirScript` (statements + params + receivers). |
| **REPL snippet** | One input chunk in an interactive session. FIR repr: `FirReplSnippet` — embeds `FirRegularClass` + `$$eval` function. **Different shape from script.** |
| **K1 / FE 1.0** | Legacy frontend (descriptor-based, PSI-tied). |
| **K2 / FIR** | Current frontend. |
| **Configurator extension** | Plugin seam to mutate FIR during build / resolve / FIR-to-IR for scripts and snippets. 6 EPs total (3 for script, 3 for snippet). |
| **Script definition** | `@KotlinScript`-annotated class declaring script shape (base class, default imports, refinement handlers, file extension). |
| **Refinement** | User-supplied callbacks that mutate `ScriptCompilationConfiguration` before parsing / on annotations / before compilation / before evaluation. Public customization surface. |
| **Implicit snippet** | (Planned) Synthetic snippet emitted by a refinement-DSL callback to run before the user's snippet — e.g. JSR-223 binding cell. See [`target/40-jsr223-target.md`](target/40-jsr223-target.md) Option D. |

---

## ⚠ Non-Negotiable Rules (stop immediately if violated)

1. **No new K1 paths.** Modules tagged REMOVE in [`current/90-legacy-inventory.md`](current/90-legacy-inventory.md) are slated for deletion — don't extend them, don't add new callers. Specifically: `LazyScriptDescriptor`, `GenericReplCompiler`, `JvmReplCompiler`/`JvmReplEvaluator` (`legacy*.kt`), `ScriptingCompilerConfigurationComponentRegistrar`.
2. **No new public extension points without ratification.** The current EP contract: `FirScriptConfiguratorExtension`, `FirReplSnippetConfiguratorExtension`, `FirScriptResolutionConfigurationExtension`, `FirReplSnippetResolveExtension`, `Fir2IrScriptConfiguratorExtension`, `Fir2IrReplSnippetConfiguratorExtension`. User customizations go through the `ScriptCompilationConfiguration` refinement DSL — see [`current/20-customization.md`](current/20-customization.md).
3. **No reviving daemon REPL / `-Xrepl` / `cli-base/repl/*`.** Goal is to delete these entirely (see [`target/30-embedding-target.md`](target/30-embedding-target.md)).
4. **Don't add a PSI-only K2 path.** `ScriptJvmK2CompilerImpl`'s `convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile` lambda is the seam. LT (`convertToFirViaLightTree`) is the only wired converter today. If you need a non-LT path for a real reason, discuss before coding.
5. **Don't tighten `K2ReplCompiler`'s PSI special-casing.** Lines 351-359 split `KtFileScriptSource` (PSI) from other sources (LT). **KT-83498** removes the split. Don't add new PSI-only branches; help unify instead.
6. **No `intellij-community` plugin dependencies in `plugins/scripting/*`.** `scripting-ide-common` (copied from IntelliJ monorepo) is REMOVE — don't extend or recreate that pattern.
7. **`libraries/scripting/intellij` is public surface.** It's used by IntelliJ plugin authors wiring custom-scripts support. Don't break compatibility; don't move/rename.
8. **NEVER create git commits without explicit user review.** All changes pass through the user first.
9. **NEVER run `-Pkotlin.test.update.test.data=true`** unless the user explicitly asks. Test data is shared across runners; bulk updates corrupt the dataset.

---

## Shell Discipline

### Session temp directory

At session start:
```bash
export SCRIPTING_TMP="/tmp/scr_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$SCRIPTING_TMP"
```
All Gradle output and saved diffs go under `$SCRIPTING_TMP`. Never write directly to `/tmp/`.

### One command per Bash call

The permission system matches the **first token only**. With `cmd1 && cmd2`, only `cmd1` is checked. Run sequential commands as separate tool calls. `|` (piping) is fine; `&&`, `||`, `;` are not.

### Gradle runs: save output, run once

Every Gradle invocation MUST `tee` to `$SCRIPTING_TMP`. After a run, grep the saved file — never rerun Gradle just to see a different slice. Don't use `--info`/`--debug` unless needed.

---

## Ground Rules

- **Use JetBrains IDE MCP** for project file operations per the repo `CLAUDE.md` (`search_in_files_by_text`, `replace_text_in_file`, `get_file_text_by_path`, `get_file_problems`, `rename_refactoring`). Fall back to standard tools only when MCP isn't available.
- **Search before reading** — prefer `search_in_files_by_text`/`search_in_files_by_regex` over loading whole files; tree-search returns only matching lines.
- **`get_file_problems` after edits** with `errorsOnly=false`. Fix warnings related to your changes.
- **Check `git diff` for unintended changes** after every test run.
- **Read the relevant `current/*` doc first** when touching a subsystem. The doc set is the single source of truth on existing structure; the code is too sprawled to derive intent from cold.
- **FIR/script terminology**: "script" = whole `.kts` file → `FirScript`. "REPL snippet" = interactive cell → `FirReplSnippet` (different shape: embedded `FirRegularClass` + `$$eval`). Don't conflate.

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

# After adding new test data fixtures
./gradlew generateTests
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
| `compiler/fir/tree/gen/.../FirScript.kt`, `FirReplSnippet.kt` | Generated; schema lives in the FIR tree generator | Regenerate via the FIR tree gen task — don't hand-edit |
| `compiler/ir/ir.tree/gen/.../IrScript.kt`, `IrReplSnippet.kt` | Generated | Regenerate — don't hand-edit |
| `compiler/fir/raw-fir/raw-fir.common/.../FirScriptConfiguratorExtension.kt`, `FirReplSnippetConfiguratorExtension.kt` | Public EP contracts | Compare with upstream; run scripts + REPL suites before/after |
| `compiler/fir/providers/.../FirReplSnippetResolveExtension.kt` | Public EP contract; storage-defined-by-impl | Same as above |
| `compiler/fir/fir2ir/.../Fir2IrScriptConfiguratorExtension.kt`, `Fir2IrReplSnippetConfiguratorExtension.kt` | Public EP contracts | Same as above |
| `compiler/cli/cli-jvm/src/.../pipeline/jvm/JvmScriptPipelinePhase.kt` | CLI entry; user-visible behavior | Run `LauncherScriptTest` + integration smoke fixtures |
| `compiler/arguments/.../CommonCompilerArguments.kt` | CLI argument table; deprecation discipline applies | Coordinate any flag change separately |
| `libraries/scripting/common/api/scriptCompilation.kt`, `scriptEvaluation.kt` | Public API; binary-compat matters | Treat as `@SinceKotlin`-stable; no breaking changes without deprecation cycle |
| `libraries/scripting/intellij/*` | Public surface for IntelliJ plugin authors | No breaking changes |

---

## Critical Patterns (do not break)

- **Parser-agnostic seam.** `ScriptJvmK2CompilerImpl` (`plugins/scripting/scripting-compiler/src/.../impl/ScriptJvmK2CompilerImpl.kt:109`) takes a `convertToFir` lambda; only `convertToFirViaLightTree` (line 317) is wired. New K2 entry points should mirror this shape — pass the converter in, don't bind it inside.
- **REPL history is storage-defined by impl.** `FirReplHistoryProvider` (`compiler/fir/providers/src/.../extensions/FirReplSnippetResolveExtension.kt`) has 4 abstract methods (`getSnippets`/`putSnippet`/`isFirstSnippet`/`getSnippetCount`). The current impl is in-memory; a class-file-backed impl is the seam for the stateless remote-compilation work. Don't bind in-memory assumptions into callers.
- **`$$eval` / `$$result` constants** live in `ReplSnippetsToClassesLowering` (`plugins/scripting/scripting-compiler/src/.../irLowerings/ReplSnippetLowering.kt`): `REPL_SNIPPET_EVAL_FUN_NAME = "$$eval"`, `REPL_SNIPPET_RESULT_PROP_NAME = "$$result"`. Don't shadow or rename.
- **Configurator EPs are PSI-agnostic by contract.** They take abstract `KtSourceFile` / `KtSourceElement`. Don't add `as? KtScript` casts. The residual one at `FirReplSnippetConfiguratorExtensionImpl.kt:173` is being removed by KT-83498 — don't extend that pattern.
- **`KtScript.isReplSnippet`** is the snippet marker for PSI sources. Propagated via `KotlinScriptStubImpl`. K2 + PSI path (`KtFileScriptSource` branch in `K2ReplCompiler`) relies on this.
- **Scripts vs snippets in FIR.** `FirScript` = statements + params + receivers. `FirReplSnippet` = embedded `FirRegularClass` + `$$eval` fn. Different shape, different EPs. Don't unify them at the FIR level.

---

## Active Workstreams

Priority TBD — the list below is unordered.

- **KT-83498** — Full LightTree path in `K2ReplCompiler`. Aligns snippet pipeline with `ScriptJvmK2CompilerImpl` shape. Removes the `KtFileScriptSource` PSI branch and the PSI touch at `FirReplSnippetConfiguratorExtensionImpl.kt:173`. See [`target/10-compiler-target.md`](target/10-compiler-target.md), [`target/50-migration-plan.md`](target/50-migration-plan.md).
- **JSR-223 K2 bindings** — Restore feature parity post-K2 port (commit `04ecbd1f8a7f`). Recommended approach: **Option D** — implicit-snippets refinement-DSL callback + JSR-223 binding configurator (definition-side). See [`target/40-jsr223-target.md`](target/40-jsr223-target.md).
- **Stateless remote REPL compilation** prototype — Move REPL compilation state out of the compiler. Snippet output = class files + sidecar metadata. Caller owns history. Storage-backed `FirReplHistoryProvider`. See [`target/40-jsr223-target.md`](target/40-jsr223-target.md), [`target/50-migration-plan.md`](target/50-migration-plan.md).
- **K1 cleanup chain** — Daemon REPL removal → `-Xrepl` removal → `cli-base/repl/*` removal → `legacyRepl*.kt` removal → `GenericReplCompiler` removal → K1 frontend bindings removal. Sequenced in [`target/50-migration-plan.md`](target/50-migration-plan.md).
- **`scripting-ide-{common,services}` deletion** — see [`target/50-migration-plan.md`](target/50-migration-plan.md) steps 9-10.
- **Classpath-discovery SPI decision** (KT-82551) — un-deprecate + document or design successor.
- **Compiler-side test cleanup** — drop K1 REPL test data; move K2 custom-script codegen tests to `plugins/scripting/scripting-tests`. See [`current/70-tests.md`](current/70-tests.md), [`target/50-migration-plan.md`](target/50-migration-plan.md) step 12.

---

## What NOT to Do

- Don't add K1 fallbacks. K1 is going.
- Don't extend the daemon REPL or revive `-Xrepl`.
- Don't add JSR-223-specific compiler paths. Bindings story belongs in [`target/40-jsr223-target.md`](target/40-jsr223-target.md).
- Don't move things out of `libraries/scripting/intellij` — it's public surface.
- Don't expand `legacy*.kt` / `obsolete*.kt` in `jvm-host`. They're slated for deletion.
- Don't leak compiler-internal EPs to users. The refinement DSL is the public customization surface.
- Don't modify generated `*Generated.java` test runners by hand — run `./gradlew generateTests`.
- Don't conflate scripts and REPL snippets at the FIR level — different shapes by design.

---

## Reference Documents

| Document | When to consult |
|---|---|
| [`ITERATION_RESULTS.md`](ITERATION_RESULTS.md) | **Append a dated entry after each iteration.** Most recent on top. Use the Entry Template at the top of the file. |
| [`current/00-overview.md`](current/00-overview.md) | Layer × module map. Pipeline diagram. Read first to orient. |
| [`current/10-compiler-representation.md`](current/10-compiler-representation.md) | **Read first for any compiler-side edit.** PSI/K1/FIR/IR per script vs snippet. Configurator EPs. Lowerings. Constants. |
| [`current/20-customization.md`](current/20-customization.md) | Refinement DSL + how it wires into FIR. Read before touching extension impls. |
| [`current/30-api-layer.md`](current/30-api-layer.md) | `libraries/scripting/*` catalog. K2 compilation core wrappers. |
| [`current/40-embedding-cli-daemon.md`](current/40-embedding-cli-daemon.md) | CLI flags, plugin autoload, daemon REPL inventory (all REMOVE). |
| [`current/41-embedding-build.md`](current/41-embedding-build.md) | Gradle subplugin + BTA discovery op. |
| [`current/50-script-definitions.md`](current/50-script-definitions.md) | Definition discovery + main-kts canonical example. |
| [`current/60-jsr223.md`](current/60-jsr223.md) | K2 engine state, bindings gap, hybrid parsing. |
| [`current/70-tests.md`](current/70-tests.md) | Per-module + compiler-side test inventory w/ disposition tags. Gradle commands. |
| [`current/90-legacy-inventory.md`](current/90-legacy-inventory.md) | **Authoritative**: every K1 / PSI / IDE-coupled / duplicated artifact with disposition (REMOVE / MIGRATE / KEEP-FOR-NOW / KEEP). |
| [`target/00-principles.md`](target/00-principles.md) | P1–P9 + P4a. Why we're doing this. |
| [`target/10-compiler-target.md`](target/10-compiler-target.md) | Keep / remove / refactor per compiler subsystem. |
| [`target/20-api-target.md`](target/20-api-target.md) | `libraries/scripting/*` post-cleanup shape. |
| [`target/30-embedding-target.md`](target/30-embedding-target.md) | CLI / daemon / Gradle / BTA post-cleanup shape. |
| [`target/40-jsr223-target.md`](target/40-jsr223-target.md) | Bindings options A–D + sketches; stateless remote-compilation design. |
| [`target/50-migration-plan.md`](target/50-migration-plan.md) | Ordered, independently-mergeable steps. Sequencing constraints. |
| [`target/90-open-questions.md`](target/90-open-questions.md) | Items still needing brainstorm. |
| Repo-wide: [`../../.ai/guidelines.md`](../../.ai/guidelines.md), [`../../compiler/AGENTS.md`](../../compiler/AGENTS.md), [`../../compiler/build-tools/AGENTS.md`](../../compiler/build-tools/AGENTS.md), [`../../docs/code_authoring_and_core_review.md`](../../docs/code_authoring_and_core_review.md), [`../../docs/fir/fir-basics.md`](../../docs/fir/fir-basics.md) | Project conventions, FIR basics, Build Tools API, commit guidelines. |

---

## Repo Conventions (selected)

- **Commits**: reference YouTrack issues — `KT-XXXXX` in subject, `^KT-XXXXX Fixed` in body to auto-close. Subject ≤ 72 chars, imperative. Explain WHAT + WHY + HOW. Tests committed alongside their code change. Non-functional changes (reformat, rename, refactor) in separate commits. `FIR:` prefix when changes are mostly in `compiler/fir/`.
- **Test data updates**: never run `-Pkotlin.test.update.test.data=true` without explicit user approval. After adding new test data, run `./gradlew generateTests`.
- **Bug-fix flow**: commit failing test first, then the fix in a second commit, so the reviewer sees the diagnostic delta.

---

*Last updated: 2026-05-16 (initial draft).*

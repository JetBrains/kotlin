# Stateless K2 REPL — BTA transport end-to-end smoke test (+ 2 packaging/compat fixes) — 2026-06-25

## Overview

Closed the **deferred BTA smoke test** follow-up for the stateless K2 REPL transport (Q5d). The
prior BTA iteration (`2026-05-28c`) landed `CompileReplSnippetOperation` (API + impl + wire codec)
but explicitly *deferred* an end-to-end test and only compiled the api/impl modules. Wiring a real
end-to-end test through `KotlinToolchains.loadImplementation` (the shaded `kotlin-build-tools-impl`
jar, where scripting-compiler classes are relocated) surfaced **two latent breakages** the
compile-only check never could:

1. a **build break** — the abstract `JvmPlatformToolchain.compileReplSnippetOperationBuilder(...)`
   was never implemented in the legacy `KotlinToolchainsV1Adapter` (compat shim), so
   `kotlin-build-tools-compat` failed to compile the moment the full op graph was demanded; and
2. a **runtime packaging gap** — the op could not compile *even `val x = 42`* because the impl
   shaded jar omitted `kotlin-script-runtime`, so the relocated
   `…internal.scripting.dependencies.DependenciesResolver$NoDependencies` (referenced when building
   a snippet's default `ScriptCompilationConfiguration`) was absent → `ClassNotFoundException`.

Both are exactly the class of defect an execution test exists to catch. With both fixed, the
transport now compiles a multi-snippet sequence end-to-end across the real BTA boundary.

## Workstream / Issue

Migration step 3 (Stateless remote REPL compilation prototype) — round 8. Q5d follow-up #1 (BTA-side
end-to-end smoke test). Continuation of the user-confirmed direction (approach sound; keep building
increments rather than redesign).

## Changes

- `compiler/build-tools/kotlin-build-tools-api-tests/src/testCompilerPlugins/kotlin/ReplSnippetCompilationTest.kt`
  — **new**. Drives `compileReplSnippetOperation` through `KotlinToolchains.loadImplementation` +
  `BuildSession.executeOperation` (in-process). Happy path compiles `val x = 42` → `val y = x + 1`
  → `x + y`, threading each produced `ByteArray` artifact into the next call as a prior, and asserts
  non-empty/distinct artifacts (cross-snippet resolution over the wire). A second test asserts an
  unresolved-symbol snippet currently surfaces failure as a thrown exception (documents the
  not-yet-structured failure surface). Mirrors `ScriptingTest`'s in-process pattern; stdlib is fed
  via `ADDITIONAL_CLASSPATH` from `currentKotlinStdlibLocation`.
- `compiler/build-tools/kotlin-build-tools-compat/.../KotlinToolchainsV1Adapter.kt` — **build-break
  fix**. Implemented `compileReplSnippetOperationBuilder(...)` in the anonymous `JvmPlatformToolchain`
  (the V1 fallback has no `CompilationService` equivalent — unlike discovery, which maps to
  `getCustomKotlinScriptFilenameExtensions`), throwing `UnsupportedOperationException`.
- `compiler/build-tools/kotlin-build-tools-api/.../wrappers/KotlinWrapperPre2_4_0.kt` —
  **correctness fix**. Added the parallel `compileReplSnippetOperationBuilder(...)` override
  (throws "available from Kotlin compiler version 2.4.0"), mirroring the existing
  `discoverScriptExtensionsOperationBuilder` override. Was a latent runtime `AbstractMethodError`
  for the op against a < 2.4.0 impl (hidden because the wrapper delegates `by base`).
- `compiler/build-tools/kotlin-build-tools-impl/build.gradle.kts` — **packaging fix**. Added
  `embedded(project(":kotlin-script-runtime")) { isTransitive = false }`. It is a transitive dep of
  `kotlin-scripting-{common,jvm}` (dropped by their `isTransitive = false`) and carries
  `kotlin.script.experimental.dependencies.DependenciesResolver` that the relocated compiler
  references at compile time.

No scripting-compiler production code (FIR/IR/backend, `K2ReplStatelessCompiler`, sidecar) was
touched — the iteration is additive on the build-tools transport edge only.

## Test Results

| Suite | Result | Notes |
|---|---|---|
| `…api-tests:testCompilerPlugins --tests *ReplSnippetCompilationTest*` | 4 tests / 0 fail / 2 skipped | new; happy path + error path green in-process (daemon variants skipped — op is in-process only) |
| `…api-tests:testCompilerPlugins` (full suite) | **80 / 0 / 19 skipped** | regression check for the new embedded jar — existing scripting discovery/compile tests unaffected |
| `:plugins:scripting:scripting-tests:test --tests *ReplStatelessDiagnosticsTestGenerated` | 24 / 0 | unchanged (no scripting production code touched) |
| `:plugins:scripting:scripting-tests:test --tests *ReplViaApiDiagnosticsTestGenerated` | 24 / 0 | unchanged |

JSR-223 suite not re-run: this iteration changed only `compiler/build-tools/*` + a new test;
`:kotlin-scripting-jsr223-test` does not depend on those modules, so the prior baseline
(17 pass / 3 pre-existing fail / 1 disabled) is unaffected.

## Files Modified

| File | Change |
|---|---|
| `…api-tests/…/testCompilerPlugins/…/ReplSnippetCompilationTest.kt` | new — end-to-end BTA smoke test |
| `…kotlin-build-tools-compat/…/KotlinToolchainsV1Adapter.kt` | implement missing op builder (throws Unsupported) — fixes compile break |
| `…kotlin-build-tools-api/…/wrappers/KotlinWrapperPre2_4_0.kt` | parallel pre-2.4.0 op override (throws Unsupported) |
| `…kotlin-build-tools-impl/build.gradle.kts` | embed `kotlin-script-runtime` into the shaded impl jar |

## Key Learnings

- **Compile-only validation hides both API-shape and packaging defects.** The BTA op was "landed"
  with green api/impl compiles, yet was unusable: one new abstract method silently un-implemented in
  the compat shim (latent compile break) + a missing embedded runtime jar (latent
  `ClassNotFoundException`). An end-to-end test through `loadImplementation` is the only check that
  exercises the relocated/shaded artifact graph the real consumer uses.
- **`isTransitive = false` on `embedded(...)` is a sharp edge.** Embedding the scripting jars without
  their transitive `kotlin-script-runtime` produced a jar that links at build time but throws at
  runtime once the relocated `DependenciesResolver` is touched. The relocated class *name* in the
  diagnostic (`org.jetbrains.kotlin.buildtools.internal.scripting.…`) is the tell that the gap is in
  the impl's own embedded set, not the caller's `ADDITIONAL_CLASSPATH`.
- **New abstract methods on `JvmPlatformToolchain` must be added in three places**: the real impl
  (`JvmPlatformToolchainImpl`), the version wrappers (`KotlinWrapperPre2_4_0` — throw "since 2.4.0"),
  and the V1 fallback (`KotlinToolchainsV1Adapter` — throw "not in v1 fallback"). The discovery op is
  the canonical precedent.

## Resources & Cost

n/a — Junie session, no JSONL. Expensive ops: 3 heavy `:kotlin-build-tools-api-tests:testCompilerPlugins`
runs (each rebuilds/assembles the shaded `kotlin-build-tools-impl` embeddable; ~40 s–1 m 8 s wall on a
warm daemon) + 1 cheap diagnostics reconfirm (~7 s). Loadout: "Stateless remote REPL design" row
(budget ~8k) — actual exploration was heavier than budget because the failure crossed into
`compiler/build-tools/*` packaging, not just scripting.

## Post-iteration checklist

- [x] Resources & Cost — recorded (n/a JSONL)
- [x] No migration-plan step fully landed (step 3 still in progress — BTA smoke test follow-up done)
- [x] Active Workstreams: step-3 row + index entry updated in `ITERATION_RESULTS.md`
- [ ] `current/90-legacy-inventory.md` — no artifact deleted
- [x] `target/90-open-questions.md` — Q5d follow-up #1 (smoke test) flipped to done; #2 (structured failure surface) still open
- [x] `target/50-migration-plan.md` — step-3 follow-up note updated
- [x] One-line index entry appended to `ITERATION_RESULTS.md`

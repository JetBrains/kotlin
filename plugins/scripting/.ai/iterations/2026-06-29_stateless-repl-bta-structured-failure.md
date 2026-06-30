# Stateless K2 REPL — BTA transport structured failure surface (Q5d follow-up #2) — 2026-06-29

## Overview

Closed the **structured failure surface** follow-up for the stateless K2 REPL transport (Q5d
follow-up #2). The BTA op `CompileReplSnippetOperation` previously modelled the result as raw
`BuildOperation<ByteArray>` and signalled a *compile* failure by **throwing a `RuntimeException`**
whose message concatenated the error diagnostics. That conflated an expected outcome (a snippet that
doesn't compile) with infrastructure errors, forced callers into `try/catch` + string-scraping, and
discarded diagnostic structure (severity, location).

This iteration replaces that with a typed result the caller pattern-matches on:

- `CompileReplSnippetOperation : BuildOperation<ReplSnippetCompilationResult>`
- `ReplSnippetCompilationResult` is a sealed interface:
  - `Success(artifact: ByteArray, diagnostics: List<ReplSnippetDiagnostic>)`
  - `Failure(diagnostics: List<ReplSnippetDiagnostic>)`
- `ReplSnippetDiagnostic(severity, message, location)` reuses the existing BTA
  `CompilerMessageRenderer.Severity` + `CompilerMessageRenderer.SourceLocation` (no parallel
  diagnostic vocabulary invented).

A plain compile failure is now a `Failure` — **no exception is thrown**. Exceptions are reserved for
genuine precondition/infra errors (unsupported `ExecutionPolicy`, an undecodable prior artifact).
As a bonus the previously-dead `COMPILER_MESSAGE_RENDERER` option is now wired: every diagnostic is
also rendered and streamed through the `KotlinLogger` at the matching level
(error/warn/info/debug) — mirroring `BaseCompilationOperationImpl`/`DiscoverScriptExtensionsOperationImpl`.

## Workstream / Issue

Migration step 3 (Stateless remote REPL compilation prototype) — round 9. Q5d follow-up #2 (BTA-side
structured failure surface). Continuation of the user-confirmed direction (approach sound; keep
building increments rather than redesign).

## Changes

- `compiler/build-tools/kotlin-build-tools-api/.../jvm/operations/ReplSnippetCompilationResult.kt`
  — **new**. Sealed `ReplSnippetCompilationResult` (`Success`/`Failure`) + `ReplSnippetDiagnostic`,
  all `@ExperimentalBuildToolsApi` (top-level types annotated; nested `Success`/`Failure` inherit
  per the `ProjectId` precedent).
- `compiler/build-tools/kotlin-build-tools-api/.../jvm/operations/CompileReplSnippetOperation.kt`
  — retyped `BuildOperation<ByteArray>` → `BuildOperation<ReplSnippetCompilationResult>`; refreshed
  the class KDoc ("Failure handling" section + wire-shape wording).
- `compiler/build-tools/kotlin-build-tools-api/.../jvm/JvmPlatformToolchain.kt` — updated the
  `compileReplSnippetOperationBuilder` KDoc to describe the structured result. Builder/convenience
  `inline fun` signatures are unchanged (they return the *operation*, not its result).
- `compiler/build-tools/kotlin-build-tools-impl/.../jvm/operations/CompileReplSnippetOperationImpl.kt`
  — `executeImpl` now returns `ReplSnippetCompilationResult`: maps every `ScriptDiagnostic` to a
  `ReplSnippetDiagnostic` (severity `FATAL`→`ERROR`; location from `SourceCode.Location`), streams
  them through the logger via `COMPILER_MESSAGE_RENDERER`, then returns `Success(encode(artifact),
  diagnostics)` / `Failure(diagnostics)`. The `throw RuntimeException(...)` on compile failure is
  gone; the `check(executionPolicy is InProcess)` precondition + codec decode still throw.
- `compiler/build-tools/kotlin-build-tools-api-tests/.../testCompilerPlugins/.../ReplSnippetCompilationTest.kt`
  — success path unwraps `Success.artifact` (via a `compileSnippetArtifact` helper asserting
  `Success`); the error-path test renamed to *"returns a structured Failure"* now asserts
  `assertInstanceOf(Failure)`, that ≥1 diagnostic is `ERROR`, and that the message references
  `noSuchSymbol` — no more `assertThrows`.
- `compiler/build-tools/kotlin-build-tools-api/api/kotlin-build-tools-api.api` — **regenerated**
  (`apiDump`). The dump was *stale*: it had never recorded the prior iteration's
  `compileReplSnippetOperationBuilder` / `compileReplSnippetOperation` / `CompileReplSnippetOperation`
  entries. The regen now adds those **and** the new `ReplSnippetCompilationResult{,$Success,$Failure}`
  + `ReplSnippetDiagnostic`. `apiCheck` passes.

No scripting-compiler production code (FIR/IR/backend, `K2ReplStatelessCompiler`, sidecar) was
touched — the iteration is additive on the build-tools transport edge only. The `kotlin-build-tools-compat`
+ `KotlinWrapperPre2_4_0` overrides are unaffected (the op's *builder* signature didn't change; only
the operation's result type-param did).

## Test Results

| Suite | Result | Notes |
|---|---|---|
| `…api-tests:testCompilerPlugins --tests *ReplSnippetCompilationTest*` | 4 / 0 fail / 2 skipped | happy path + new structured-`Failure` path green in-process (daemon variants skipped — op is in-process only) |
| `…api-tests:testCompilerPlugins` (full suite) | BUILD SUCCESSFUL (0 failures) | regression check for the retyped op + regenerated embeddable |
| `:compiler:build-tools:kotlin-build-tools-api:apiDump` + `apiCheck` | BUILD SUCCESSFUL | dump regenerated to include the REPL op + new result types; check green |
| `:plugins:scripting:scripting-tests:test --tests *ReplStatelessDiagnosticsTestGenerated --tests *ReplViaApiDiagnosticsTestGenerated` | UP-TO-DATE | inputs unchanged (no scripting production code touched) → unaffected; prior 24/24 + 24/24 holds |

JSR-223 suite not re-run: this iteration changed only `compiler/build-tools/*` + a test;
`:kotlin-scripting-jsr223-test` does not depend on those modules, so the prior baseline
(17 pass / 3 pre-existing fail / 1 disabled) is unaffected.

## Files Modified

| File | Change |
|---|---|
| `…api/…/jvm/operations/ReplSnippetCompilationResult.kt` | new — sealed result + `ReplSnippetDiagnostic` |
| `…api/…/jvm/operations/CompileReplSnippetOperation.kt` | retype op result + KDoc |
| `…api/…/jvm/JvmPlatformToolchain.kt` | builder KDoc refresh |
| `…impl/…/jvm/operations/CompileReplSnippetOperationImpl.kt` | structured result + diagnostic mapping + logger streaming |
| `…api-tests/…/testCompilerPlugins/…/ReplSnippetCompilationTest.kt` | assert structured `Success`/`Failure` |
| `…api/api/kotlin-build-tools-api.api` | regenerated ABI dump |

## Key Learnings

- **A `BuildOperation<R>` carries its result back through `executeOperation`; the seam to model a
  structured outcome is just `R`.** Builders/convenience `inline fun`s return the *operation*, so
  retyping `R` from `ByteArray` to a sealed result is localized to the interface + impl + the
  consuming test. No transport/wrapper signatures move.
- **The api dump tracks experimental ops.** `@ExperimentalBuildToolsApi` does *not* exclude a
  declaration from `kotlin-build-tools-api.api` (the dump already listed
  `discoverScriptExtensionsOperationBuilder`). The dump was nonetheless stale for the entire REPL op
  — a latent `apiCheck` failure left by the prior smoke-test iteration, now fixed alongside this
  change. Always run `:…:kotlin-build-tools-api:apiDump` after touching that module's public surface.
- **Reuse the existing diagnostic vocabulary.** `CompilerMessageRenderer.Severity` /
  `SourceLocation` already exist for BTA diagnostics; mapping `ScriptDiagnostic` onto them (rather
  than inventing a parallel severity/location type) keeps the surface coherent and lets the same
  `COMPILER_MESSAGE_RENDERER` option render REPL diagnostics like every other op.

## Resources & Cost

n/a — Junie session, no JSONL. Expensive ops: 1 targeted + 1 full `:…:kotlin-build-tools-api-tests:testCompilerPlugins`
run (rebuilds/assembles the shaded `kotlin-build-tools-impl` embeddable; ~17–19 s wall on a warm
daemon), 1 `apiDump` + 1 `apiCheck` (~5–8 s each), 1 diagnostics reconfirm (UP-TO-DATE, ~1 s).

## Post-iteration checklist

- [x] Resources & Cost — recorded (n/a JSONL)
- [x] No migration-plan step fully landed (step 3 still in progress — Q5d follow-up #2 done)
- [x] Active Workstreams: step-3 row + index entry updated in `ITERATION_RESULTS.md`
- [ ] `current/90-legacy-inventory.md` — no artifact deleted
- [x] `target/90-open-questions.md` — Q5d follow-up #2 (structured failure surface) flipped to done
- [x] `target/50-migration-plan.md` — step-3 follow-up note + Last-verified updated
- [x] One-line index entry appended to `ITERATION_RESULTS.md`

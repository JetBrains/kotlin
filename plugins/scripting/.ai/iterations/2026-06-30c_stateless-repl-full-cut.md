# Stateless K2 REPL — full cut of the standalone sidecar blob

- **Date**: 2026-06-30
- **Workstream**: Stateless remote REPL compilation (migration step 3) — Q5b (sidecar → in-metadata, **full cut**)
- **Round**: step 3 round 15
- **Loadout**: Stateless remote REPL design (`AGENT_INSTRUCTIONS.md` + `target/40-jsr223-target.md` remote section + `current/30-api-layer.md` + Q5 sub-table)

## Summary

Completed the **full cut** deferred by the previous round (`2026-06-30b`): the standalone `SnippetArtifactSidecar` blob is **removed** from the transport. A `SnippetArtifact` now carries only its class files plus a minimal out-of-band `SnippetArtifactHeader`; the entire reconstruction payload (declarations + source-level visibilities + file-level imports) is read **exclusively** from the snippet wrapper class's embedded `.kotlin_metadata` (the `ProtoBuf.CompilerPluginData` channel landed in `2026-06-30b`). No `.proto` change, no metadata-version bump.

`ArtifactBackedFirReplHistoryProvider` now uses the header for exactly two things — the `classId` lookup that *finds* the wrapper class, and the `isImplicit` history-provider flag — and sources everything else from the embedded sidecar, with **no standalone fallback**.

## The out-of-band header

`SnippetArtifactHeader` (hand-rolled `SnippetArtifactHeaderProtoCodec`, `headerVersion = 1`) keeps only the facts a consumer genuinely needs *before/without* deserializing a class's metadata:

- `snippetClassInternalName` + `packageFqName` — the wrapper class id (needed to locate the class before its metadata can be read; also used by `SnippetArtifactEvaluator` to load the class to run);
- `snippetName` — names the reconstructed `FirReplSnippet` + diagnostics;
- `stateObjectFqName` — validated by `K2ReplStatelessCompiler` *before* any class is compiled/loaded;
- `resultPropertyName` — the emitted `res<id>` field, read reflectively by the evaluator (post-codegen, no metadata deserialization);
- `isImplicit` — the Q10b flag, needed *before* `materialize()` runs.

The `SnippetArtifactCodec` JSON envelope was bumped to `artifactVersion = 2` (key `sidecar` → `header`).

## The one real gap and its fix

Removing the fallback surfaced a genuine gap the additive round had masked: **best-effort ERROR snippets never embedded their sidecar.** Plugin `IrGenerationExtension`s — including `ReplLoweringExtension`, which does both the snippet→class conversion *and* the `.kotlin_metadata` embed — are **skipped by `convertToIrAndActualizeForJvm` when the FIR reporter has errors**. On the stateless best-effort path the wrapper class is still emitted (via the fresh-env codegen after `elideErrorBodiedEvalFunctions`), but it carried no embedded sidecar; previously the standalone blob (built post-compile from the captured FIR, which fires even in best-effort) covered it.

Fix: run `ReplSnippetsToClassesLowering(irIn.pluginContext).lower(irIn.irModuleFragment)` explicitly in `K2ReplCompiler`'s best-effort branch, right after error-body elision. It is a safe no-op when the extension already ran (the `IrReplSnippet`s are then already removed from the module). Confirmed via tracing: the errored `Snippet_004_repl` now reports `attrPresent=true` on embed and `embedded sidecar=true` on read, and both previously-failing diagnostics files (`function_returns_anonymous_object`, `sealed_hierarchies`) go green.

## Changes

- `plugins/scripting/scripting-compiler/.../impl/SnippetArtifact.kt`: added `SnippetArtifactHeader` + `SnippetArtifactHeaderProtoCodec` (hand-rolled protobuf wire, stable field numbers, forward-compatible skip); repointed `SnippetArtifact.sidecar: ByteArray` → `header: ByteArray` (+ `equals`/`hashCode`); replaced `SnippetArtifactSidecar.toArtifact`/`decodeSidecar` with `SnippetArtifactHeader.toArtifact`/`decodeHeader`; `SnippetArtifactCodec` envelope `artifactVersion` 1→2, key `sidecar`→`header`. `SnippetArtifactSidecar` + its proto codec are retained — now the embedded-only reconstruction payload.
- `plugins/scripting/scripting-compiler/.../impl/SnippetArtifactEmission.kt`: `buildSnippetArtifactFromCompile` now builds a header via a new `buildReplHeaderFromFir(...)` (dropped the post-compile `buildSidecar`); `buildReplSidecarFromFir(...)` (the embedded write path assembly) is unchanged.
- `plugins/scripting/scripting-compiler/.../impl/K2ReplStatelessCompiler.kt`: decode/validate via `decodeHeader()`; `writeClassFiles(..., header)`; dropped the now-unused `session` field from `CapturedCompile` and the `session`/`historyIndex` args to the artifact builder.
- `plugins/scripting/scripting-compiler/.../impl/SnippetArtifactEvaluator.kt`: reads the class id + result-field name from the header (`decodeHeader()`).
- `plugins/scripting/scripting-compiler/.../services/ArtifactBackedFirReplHistoryProvider.kt`: `decodedHeaders`/`symbolToEmbeddedSidecar`; `materialize` locates the class via `header.toClassId()` and reads declarations/imports **only** from `readEmbeddedSidecar(...)` (no fallback); `findSidecarFor`→`findHeaderFor`; `getSnippetImports` reads the embedded sidecar.
- `plugins/scripting/scripting-compiler/.../impl/K2ReplCompiler.kt`: best-effort branch runs `ReplSnippetsToClassesLowering` explicitly so errored snippets embed like clean ones (the fix above).
- `plugins/scripting/scripting-tests/testFixtures/.../FirReplStatelessCompilerFacade.kt`: `latestDecodedSidecar()` → `latestDecodedHeader()`.
- `compiler/build-tools/kotlin-build-tools-api/.../operations/CompileReplSnippetOperation.kt`: wire-shape doc updated (`artifactVersion: 2`, `header`, payload rides `.kotlin_metadata`).
- Tests: `K2ReplStatelessCompilerTest` — replaced the standalone-strip proof with `testStatelessReplReconstructsDeclarationsFromEmbeddedMetadataAcrossWire` (round-trips the artifact through `SnippetArtifactCodec`, then proves `x` resolves — the header cannot carry declarations, so only the embedded metadata can supply them) + new `testHeaderProtoRoundtrip`; declaration/`historyIndex` assertions across `K2ReplStatelessCompilerTest` + `ScriptingCompilerPluginTest` retargeted to the header.

## Verification

- `:kotlin-scripting-compiler:test --tests *K2ReplStatelessCompilerTest*` — **7/7**, 0 failures.
- `:kotlin-scripting-compiler:test --tests *ScriptingCompilerPluginTest` — **7/7**, 0 failures (incl. the separate-process `kotlinc` subprocess test).
- `:plugins:scripting:scripting-tests:test` guards — `*ReplStatelessDiagnosticsTestGenerated` **24/24**, `*ReplViaApiDiagnosticsTestGenerated` **24/24**, 0 failures (both previously-failing files now pass with the best-effort embed fix).
- `:compiler:build-tools:kotlin-build-tools-api-tests:testCompilerPlugins --tests *ReplSnippetCompilationTest*` — **6 / 3 skipped / 0 failures** (daemon smoke transports the header + embedded metadata across the real shaded-impl/out-of-process boundary).

## Follow-ups (next increment)

- Public `StatelessReplCompiler` API (Q5e): promote the prototype `K2ReplStatelessCompiler` to a stable `libraries/scripting/common` surface (design pass — commits public API shape).
- Performance / caller-side caching (Q5c): the O(N²) FIR-reconstruction risk for long sessions is still unmeasured.
- Optional cleanup: `SnippetArtifactSidecar` still carries config-only fields (`historyIndex`, `isSynthetic`, `resultPropertyName`) that the embedded copy no longer needs; they could be trimmed from the embedded payload in a later tidy-up (kept now to avoid re-touching the codec + round-trip test in the same round).

## Resources & Cost

- n/a — Junie session, no JSONL. Loadout: Stateless remote REPL design (budget ~8k); over budget this round — the full cut looked mechanical but the removed fallback exposed the best-effort-error-snippet embed gap, which required tracing the `convertToIrAndActualizeForJvm` error-gating of `IrGenerationExtension`s to locate the fix.

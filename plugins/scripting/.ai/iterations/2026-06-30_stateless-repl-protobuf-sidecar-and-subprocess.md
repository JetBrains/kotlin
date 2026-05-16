# Stateless K2 REPL — protobuf sidecar cut + out-of-process-only transport + subprocess CLI test

- **Date**: 2026-06-30
- **Workstream**: Stateless remote REPL compilation (migration step 3) — Q5b (sidecar format), Q5d (transport)
- **Round**: step 3 round 13
- **Loadout**: Stateless remote REPL design (`AGENT_INSTRUCTIONS.md` + `target/40-jsr223-target.md` remote section + `current/30-api-layer.md` + Q5 sub-table)

## Summary

Three landed, fully-verified increments plus one ratified-but-deferred design:

1. **Out-of-process-only BTA op (assertion + coverage).** `CompileReplSnippetOperation` is an out-of-process operation; `CompileReplSnippetOperationImpl.executeImpl` now **rejects `ExecutionPolicy.InProcess`** with an explanatory `UnsupportedOperationException` (the in-process drive code + 8 now-unused imports were removed). All *out-of-process* execution paths are covered: core (`K2ReplStatelessCompilerTest`), regular-compile consumer (`ScriptingCompilerPluginTest`), daemon transport (`ReplSnippetCompilationTest` with-daemon), and now a genuine **separate-process `kotlinc`** path.

2. **Protobuf sidecar cut (Q5b).** Replaced the hand-rolled JSON `SnippetArtifactJsonCodec` with a self-contained, hand-rolled **protobuf-wire** codec `SnippetArtifactSidecarProtoCodec` (varints + length-delimited fields, stable field numbers, forward-compatible unknown-field skipping). No `.proto`-generation build wiring, no dependency on the relocated protobuf runtime's API. The same bytes are what the eventual `.kotlin_metadata` embedding will carry (see #4); added `REPL_SIDECAR_PLUGIN_ID` to name that contract.

3. **Separate-process CLI test (user-requested).** `ScriptingCompilerPluginTest.testReplSnippetCompilationViaKotlincSubprocess` forks a fresh JVM running `K2JVMCompiler` off the test classpath **twice** — snippet 1 (`val x = 42`) then snippet 2 (`x + 1`) with snippet 1 handed in as a prior — proving the stateless sequence resolves cross-snippet references across a real OS-process boundary via the regular compile path (no daemon, no in-process shortcut).

4. **`.kotlin_metadata` embedding (Q5b end target) — design ratified, implementation deferred.** Investigation (below) confirmed the user's "non-intrusive" intuition: Kotlin metadata already has a first-class generic channel, `ProtoBuf.CompilerPluginData { plugin_id; bytes data }`, so embedding needs **no `.proto` change and no metadata-version bump**. Write path was found to be larger than one safe step; deferred with a concrete plan.

## Context / investigation (the user's "dig the docs and code again" ask)

**Is the already-serialized `.kotlin_metadata` sufficient on its own?** No — but most of the current sidecar is redundant. The reconstruction read path (`ArtifactBackedFirReplHistoryProvider.materialize`) genuinely needs, beyond standard metadata: the `isReplSnippetDeclaration` markers (which members are user declarations — a FIR attribute that is *not* serialized), the file-level `imports`, and (for Q10b) `isImplicit`. The rest is already in metadata: `returnTypeSignature` is explicitly *not consumed* (comment: deserialized metadata already carries the type), member kinds/signatures are present, and `visibility` is likely recoverable (a code comment claims REPL members are recorded `public` in metadata — to verify when implementing). `resultPropertyName` is consumed only by the test `SnippetArtifactEvaluator`, not by reconstruction.

**Is there a standard, non-intrusive embedding mechanism?** Yes — `ProtoBuf.CompilerPluginData` (a `repeated` field already on `Class`/`Function`/`Property`/`Constructor`/`TypeAlias` in `core/metadata/src/metadata.proto`). Write: `IrGeneratedDeclarationsRegistrar.addCustomMetadataExtension(irDecl, pluginId, ByteArray)` → `FirElementSerializer` writes it. Read: the FIR deserializer populates `firDeclaration.compilerPluginMetadata: Map<String, ByteArray>` (`deserializeCompilerPluginMetadata` in `FirDeserializationUtils.kt`). Other consumers ignore unknown plugin ids; no version bump. Precedent: kotlinx.serialization's `SerializationPluginMetadataExtensions` (a standalone `.proto` extension) and the generic `addCustomMetadataExtension` used by IR plugins.

**Why the write path was deferred.** `Fir2IrReplSnippetConfiguratorExtensionImpl.prepareSnippet` runs *before* the wrapper IR class is built — `Fir2IrVisitor` calls `prepareSnippet` (line ~383) then sets `irSnippet.targetClass` via `visitRegularClass(replSnippet.snippetClass)` (line ~404). So no wrapper `IrClass` exists at prep time to attach metadata to. The IR lowering `ReplSnippetsToClassesLowering.finalizeReplSnippetClass` *does* have the wrapper class (`irSnippet.targetClass!!.owner`) and the `IrPluginContext` (→ `metadataDeclarationRegistrar`), but the residual data needs the FIR session + container-file imports, which aren't reachable from the IR lowering. Resolving this cleanly needs either a new post-class-build configurator hook, or assembling the bytes in `prepareSnippet` and registering them against a cached IR class header (`classifierStorage.getIrClass(firClass)` — flagged "should not be used on the first FIR2IR stage", so risky). That cross-cutting integration + full regression is more than one safe increment, so it was deferred rather than risk a broken build.

## Changes

- `plugins/scripting/scripting-compiler/.../impl/SnippetArtifact.kt`: JSON sidecar codec → `SnippetArtifactSidecarProtoCodec` (hand-rolled protobuf wire); `toArtifact`/`decodeSidecar` repointed; added `REPL_SIDECAR_PLUGIN_ID`; doc updates. `JsonParser` kept (still used by the `SnippetArtifactCodec` BTA envelope, which base64-wraps the now-protobuf sidecar blob unchanged).
- `plugins/scripting/scripting-compiler/tests/.../K2ReplStatelessCompilerTest.kt`: migrated the round-trip test to the proto codec (`testSidecarJsonRoundtrip` → `testSidecarProtoRoundtrip`).
- `compiler/build-tools/kotlin-build-tools-impl/.../jvm/operations/CompileReplSnippetOperationImpl.kt`: in-process branch now throws an explanatory `UnsupportedOperationException`; removed the in-process drive (`executeInProcess`, `reportDiagnostics`, `toReplSnippetDiagnostic`) + unused imports; KDoc reworked to "out-of-process only".
- `compiler/build-tools/kotlin-build-tools-api-tests/src/testCompilerPlugins/kotlin/ReplSnippetCompilationTest.kt`: success/error cases gated daemon-only (`assumeDaemon`); new `replSnippetCompilationRejectsInProcessExecution` asserting the in-process rejection.
- `plugins/scripting/scripting-compiler/tests/.../ScriptingCompilerPluginTest.kt`: new `testReplSnippetCompilationViaKotlincSubprocess` (+ `runCompilerSubprocess` helper).

## Verification

- `:kotlin-scripting-compiler:test --tests *K2ReplStatelessCompilerTest*` — **5/5**, 0 failures (incl. multi-snippet execution + both wire codecs).
- `:plugins:scripting:scripting-tests:test` guards — `*ReplStatelessDiagnosticsTestGenerated` **24/24**, `*ReplViaApiDiagnosticsTestGenerated` **24/24**, 0 failures.
- `:kotlin-scripting-compiler:test --tests *ScriptingCompilerPluginTest` — **7/7**, 0 failures (subprocess test runs two real forked compiles, ~8.4s).
- `:compiler:build-tools:kotlin-build-tools-api-tests:testCompilerPlugins --tests *ReplSnippetCompilationTest*` — **6 tests / 3 skipped / 0 failures** (daemon smoke + daemon error transport the protobuf sidecar across the real shaded-impl boundary; in-process rejection passes; in-process smoke/error + daemon-rejection correctly skipped).

## Follow-ups (next increment)

- **`.kotlin_metadata` embedding (write+read) via `CompilerPluginData`.** Read side: `ArtifactBackedFirReplHistoryProvider.materialize` prefers `classSymbol.fir.compilerPluginMetadata[REPL_SIDECAR_PLUGIN_ID]` (decode with `SnippetArtifactSidecarProtoCodec`), falling back to the standalone blob — additive, so the existing suite exercises the embedded path once the write side lands. Write side: add a post-class-build hook (or use a cached IR class header) gated to stateless mode so the stateful/golden path is untouched; assemble the residual sidecar (declarations + imports + stateObjectFqName + name) in `prepareSnippet` where FIR session/imports are available, register via `addCustomMetadataExtension`. Then collapse `SnippetArtifact` toward class-files-only (keep blob for the test evaluator / BTA until the read side is proven).
- Verify the "REPL members recorded as `public` in metadata" claim; if false, drop `visibility` from the sidecar (recover from deserialized metadata directly).

## Resources & Cost

- n/a — Junie session, no JSONL. Loadout: Stateless remote REPL design (budget ~8k); actual interaction larger than budget (multi-increment iteration spanning codec + transport + a separate-process test + a deep metadata-extension investigation).

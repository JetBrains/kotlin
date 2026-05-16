# Stateless K2 REPL — residual sidecar embedded in `.kotlin_metadata` via `CompilerPluginData`

- **Date**: 2026-06-30
- **Workstream**: Stateless remote REPL compilation (migration step 3) — Q5b (sidecar → in-metadata)
- **Round**: step 3 round 14
- **Loadout**: Stateless remote REPL design (`AGENT_INSTRUCTIONS.md` + `target/40-jsr223-target.md` remote section + `current/30-api-layer.md` + Q5 sub-table)

## Summary

Landed the **`.kotlin_metadata` embedding** that the previous round (`2026-06-30`) ratified-but-deferred. The frontend-derivable residual sidecar (declarations + visibilities + imports + state-object FQ name + name) is now serialized into the snippet wrapper class's `.kotlin_metadata` via the generic `ProtoBuf.CompilerPluginData` channel (keyed by `REPL_SIDECAR_PLUGIN_ID`), and the read path consumes it from a reconstructed prior snippet. **No `.proto` change, no metadata-version bump** — confirming the user's "non-intrusive" intuition end-to-end.

The change is **additive**: the standalone JSON-→-protobuf blob is kept (it is still required for the `classId` lookup that *finds* the wrapper class before its metadata can be read, and for the config-only flags). The embedded copy is preferred for the reconstruction fields, proven by a **falsifiable** test that strips those fields out of the standalone blob and shows cross-snippet resolution still works.

## How the previous round's deferral was resolved

The `2026-06-30` log deferred the write side because `Fir2IrReplSnippetConfiguratorExtensionImpl.prepareSnippet` runs *before* the wrapper `IrClass` is built, while the metadata registrar (`metadataDeclarationRegistrar`) is only reachable from the IR lowering (`ReplSnippetsToClassesLowering.finalizeReplSnippetClass`), which in turn lacks the FIR session + container-file imports.

Resolution: a tiny **IR-attribute bridge**. `prepareSnippet` (which *does* have FIR session + imports) assembles the residual sidecar and stashes its protobuf-wire bytes on a new `IrReplSnippet.replSidecarMetadataAttr` (`irAttribute(copyByDefault = false)`); `finalizeReplSnippetClass` (which *does* have the wrapper `IrClass` + `IrPluginContext`) reads the attribute and calls `addCustomMetadataExtension`. No new configurator EP, no risky use of the FIR2IR-stage-forbidden `classifierStorage.getIrClass`.

## Changes

- `plugins/scripting/scripting-compiler/.../irLowerings/ScriptConfigurationAttributes.kt`: new `IrReplSnippet.replSidecarMetadataAttr: ByteArray?` (the prep→lowering bridge; `copyByDefault = false`).
- `plugins/scripting/scripting-compiler/.../impl/SnippetArtifactEmission.kt`: extracted the frontend-derivable assembly into a shared `internal fun buildReplSidecarFromFir(firSnippet, session, hostConfiguration, historyIndex, resultPropertyName, isSynthetic, isImplicit)`; the post-compile `buildSidecar` now computes the config-only fields and delegates. The two producers therefore agree bit-for-bit on every field the read path consumes.
- `plugins/scripting/scripting-compiler/.../services/Fir2IrReplSnippetConfiguratorExtensionImpl.kt`: `prepareSnippet` calls `stashReplSidecarMetadataIfStateless(...)` — gated to stateless mode by `hostConfiguration[repl.firReplHistoryProvider] is ArtifactBackedFirReplHistoryProvider` (only the stateless orchestrator installs one), assembles via `buildReplSidecarFromFir` (config-only fields = best-effort defaults; not consumed from the embedded copy), and stashes the encoded bytes on the IR snippet.
- `plugins/scripting/scripting-compiler/.../irLowerings/ReplSnippetLowering.kt`: `finalizeReplSnippetClass` calls `embedReplSidecarMetadata(...)` — reads the attribute and, when present, `context.metadataDeclarationRegistrar.addCustomMetadataExtension(irSnippetClass, REPL_SIDECAR_PLUGIN_ID, bytes)`. No-op (hence golden-path-bit-identical) when the attribute is absent.
- `plugins/scripting/scripting-compiler/.../services/ArtifactBackedFirReplHistoryProvider.kt`: `materialize` now reads `standaloneSidecar` from the blob (for `toClassId()` + lookup), then prefers `readEmbeddedSidecar(classSymbol)` (decode `classSymbol.fir.compilerPluginMetadata[REPL_SIDECAR_PLUGIN_ID]`) for the reconstruction fields, falling back to the standalone blob. The config-only flags (`isImplicit` via `findSidecarFor`) still come from `decodedSidecars`.
- `plugins/scripting/scripting-compiler/tests/.../K2ReplStatelessCompilerTest.kt`: new **falsifiable** `testStatelessReplReconstructsFromEmbeddedMetadataWhenStandaloneStripped` — strips declarations+imports from the standalone blob (keeping class-id fields), then proves `x + 1` still resolves against the stripped prior, i.e. reconstruction sourced `x` from the embedded `.kotlin_metadata` copy.

## Verification

- `:kotlin-scripting-compiler:test --tests *K2ReplStatelessCompilerTest*` — **6/6**, 0 failures (incl. the new embedded-read proof + the multi-snippet execution test).
- `:kotlin-scripting-compiler:test --tests *ScriptingCompilerPluginTest` — **7/7**, 0 failures (incl. the separate-process `kotlinc` subprocess test, which now also exercises the embed→serialize→deserialize cycle across a real OS-process boundary).
- `:plugins:scripting:scripting-tests:test` guards — `*ReplStatelessDiagnosticsTestGenerated` **24/24**, `*ReplViaApiDiagnosticsTestGenerated` **24/24**, 0 failures.
- `:compiler:build-tools:kotlin-build-tools-api-tests:testCompilerPlugins --tests *ReplSnippetCompilationTest*` — **6 / 3 skipped / 0 failures** (daemon multi-snippet smoke transports the embedded metadata across the real shaded-impl/out-of-process boundary).

## Follow-ups (next increment)

- **Cut the standalone blob.** This round keeps it because (a) the `classId` lookup needs *some* index before the class metadata can be read, and (b) the config-only flags (`isImplicit`, the emitted result-field name) are not assembled on the embedded write path. To drop the blob: deliver the class-id list out-of-band (e.g. the BTA passes class-file internal names), and either thread the script compilation configuration into `prepareSnippet` or recover `isImplicit` from a serialized marker so the embedded copy becomes authoritative for *all* fields.
- Verify the "REPL members recorded as `public` in metadata" claim; if true, `visibility` can be dropped from the residual sidecar (recover from deserialized metadata) — currently retained because the read path restamps it to drive the `property_visibility` diagnostic.

## Resources & Cost

- n/a — Junie session, no JSONL. Loadout: Stateless remote REPL design (budget ~8k); actual within budget — single focused increment building directly on the prior round's ratified plan and `REPL_SIDECAR_PLUGIN_ID` scaffolding.

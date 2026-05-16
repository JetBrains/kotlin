# Stateless K2 REPL — diagnostics-corpus coverage + Q10b sidecar tagging — 2026-05-27 (round 2)

## Overview

Drove the stateless K2 REPL prototype (`K2ReplStatelessCompiler`) against the existing `testData/diagnostics/repl/*` corpus by adding an analogous abstract test class to `AbstractReplWithTestExtensionsDiagnosticsTest` — `AbstractReplStatelessDiagnosticsTest`. Each multi-snippet `.repl.kts` is now exercised end-to-end through the JSON-sidecar round-trip path: snippet N's frontend resolves against the *reconstructed* history (classfiles written to a per-call temp directory, decls re-tagged via `ArtifactBackedFirReplHistoryProvider`) rather than a live `FirSession` chain.

**19 of 23 generated tests pass on the first run.** The remaining 4 surface concrete, prototype-specific reconstruction gaps in the JSON sidecar — exactly what the previous iteration flagged as "scenarios still better tested on JSON" before promoting to protobuf-in-`.kotlin_metadata`. **Q10b** ("implicit-snippet tagging in `FirReplHistoryProvider` vs caller-side bookkeeping") settled in favour of the **history-provider tagging** direction: added an `isImplicit: Boolean` field to `SnippetArtifactSidecar` (sidecar version bumped 1→2) and surfaced it on the read side through `ArtifactBackedFirReplHistoryProvider.isImplicit(symbol)` / `findSidecarFor(symbol)`.

## Workstream / Issue

Migration plan step **3** continuation (Stateless remote REPL compilation prototype). Coverage round drives the Q5b "scenarios still better tested on JSON" list from the [2026-05-27 raw-prototype iteration](2026-05-27_stateless-repl-prototype.md). **Q10b** flipped to **in-design with prototype-locked direction**: tagging lives on `SnippetArtifactSidecar.isImplicit` and surfaces on `ArtifactBackedFirReplHistoryProvider`. The semantic distinction between `isImplicit` and the existing `isSynthetic` is documented inline.

## Changes

### Sidecar schema bump (1 → 2)

- `plugins/scripting/scripting-compiler/src/.../impl/SnippetArtifact.kt`
  - Added `isImplicit: Boolean = false` to `SnippetArtifactSidecar`. Kdoc explicitly distinguishes it from `isSynthetic`: `isSynthetic` records that the compilation configuration carried the compile-side `_isSyntheticSnippet` flag; `isImplicit` is the **history-provider** view — whether downstream consumers should expose this snippet as user-authored or implicitly emitted (e.g. a JSR-223 binding cell from `prependSyntheticSnippets`, or a refinement-handler-injected helper cell).
  - Bumped `SnippetArtifactSidecar.CURRENT_VERSION` from `1` to `2`. Old sidecars are rejected by the existing version-mismatch error — there is no v1 wire format in the wild because the prototype is purely additive and unshipped.
  - Extended `SnippetArtifactJsonCodec.encode` / `.decode` to round-trip the new field.
  - **Internal → public visibility drop** on `K2ReplStatelessCompiler`, `SnippetArtifact`, `SnippetArtifactSidecar`, `SnippetArtifactJsonCodec`, `toArtifact`, `decodeSidecar`. Reason: the new test fixture lives in `:plugins:scripting:scripting-tests` and the previous module-private `internal` modifier made cross-module references fail (matches the `K2ReplCompiler` precedent which is also non-`internal` for the same reason).

### Sidecar emission

- `plugins/scripting/scripting-compiler/src/.../impl/SnippetArtifactEmission.kt`
  - `buildSnippetArtifactFromCompile` now derives `isImplicit = isSynthetic` for the prototype (today the only known producer of implicit snippets is `prependSyntheticSnippets`, which sets the compile-side `_isSyntheticSnippet` flag). The fields stay independent on the wire so future producers (refinement-handler-injected helper cells that do not set the compile-side flag) can mark snippets implicit without touching the synthetic flag.

### History-provider read path (Q10b)

- `plugins/scripting/scripting-compiler/src/.../services/ArtifactBackedFirReplHistoryProvider.kt`
  - New public surface on the provider:
    - `implicitFlags: List<Boolean>` — index-aligned with `priorSnippets`, suitable for walking the history with positional knowledge.
    - `findSidecarFor(symbol: FirReplSnippetSymbol): SnippetArtifactSidecar?` — O(N) lookup that matches the reconstructed `FirReplSnippetSymbol` to its sidecar by wrapper-class short name. Cheap for the prototype because `priorSnippets` is bounded by session length per call.
    - `isImplicit(symbol: FirReplSnippetSymbol): Boolean` — read-side answer to Q10b: "is this prior snippet implicit per the history provider?"

### Stateless compiler — `parentDisposable` caller-owned lifecycle

- `plugins/scripting/scripting-compiler/src/.../impl/K2ReplStatelessCompiler.kt`
  - Added `parentDisposable: Disposable? = null` parameter to `compile(...)`. When supplied, the call attaches its per-call environment to the caller's disposable and skips `Disposer.dispose` on both success and failure paths. Out-of-process callers (production) keep the previous behaviour by passing `null`.
  - **Rationale**: when running inside a hosted JUnit test fixture (which already owns project state via `testRootDisposable`), the previous per-call `Disposer.dispose` call hit `Write access is allowed inside write-action only (see Application.runWriteAction())` during cleanup. Threading a caller-owned disposable removes the conflict cleanly without changing the out-of-process contract.

### New test infrastructure

- `plugins/scripting/scripting-tests/testFixtures/.../FirReplStatelessCompilerFacade.kt` — **new**. Mirrors `FirReplCompilerFacade` (stateful via-API path) but:
  - holds a single `K2ReplStatelessCompiler` instance and an `accumulatedArtifacts: MutableList<SnippetArtifact>` across snippets in one test file;
  - per snippet, calls `statelessCompiler.compile(priorSnippets = priorsSnapshot, snippet = snippetSource, scriptCompilationConfiguration = baseConfig, parentDisposable = testRootDisposable)`;
  - appends the produced artifact to the accumulator on `Success` only;
  - repackages the result as `ReplCompilationArtifact(snippetSource, …)` so the existing `ReplCompilerDiagnosticsHandler` consumes it unchanged.
- `plugins/scripting/scripting-tests/testFixtures/.../AbstractReplTestBaseClasses.kt` — added `AbstractReplStatelessDiagnosticsTest` analogous to `AbstractReplViaApiDiagnosticsTest`, but driving the new facade.
- `plugins/scripting/scripting-tests/testFixtures/.../TestGenerator.kt` — registered the new abstract class against `testData/diagnostics/repl` with the same exclusion pattern as the other diagnostics runners.

### Generated tests

- `plugins/scripting/scripting-tests/tests-gen/.../ReplStatelessDiagnosticsTestGenerated.java` — **new** (via `./gradlew generateTests`). 23 generated test methods covering every `.repl.kts` in the corpus (matching the via-API class for parity).

### Roundtrip-test extension

- `plugins/scripting/scripting-compiler/tests/.../K2ReplStatelessCompilerTest.kt`
  - Extended `testSidecarJsonRoundtrip` to cover the new `isImplicit` field in all four polarity combinations (synthetic × implicit), proving the two flags round-trip independently — i.e. the Q10b read-side semantic is decoupled from the compile-side `_isSyntheticSnippet`.

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:plugins:scripting:scripting-tests:test` `*ReplStatelessDiagnosticsTestGenerated*` | n/a (new) | **19 PASS / 4 FAIL** of 23 generated tests | All 4 failures are `Actual data differs from file content: …` against the existing `.repl.kts` expected files. Via-API equivalents of the same 4 fixtures pass, so the divergences are stateless-prototype specific. |
| `:kotlin-scripting-compiler:test` `*K2ReplStatelessCompilerTest*` | 3 PASS / 0 FAIL | **3 PASS / 0 FAIL** | Roundtrip now also exercises `isImplicit` polarity coverage. |
| `:plugins:scripting:scripting-tests:test` `*ReplViaApiDiagnosticsTestGenerated*` | green | green | Regression guard: stateful via-API path unchanged. |
| `:plugins:scripting:scripting-tests:test` `*ReplWithTestExtensionsDiagnosticsTestGenerated*` | green | green | Regression guard: test-frontend-facade path unchanged. |

## Failing scenarios — concrete Q5c / Q5b / Q10b inputs

These are the 4 fixtures that diverge on the stateless path. Each is a real reconstruction gap that the prototype was supposed to surface; promoting to protobuf-in-`.kotlin_metadata` while any of them remain open would lock the schema before it is correct.

### 1. `import_visible_in_next_snippet.repl.kts` — file-level imports from history

```kts
// SNIPPET
import kotlin.random.Random
Random.nextInt(10)
// SNIPPET
Random.nextInt(20)   // expected: no diagnostic — `Random` is in scope via snippet-1 import
```

Stateful path: `FirReplHistoryProviderImpl` walks prior `FirReplSnippet`s and `FirReplSnippetResolveExtensionImpl.getImportsFromHistory` collects their file-level imports.

Stateless path: the sidecar **does** carry the prior snippet's imports (`ImportEntry` list), and `materialize()` reconstructs the `FirReplSnippetSymbol`s. But the resolve extension reads `firProvider.getFirReplSnippetContainerFile(symbol)` on the prior snippet's symbol — and our `ReconstructedFirReplSnippet` stub does **not** register a container file. The imports list is on the sidecar but never reaches the resolution scope.

**Gap**: `ArtifactBackedFirReplHistoryProvider.materialize()` must synthesise a minimal `FirFile` (or hook into `firProvider`) carrying the sidecar's `imports` so `getImportsFromHistory` finds them. Field set is *already correct*; the wiring on the read side is missing. Cheap follow-up — keep on JSON until fixed.

### 2. `sealed_hierarchies.repl.kts` — cross-snippet sealed hierarchies

```kts
// SNIPPET — sealed declaration in snippet 1
sealed interface BaseObjIface
object IObj1 : BaseObjIface { ... }
// SNIPPET
sealed class BaseObjClass(val y: Int)
// SNIPPET
val resi1 = IObj1.x ; val res2 = Obj2.y    // expected OK
// SNIPPET
object IObj3 : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>BaseObjIface<!> { ... }
object Obj3 : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>BaseObjClass<!>(13)
```

The stateful path emits `SEALED_INHERITOR_IN_DIFFERENT_MODULE` because each snippet is a separate module (REPL invariant), but the checker has special handling for snippets sharing the same chain.

Stateless path: prior snippets are loaded as a **library dependency** (classfiles in a temp dir on the classpath), so they belong to an unrelated `FirModuleData` rather than the per-snippet REPL module-data chain. The sealed-checker either fires for the wrong reason or doesn't fire at all because the inheritance is now plainly cross-module (library vs source) rather than cross-snippet (sibling source modules).

**Gap**: the stateless reconstruction needs to claim prior-snippet declarations as "library-but-treat-as-REPL-history" so the FIR checker can apply REPL-aware semantics. Either annotate the rebuilt `FirReplSnippetSymbol`s with their own module-data lineage, or expand `ReplModuleDataProvider` to recognise artifact-backed snippets as REPL siblings rather than plain library dependencies. Sidecar likely needs no new field — the issue is module-data wiring on the read side.

### 3. `function_returns_anonymous_object.repl.kts` — anonymous-object return types

```kts
// SNIPPET
fun foo() = object { val v = "OK" }
interface IV { val v: String }
fun bar() = object: IV { override val v = "OK" }
fun baz(): IV = object: IV { override val v = "OK" }
foo().<!UNRESOLVED_REFERENCE!>v<!>   // expected: UNRESOLVED — anonymous return type leaks
bar().v
baz().v
// SNIPPET
foo().<!UNRESOLVED_REFERENCE!>v<!>   // expected: UNRESOLVED — same diagnostic from prior snippet
bar().v
baz().v
```

The second snippet's `foo().v` must reproduce `UNRESOLVED_REFERENCE` (the anonymous-object type cannot be referred to outside its declaring scope). On the stateful path this works because `foo`'s return type is the *anonymous* `FirRegularClass` retained on the live FIR chain.

Stateless path: the sidecar's `MemberRef.Kind.FUNCTION` entry for `foo` has `descriptor = null` (the prototype emits `null` for descriptors). On reconstruction the function's return type comes from the JVM classfile — i.e. the anonymous object is now a deserialized class with a *resolvable* internal name. So `foo().v` resolves where it should not, **or** it produces a *different* shape of error (member not found vs class not visible).

**Gap (Q5b candidate)**: the sidecar needs the original (FIR) return-type signature for `MemberRef.Kind.FUNCTION` so the resolver can preserve "this type is anonymous; do not refer to it across snippets". This is exactly the kind of field that should be added on JSON before locking the protobuf schema. Hold on JSON.

### 4. `property_visibility.repl.kts` — `private` declarations across snippets

```kts
// SNIPPET
private val x = 1
println(x)
// SNIPPET
println(<!INVISIBLE_REFERENCE!>x<!>)   // expected: INVISIBLE_REFERENCE (private in another snippet)
```

The stateful path emits `INVISIBLE_REFERENCE` (the property exists but its visibility excludes the call site). The stateless path almost certainly emits a different diagnostic (`UNRESOLVED_REFERENCE`) — because `private val x` on the wrapper class becomes a Kotlin-private member that the deserialized symbol provider does not surface at all to symbols outside the original module.

**Gap (Q5b candidate)**: the sidecar's `MemberRef` does not carry the property's *visibility*. `INVISIBLE_REFERENCE` requires the resolver to **see** the symbol *and then reject it on visibility grounds*. Adding `visibility: kotlin.reflect.KVisibility` (or its FIR equivalent) to `MemberRef` lets `ArtifactBackedFirReplHistoryProvider.materialize()` retain private decls at the sidecar level — even though they would be invisible from outside the class — and tag them so the resolve extension surfaces the symbol with REPL-private visibility. Hold on JSON until added.

## Q10b — settled

Per [`target/90-open-questions.md`](../target/90-open-questions.md), Q10b ("Implicit-snippet tagging in `FirReplHistoryProvider`: needs an EP "implicit" tag, or caller-side bookkeeping?") is flipped to **in-design — sidecar-tag direction locked**. The chosen shape:

- **Storage**: `SnippetArtifactSidecar.isImplicit: Boolean` (sidecar version 2). Boolean, not enum — defer richer `originKind` until a concrete second producer beyond `prependSyntheticSnippets` arrives (option deferred per user 2026-05-27 decision).
- **Read path**: `ArtifactBackedFirReplHistoryProvider.isImplicit(symbol)` + `findSidecarFor(symbol)` + `implicitFlags` (positional). Consumers of `FirReplHistoryProvider.getSnippets()` can query implicit-ness without touching the symbol's contents.
- **Decoupling from `isSynthetic`**: the two flags round-trip independently in `testSidecarJsonRoundtrip`. Today's compiler always emits `isImplicit == isSynthetic`, but the wire format permits the next iteration to set `isImplicit = true` for refinement-handler helper cells without touching `_isSyntheticSnippet`.

Closing this question outright is deferred until a concrete consumer (Option D's `prependSyntheticSnippets` integration or a JSR-223 binding-cell test) exercises the read path. The schema is committed.

## Files Modified

| File | Change |
|---|---|
| `plugins/scripting/scripting-compiler/src/.../impl/SnippetArtifact.kt` | Added `isImplicit` field; bumped `CURRENT_VERSION` 1→2; extended codec; dropped `internal` from public surface (`SnippetArtifact`, `SnippetArtifactSidecar`, `SnippetArtifactJsonCodec`, `toArtifact`, `decodeSidecar`). |
| `plugins/scripting/scripting-compiler/src/.../impl/SnippetArtifactEmission.kt` | Threaded `isImplicit = isSynthetic` on the emit path with rationale comment. |
| `plugins/scripting/scripting-compiler/src/.../services/ArtifactBackedFirReplHistoryProvider.kt` | Added `implicitFlags`, `findSidecarFor`, `isImplicit(symbol)` read-side accessors for Q10b. |
| `plugins/scripting/scripting-compiler/src/.../impl/K2ReplStatelessCompiler.kt` | Added `parentDisposable: Disposable? = null` parameter; new branch skips `Disposer.dispose` on success and failure when caller owns lifecycle. Dropped `internal` on the class. |
| `plugins/scripting/scripting-compiler/tests/.../K2ReplStatelessCompilerTest.kt` | Extended `testSidecarJsonRoundtrip` to cover all four polarity combinations of `(isSynthetic, isImplicit)`. |
| `plugins/scripting/scripting-tests/testFixtures/.../FirReplStatelessCompilerFacade.kt` | **new** — stateless mirror of `FirReplCompilerFacade`. |
| `plugins/scripting/scripting-tests/testFixtures/.../AbstractReplTestBaseClasses.kt` | Added `AbstractReplStatelessDiagnosticsTest`. |
| `plugins/scripting/scripting-tests/testFixtures/.../TestGenerator.kt` | Registered the new abstract class against `testData/diagnostics/repl`. |
| `plugins/scripting/scripting-tests/tests-gen/.../ReplStatelessDiagnosticsTestGenerated.java` | **new** — generated 23-test runner. |

## Key Learnings

- **The stateless prototype already covers ~83 % of the diagnostics corpus** (19/23 fixtures, including `separate_snippets_use_before_define` which exercises mixed forward/backward references across 6 snippets). The corpus is a *strong* coverage signal that the JSON sidecar's core field set (member refs + imports + state-object FQ) is correct for the common cross-snippet cases.
- **Each remaining failure is a different, named gap** rather than a single bug — supports the previous iteration's claim that the field set is unstable and protobuf promotion would lock the schema prematurely. The failures cluster neatly:
  - **Read-side wiring**, no sidecar change needed (`import_visible_in_next_snippet`, `sealed_hierarchies`) — fix on JSON.
  - **Missing sidecar field**, schema change required (`function_returns_anonymous_object` → function return-type signature; `property_visibility` → declaration visibility) — exactly the kind of additions that would be expensive to roll out post-protobuf. Keep on JSON until added.
- **Q10b read path is now a verified accessor surface**, not just a wire-format field. The provider's `isImplicit(symbol)` exposes the flag at the same call sites that `getSnippets()` returns; consumers can branch on implicit-ness without re-decoding the sidecar.
- **`parentDisposable` is the right boundary** for in-process callers (tests *and* future in-process JSR-223 hosts). The same change makes the prototype embeddable in any host that owns its project lifecycle, not just out-of-process callers.
- **`internal` modifiers on prototype classes were too tight** for the multi-module test layout (`scripting-tests` is a separate Gradle module). Dropping them matches the precedent of `K2ReplCompiler` (also non-`internal`) and removes friction for future test-side experimentation. The "internal and prototype-only" doc statement is retained in Kdoc, just not enforced by the visibility modifier.
- **Sidecar version 2** is the *first* live bump of the version field. The existing version-mismatch error path already covers rejection of older sidecars; the prototype is purely additive and unshipped, so no migration scaffolding was needed.

## Resources & Cost

n/a — Junie session, no JSONL. See [`JUNIE_NOTES.md`](../JUNIE_NOTES.md) §Iteration close for substitute metrics convention.

| Metric | Value |
|---|---|
| Sessions aggregated | Single Junie session (continuation of 2026-05-27 prototype) |
| Time span | One iteration |
| Subagent calls (total) | 0 |
| Gradle wall-time | ~3 min total — fixtures compile, generateTests, test runs for stateless + via-API + with-test-extensions, plus regression unit-test run |
| Tool calls — bash | ~12 |
| Tool calls — search/open | ~25 |
| Tool calls — search_replace/multi_edit/create | ~10 |
| Files touched (production) | 4 (`SnippetArtifact.kt`, `SnippetArtifactEmission.kt`, `ArtifactBackedFirReplHistoryProvider.kt`, `K2ReplStatelessCompiler.kt`) |
| Files touched (tests + fixtures) | 5 (`K2ReplStatelessCompilerTest.kt`, `FirReplStatelessCompilerFacade.kt` *new*, `AbstractReplTestBaseClasses.kt`, `TestGenerator.kt`, `ReplStatelessDiagnosticsTestGenerated.java` *generated*) |
| Test suites run + pass/fail | new `ReplStatelessDiagnosticsTestGenerated` 19P/4F; `K2ReplStatelessCompilerTest` 3P/0F; via-API + with-test-extensions diagnostics: green; targeted 4-failure subset confirmed via-API pass. |
| Core-docs sufficiency | Yes — `AGENT_INSTRUCTIONS.md` + previous iteration + the recipient files (`FirReplCompilerFacade`, `AbstractReplTestBaseClasses`) were sufficient; no extra docs needed beyond targeted searches. |

### Loadout-vs-actual

- Loadout matrix row used: **Migration-step execution** (medium budget tier) — same as previous iteration.
- Actual model: claude-opus-4-7 (Junie session-fixed; no per-task switch).
- Budget hit / over / under: hit — additive prototype, no `:dist` rebuild, no broad subsystem touches. Two retry cycles on the test runs (one for the threading fix, one for the `asDiagnostics` import) cost an extra minute each but were caught by the Gradle-tee log and resolved in single edits.
- Subagent dispatch followed: N/A — no `cavecrew-*` under Junie. Did the read-only investigation sweep first (per JUNIE_NOTES.md §Subagent roles), then opened edits.
- No over-budget intervention needed.

## Post-iteration checklist

(See `AGENT_INSTRUCTIONS.md` "Post-iteration checklist" — confirm each.)

- [x] Resources & Cost section populated (Junie substitute metrics, Loadout-vs-actual filled).
- [x] Migration-plan step strike-through — step 3 status updated (`target/50-migration-plan.md` reflects the diagnostics-coverage round).
- [x] `target/90-open-questions.md` — Q10b flipped to **in-design, sidecar-tag direction locked**, linking this iteration; Q5b's "scenarios still better tested on JSON" list now backed by concrete fixture-by-fixture evidence.
- [x] Active Workstreams updated — `ITERATION_RESULTS.md` workstream-state row for "Stateless remote REPL compilation prototype" updated with the diagnostics-coverage milestone.
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.
- [ ] `current/30-api-layer.md` — no public-surface change (visibility drops are still on the prototype-only classes; `libraries/scripting/common` untouched).
- [ ] `current/70-tests.md` — new generated test class lives under `:plugins:scripting:scripting-tests:test` and is documented inline in this iteration entry; if the JSR-223 / scripting-tests matrix in `current/70-tests.md` ever grows a per-runner row, add `ReplStatelessDiagnosticsTestGenerated` then.

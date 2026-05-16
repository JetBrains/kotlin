# 2026-05-27 — Stateless REPL: sidecar v3 (visibility + returnTypeSignature)

## Goal

User request: **"add `visibility` and `returnTypeSignature` fields"** on `SnippetArtifactSidecar.MemberRef`.

These are the two schema-shaped gaps that the previous iteration
(`2026-05-27_stateless-repl-diagnostics-coverage.md`) flagged as the blocker to a JSON→protobuf
promotion:

- `property_visibility` — the prototype cannot encode that `private val x` in snippet *k* must be
  invisible to snippet *n>k*, so the expected `INVISIBLE_REFERENCE` is not emitted.
- `function_returns_anonymous_object` — `MemberRef.Kind.FUNCTION` carries only `(name, kind)`, so a
  function whose return type is an anonymous declaration in the same snippet has no signature
  that snippet N+1's resolver can re-anchor onto.

This iteration **does not aim to fix either diagnostic** — only to commit the wire-format extension
needed to fix them.

## Changes

### Schema — `SnippetArtifactSidecar` v2 → v3

`MemberRef` gained two fields, defaulted so older test fixtures still construct:

```kotlin
data class MemberRef(
    val kind: Kind,
    val name: String,
    val descriptor: String?,
    val visibility: Visibility = Visibility.UNKNOWN,
    val returnTypeSignature: String? = null,
)

enum class Visibility { PUBLIC, INTERNAL, PROTECTED, PRIVATE, UNKNOWN }
```

`CURRENT_VERSION` bumped 2 → 3; the KDoc-encoded version table is the source of truth.

### Codec — `SnippetArtifactJsonCodec`

`encode` writes two extra JSON fields per `MemberRef` (`"visibility"`, `"returnTypeSignature"`);
`decode` reads them. `visibility` is required on the wire (mirrors the enum-cast contract used for
`MemberRef.Kind`); `returnTypeSignature` is nullable.

### Producer — `SnippetArtifactEmission.buildSnippetSidecar`

- `visibility` is read from `FirMemberDeclaration.status.visibility` and projected onto the small
  enum via a private `toMemberRefVisibility()` helper. The four well-known Kotlin visibilities
  (Public / Internal / Protected / Private / PrivateToThis) map directly; everything else maps to
  `UNKNOWN`. The consumer defaults `UNKNOWN` to PUBLIC (safe default — extra leakage is preferable
  to dropping real declarations).
- `returnTypeSignature` is `coneTypeOrNull?.toString()` for PROPERTY / FUNCTION declarations and
  `null` for CLASS / TYPEALIAS (the type *is* the declaration). The render uses Cone's default
  `toString`; structural type encoding is a protobuf-cut concern.

### Consumer — `ArtifactBackedFirReplHistoryProvider.materialize`

Records the fields but does **not** branch on them yet. A behavioural branch was tried (filter out
PRIVATE/PROTECTED from the `isReplSnippetDeclaration` re-tag), but it broke the expected
`INVISIBLE_REFERENCE` golden — the resolver needs to *find* the symbol to then mark it invisible,
not have it filtered out. The proper fix is to either (a) check that the deserialised visibility
survives the resolver path that consumes prior REPL snippets, or (b) introduce a REPL-aware
visibility check.

### Test — `K2ReplStatelessCompilerTest.testSidecarJsonRoundtrip`

The roundtrip test now constructs a 5-element `replSnippetDeclarations` list covering:

- PROPERTY · PUBLIC · `"kotlin.Int"`
- FUNCTION · INTERNAL · `"kotlin.Unit"`
- CLASS · PROTECTED · `null`
- TYPEALIAS · PRIVATE · `null`
- PROPERTY · UNKNOWN · `null`  (pre-v3 producer compatibility)

The two new fields round-trip identically through `encode`/`decode`.

## Verification

- `:kotlin-scripting-compiler:test --tests "*K2ReplStatelessCompilerTest*"` — green (codec + the two
  prior compile-driven tests).
- `:plugins:scripting:scripting-tests:test --tests "*ReplStatelessDiagnosticsTestGenerated*"` —
  **19 / 23**, same 4 failures as before (`function_returns_anonymous_object`,
  `import_visible_in_next_snippet`, `property_visibility`, `sealed_hierarchies`). The schema fields
  are present in the produced sidecars; the failures persist because the *consumer* doesn't yet
  branch on them and the materialised FIR's existing visibility/return-type information isn't
  surfaced to the resolver of snippet N+1.
- `:plugins:scripting:scripting-tests:test --tests "*ReplViaApiDiagnosticsTestGenerated*"` —
  **23 / 23**.
- `:plugins:scripting:scripting-tests:test --tests "*ReplWithTestExtensionsDiagnosticsTestGenerated*"` —
  **23 / 23**.

No regression on the stateful path; no regression on the previous-iteration baseline; the new
fields are wire-format-stable.

## Remaining gaps (now read-side wiring only)

After this iteration **none of the four remaining failures is schema-shaped** — every fix is in
the materialise / resolve / module-data path:

| Test | What's missing |
|------|----------------|
| `property_visibility` | Materialise/resolve must honour the deserialised declaration's visibility (or the sidecar's `visibility` field) on cross-snippet references. |
| `function_returns_anonymous_object` | Materialise must surface the function's return type (already on the wire as `returnTypeSignature`; the protobuf-bound version will carry a structured type descriptor). |
| `import_visible_in_next_snippet` | `materialize()` must attach a synthetic `FirFile` carrying the sidecar's `ImportEntry` list. |
| `sealed_hierarchies` | `ReplModuleDataProvider` must treat artifact-backed snippets as REPL siblings, not library deps. |

The "sidecar field set demonstrably stable" gate (the last open precondition for the protobuf cut)
is now within striking distance: once these four behavioural fixes land and the suite goes
23 / 23 without further schema changes, the field set has earned the right to be committed to
`.kotlin_metadata`.

## Q5e / Q10b status

- Q10b — closed in the previous iteration (`isImplicit` round-trips, accessor on the provider).
- Q5e — still untouched. Public-API sketch remains a follow-up.

## Recommended next iteration

1. Surface the deserialised visibility / return type to the cross-snippet resolver so
   `property_visibility` + `function_returns_anonymous_object` flip green **without** further
   schema changes. (If the resolver insists on the sidecar fields rather than the deserialised
   `.kotlin_metadata`, switch over — the wire format is already there.)
2. Attach a synthetic `FirFile` in `materialize()` carrying `ImportEntry`s → fixes
   `import_visible_in_next_snippet`.
3. Wire `ReplModuleDataProvider` to treat artifact-backed snippets as siblings → fixes
   `sealed_hierarchies`.
4. Re-run the stateless diag suite; target 23 / 23.
5. Long-history reproducer (≥20 priors, cross-snippet class + shadowing) — answers Q5c.
6. BTA `CompileReplSnippetOperation` transport — answers Q5d, surfaces envelope/framing
   requirements before the protobuf cut.
7. Q5e public-API sketch.
8. Cut the protobuf schema.

# JSR-223 K2 bindings — JVM-safe binding-name encoding (Q14) — 2026-07-01

## Overview

Prototyped the K2 replacement for the K1 `\`-escape binding-name scheme (Q14): binding names that aren't plain Kotlin identifiers are now exposed as typed properties whose JVM-unsafe / non-ASCII characters are reversibly encoded into `__<mnemonic>__` markers (`a.b` → `a__dot__b`, `c:d` → `c__colon__d`, `☺` → `__u263a__`). The encoded form is a plain identifier that passes `FirJvmNamesChecker` and needs no backticks. `testEvalWithContextNamesWithSymbols` un-`@Disabled` and now PASSes, asserting the new marker spellings rather than the K1 backslash table.

## Workstream / Issue

JSR-223 K2 bindings (Option D — synthetic-snippets DSL callback), migration-plan step 1. Resolves `target/90-open-questions.md` Q14; marks `current/80-known-gotchas.md` G8 RESOLVED (prototype).

## Root cause(s)

Two independent problems, both in `libraries/scripting/jvm-host/src/kotlin/script/experimental/jvmhost/jsr223/propertiesFromContext.kt`:

1. **K1 escaping is unusable on K2.** K1's `toValidJvmIdentifier` mangled `.` → `\,`, `:` → `\!`, etc. (John Rose's "symbolic freedom in the VM" table), which relies on `\` as the escape prefix. On K2 `FirJvmNamesChecker.INVALID_CHARS` = `. ; [ ] / < > : \` — `\` is itself illegal, so every escaped name trips `INVALID_CHARACTERS`. Backtick-quoting doesn't help: it relaxes the parser but the resulting `Name` still goes through the checker.
2. **The pre-existing "clean identifier?" gate was wrong.** The generator gated the verbatim/backtick decision on `Name.isValidIdentifier`, which is a **JVM-spec** check — it returns `false` only for `. ; [ /` and a leading `<`. So JVM-legal-but-non-Kotlin names like `o]p`, `g$h`, `c:d`, `i<j`, `k>l`, `☺` and space slipped through as **raw** property names, producing `var <no name provided>` / "Expecting property name or receiver type" parse errors in the synthetic snippet. (This is why only the `. ; [ /` subset of the test's names reached any encoder at all; the rest were emitted raw.)

## Design decision

- **Encoding: prefix-encoded ASCII `__<mnemonic>__`** (Q14 option row 1). Each JVM-unsafe / non-ASCII character maps to a readable mnemonic (`dot`, `colon`, `semicolon`, `dollar`, `lt`, `gt`, `lbracket`, `rbracket`, `slash`, `backslash`, `question`, `star`, `quote`, `pipe`, `percent`, `space`, `backtick`); anything unmapped uses a `u<hex>` code-point mnemonic. ASCII letters/digits/`_` are kept verbatim. A leading kept digit is `_`-guarded (`1.2` → `_1__dot__2`).
- **Only injectivity is required, not runtime decode.** The generated getter/setter reaches the value through the *raw* binding key (`bindings["a.b"]` via `escapeForKotlinStringLiteral`), never by decoding the identifier — so the encoding just needs distinct raw names → distinct identifiers. This holds for every realistic name; the one pathological collision (a binding literally spelled like an emitted marker, e.g. `a__dot__b`, colliding with the encoding of `a.b`) is the documented prototype limitation.
- **Gate fix.** Replaced the `Name.isValidIdentifier` gate with a proper plain-ASCII-Kotlin-identifier check: emit verbatim only for names that are ASCII letters/digits/`_`, don't start with a digit, and aren't reserved all-underscore; ASCII-only-but-not-plain names (leading digit, all-underscore) are backtick-quoted; everything else is marker-encoded.
- **Contract note.** This is a user-visible K1 → K2 change (K1 spelled `a.b` as `` `a\,b` ``; K2 spells it `a__dot__b`). The prototype picks a readable, self-documenting alphabet; a final sign-off could swap it (punycode, etc.) without touching the surrounding machinery.

## Changes

- `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt`:
  - Added `BINDING_NAME_CHAR_MNEMONICS` (mnemonic table), `Char.isAsciiIdentifierChar()`, and `encodeBindingNameToMarkerIdentifier(name)` (marker encoder + leading-digit guard).
  - Rewrote `encodeBindingNameToKotlinIdentifier` to gate on a real plain-ASCII-identifier check (drops the JVM-only `Name.isValidIdentifier`) and route every non-plain name through the marker encoder.
- `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt`: un-`@Disabled` `testEvalWithContextNamesWithSymbols`; rewrote its assertions to reference the K2 marker spellings (`a__dot__b`, `c__colon__d`, `g__dollar__h`, `u__space__v`, `__space__`, `__u263a__`, ...).

## Test Results

| Suite | Before | After | Notes |
|---|---|---|---|
| `:kotlin-scripting-jsr223-test:test` | 23 / 0-fail / 2-skip (`testEvalWithContextNamesWithSymbols` was `@Disabled`) | **23 / 0-fail / 1-skip** | `testEvalWithContextNamesWithSymbols` un-`@Disabled` → PASS; only remaining skip = Q16 (`testEvalInEvalWithBindingsWithLambda`). |
| `:kotlin-scripting-jvm-host-test:test` | green | green | regression check for the changed module (`jvm-host`). |

Diagnosis was dump-driven: a temporary `System.err.println` of the generated synthetic snippet showed the raw-name properties (`var o]p`, `var g$h`, `var  :`, `var ☺`) that the mnemonic-only theory couldn't explain, exposing root cause #2 (the `Name.isValidIdentifier` gate). The debug print was removed before the final run.

## Files Modified

| File | Change |
|---|---|
| `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt` | Marker encoder + mnemonic table + `isAsciiIdentifierChar`; rewrote the identifier gate (no more JVM-only `Name.isValidIdentifier`). |
| `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt` | Un-`@Disabled` + rewrote `testEvalWithContextNamesWithSymbols` for the K2 marker spellings. |
| `plugins/scripting/.ai/target/90-open-questions.md` | Q14 → resolved (prototype); header "Last verified" bumped. |
| `plugins/scripting/.ai/current/70-tests.md` | `testEvalWithContextNamesWithSymbols` row → PASS; acceptance note + header (23 / 1-skip / 0-fail). |
| `plugins/scripting/.ai/current/80-known-gotchas.md` | G8 → RESOLVED (prototype) + `Name.isValidIdentifier` companion gotcha; header bumped. |
| `plugins/scripting/.ai/ITERATION_RESULTS.md` | Index entry + JSR-223 workstream row updated. |

## Key Learnings

- **`Name.isValidIdentifier` is a JVM-spec check, not a Kotlin-identifier check.** It rejects only `. ; [ /` (and a leading `<`) — it happily accepts `]`, `$`, `\`, `:`, `<`(not first), `>`, space, and non-ASCII symbols. Never use it to decide "can I emit this verbatim as Kotlin source?"; use an explicit plain-identifier predicate.
- **Binding-name encoding only needs injectivity, not decodability**, because the property getter/setter dereferences the raw binding key, not a decoded identifier. This is what lets a readable mnemonic scheme (with a documented pathological-collision caveat) be "good enough" without a fully collision-free codec.
- **`\` cannot anchor a K2 escape scheme** — it's in `FirJvmNamesChecker.INVALID_CHARS`. Any K2 name-mangling must draw its alphabet from `[A-Za-z0-9_]` only, which is exactly what the `__<mnemonic>__` scheme does.
- **Marker identifiers may start with `__` safely** — leading double-underscore is fine as long as the identifier also contains letters (only *all*-underscore names `_`, `__`, `___` are reserved). The earlier hypothesis "leading `__` breaks parsing" was wrong; the parse failures were the raw-name properties.

## Resources & Cost

n/a — Junie session, no JSONL to read (see `JUNIE_NOTES.md` §Iteration close).

### Loadout-vs-actual

- Loadout matrix row used: "JSR-223 / bindings design" (core: `AGENT_INSTRUCTIONS.md` + `target/40-jsr223-target.md` + `current/60-jsr223.md`; optional `target/90-open-questions.md` Q14, `current/70-tests.md`, `current/80-known-gotchas.md` G8).
- Actual model: session-fixed (Junie).
- Budget hit / over / under: on budget — one repro build, one diagnosis build (dump), one fix/verify build, one module regression build.
- Subagent dispatch followed: n/a (Junie — cavecrew unavailable).

## Post-iteration checklist

- [x] Resources & Cost section populated (n/a — Junie, Loadout-vs-actual filled)
- [ ] Migration-plan step strike-through — N/A: Q14 is a step-1 residual, not a whole step; step 1 stays "In progress" (Q15/Q16 design sign-off + `Jsr223BindingsConfigurator` extraction remain; classloader-reflection postponed).
- [ ] Active Workstreams updated — N/A: JSR-223 bindings workstream still in progress.
- [ ] `current/90-legacy-inventory.md` — N/A (no artifact deleted).
- [x] `current/70-tests.md` updated (matrix row + acceptance note + header).
- [x] `current/80-known-gotchas.md` G8 → RESOLVED (+ companion gotcha).
- [x] `target/90-open-questions.md` — Q14 flipped to resolved (prototype).
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.

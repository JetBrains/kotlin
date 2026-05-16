# JSR-223 K2 bindings — uniform hex code-point marker alphabet (Q14 refinement) — 2026-07-02b

## Overview

Follow-up to the same-day `2026-07-02_jsr223-binding-name-delegate-fix.md` iteration. The user asked to replace the hand-picked mnemonic words (`dot`, `colon`, `lbracket`, ...) used by the marker encoder with "some more well-known encoding, e.g. html one". Researched HTML5 named character references as a candidate (confirmed via the WHATWG/W3C spec and the Wikipedia "List of XML and HTML character entity references" that `&period;`, `&colon;`, `&semi;`, `&lsqb;`/`&rsqb;`, `&sol;`, `&bsol;`, `&lt;`, `&gt;` all exist) but found it only covers a subset of the still-marker-encoded characters (no HTML5 named reference for backtick or raw newline/CR) and a literal numeric HTML/URL-style escape (`&#46;` / `%2E`) can't be embedded directly, since it reintroduces characters (`&`, `#`, `;`, `%`) that are themselves marker-needing or otherwise unsafe. After presenting this analysis alongside two more general, fully-uniform alternatives (a bare hex code-point escape, and the JDK's own JNI native-method name-mangling scheme), the user picked the uniform hex code-point option, then clarified they specifically wanted the `u<hex>` code-point spelling already used by the pre-existing fallback (as seen in the `` `\u263a` `` test line) applied to *every* marker-needing character, replacing the mnemonic table outright.

## Workstream / Issue

JSR-223 K2 bindings (Option D — synthetic-snippets DSL callback), migration-plan step 1. Further refines `target/90-open-questions.md` Q14 (this time the marker *alphabet*, not the marker/backtick split established by `2026-07-02_jsr223-binding-name-delegate-fix.md`); refines `current/80-known-gotchas.md` G8.

## Design decision

- **Dropped `BINDING_NAME_CHAR_MNEMONICS` entirely.** Every character that reaches `encodeBindingNameToMarkerIdentifier` (ASCII letters/digits/underscore aside) is now encoded as `__u<hex>__`, where `<hex>` is its Unicode code point in lowercase hex, zero-padded to at least 4 digits — the same rule the fallback branch already used for characters with no mnemonic entry (e.g. `☺` → `__u263a__`), now applied uniformly instead of only when a name lacked a hand-picked word.
- **Why not literal `\uXXXX`**: a real backslash can't appear in the marker text because `\` is itself one of the `NEEDS_MARKER_ENCODING_CHARS` (JVM-hard-invalid); reusing it would reintroduce the very character the scheme exists to eliminate. `__u<hex>__` is the closest legally-achievable analogue to the familiar escape convention.
- **Why not HTML5 named references (considered and rejected)**: they read naturally for a subset (`.`→`period`, `:`→`colon`, `;`→`semi`, `[`→`lsqb`, `]`→`rsqb`, `/`→`sol`, `\`→`bsol`, `<`→`lt`, `>`→`gt`), but HTML has no named reference for backtick or raw newline/CR (two of the twelve `NEEDS_MARKER_ENCODING_CHARS`), so a full switch would still need a fallback rule for those — leaving two rules instead of one. A literal numeric HTML/URL-style escape (`&#46;`, `%2E`) is not embeddable as-is: `&`, `#`, `;`, `%` are themselves problematic/unsafe in this context, so it would just move the problem rather than solve it.
- **Net effect**: one rule, zero maintenance, fully general over all Unicode code points — simpler than both the previous mnemonic table and any partial "well-known" alternative.

## Changes

- `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt`: removed `BINDING_NAME_CHAR_MNEMONICS`; `encodeBindingNameToMarkerIdentifier` now always emits `__u<hex>__`; updated the doc comments on both the function and its examples (`a.b` → `a__u002e__b`, `c:d` → `c__u003a__d`) to describe the uniform rule and explain why a literal backslash escape isn't possible.
- `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt`: updated `testEvalWithContextNamesWithSymbols` assertions for the marker-encoded names to the new `__u<hex>__` spellings (`a__u002e__b`, `c__u003a__d`, `e__u003b__f`, `i__u003c__j`, `k__u003e__l`, `m__u005b__n`, `o__u005d__p`, `q__u002f__r`, `s__u005c__t`); the backtick-quoted assertions (`` `\u263a` ``, `` `g$h` ``, `` `u v` ``, ...) are unchanged, since this refinement only touches the marker alphabet, not the marker/backtick split.

## Test Results

| Suite | Result | Notes |
|---|---|---|
| `:kotlin-scripting-jsr223-test:test` (single test) | **PASS** | `testEvalWithContextNamesWithSymbols` re-run in isolation first to confirm the new marker spellings compile and evaluate correctly. |
| `:kotlin-scripting-jsr223-test:test` (full class) | **23 / 0-fail / 1-skip** | Full regression run after the change — build green, no new failures. Only remaining skip is the pre-existing Q16 `@Disabled`. |

## Files Modified

| File | Change |
|---|---|
| `libraries/scripting/jvm-host/src/.../jsr223/propertiesFromContext.kt` | Removed the named-mnemonic table; `encodeBindingNameToMarkerIdentifier` now uniformly emits `__u<hex>__` for every marker-needing character; doc comments rewritten. |
| `libraries/scripting/jsr223-test/test/.../KotlinJsr223ScriptEngineIT.kt` | `testEvalWithContextNamesWithSymbols` assertions updated to the new marker spellings. |
| `plugins/scripting/.ai/target/90-open-questions.md` | Q14 refined again (marker alphabet); header bumped. |
| `plugins/scripting/.ai/current/80-known-gotchas.md` | G8 refined (marker alphabet); header bumped. |
| `plugins/scripting/.ai/current/70-tests.md` | Test row + acceptance sentence updated; header bumped. |
| `plugins/scripting/.ai/ITERATION_RESULTS.md` | Index entry appended. |

## Key Learnings

- **"Well-known" isn't automatically better if it only covers part of the problem.** HTML5 named character references look like a natural fit at first (several names already coincided: `colon`, `lt`, `gt`), but they don't have entries for every character this feature needs to encode (backtick, newline/CR), so adopting them would trade a self-invented table for a partially-external one without actually eliminating the need for a table.
- **A literal well-known escape syntax (`\uXXXX`, `&#NN;`, `%XX`) can't be reused verbatim when the escape-introducing character itself is one of the forbidden characters** — this is the same regress that ruled out the K1 backslash-escape scheme in the first place (G8), just recurring at the "pick a replacement encoding" layer instead of the "pick an encoding" layer.
- **The simplest fully-general rule was already half-implemented as a fallback** — promoting the existing `u<hex>` fallback to the *only* rule removed a whole table with no loss of generality, illustrating that a "for the common cases, X; otherwise, Y" design is often better simplified to "always Y" once Y is shown to work for the common cases too.

## Resources & Cost

n/a — Junie session, no JSONL to read (see `JUNIE_NOTES.md` §Iteration close).

### Loadout-vs-actual

- Loadout matrix row used: "JSR-223 / bindings design" (core: `AGENT_INSTRUCTIONS.md` + `target/40-jsr223-target.md` + `current/60-jsr223.md`; optional `target/90-open-questions.md` Q14, `current/80-known-gotchas.md` G8).
- Actual model: session-fixed (Junie).
- Budget hit / over / under: on budget — one web-research pass (HTML5 named references), one design-choice check-in with the user, one targeted test run + one full-class regression run.
- Subagent dispatch followed: n/a (Junie — cavecrew unavailable).

## Post-iteration checklist

- [x] Resources & Cost section populated (n/a — Junie, Loadout-vs-actual filled)
- [ ] Migration-plan step strike-through — N/A: Q14 is a step-1 residual, not a whole step; step 1 stays "In progress" (Q15/Q16 design sign-off + `Jsr223BindingsConfigurator` extraction remain).
- [ ] Active Workstreams updated — N/A: JSR-223 bindings workstream still in progress.
- [ ] `current/90-legacy-inventory.md` — N/A (no artifact deleted).
- [x] `current/70-tests.md` updated (row + acceptance note + header).
- [x] `current/80-known-gotchas.md` G8 refined again.
- [x] `target/90-open-questions.md` — Q14 refined again.
- [x] One-line index entry appended to `ITERATION_RESULTS.md`.

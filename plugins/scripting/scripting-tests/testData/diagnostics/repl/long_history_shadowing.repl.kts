// Q5c long-history reproducer (≥20 priors, shadowing). See
// `iterations/2026-05-27_stateless-repl-sidecar-v3.md` and
// `iterations/2026-05-28_stateless-repl-read-side-wiring.md`.
//
// The first 20 snippets each declare a unique `val aN` to populate the artifact-backed history
// at scale; snippet 21 declares `val shadow` first; snippet 22 shadows it; snippet 23 reads
// `shadow` (expected to bind to snippet 22's definition — REPL shadowing semantics) and the
// scope-spanning sum of all 20 priors.
//
// SCHEMA-STABILITY INTENT: zero expected diagnostics. Any failure here surfaces a sidecar
// field-set or scope-builder gap that the small-history golden suite cannot reach.

// SNIPPET
val a1 = 1
// SNIPPET
val a2 = 2
// SNIPPET
val a3 = 3
// SNIPPET
val a4 = 4
// SNIPPET
val a5 = 5
// SNIPPET
val a6 = 6
// SNIPPET
val a7 = 7
// SNIPPET
val a8 = 8
// SNIPPET
val a9 = 9
// SNIPPET
val a10 = 10
// SNIPPET
val a11 = 11
// SNIPPET
val a12 = 12
// SNIPPET
val a13 = 13
// SNIPPET
val a14 = 14
// SNIPPET
val a15 = 15
// SNIPPET
val a16 = 16
// SNIPPET
val a17 = 17
// SNIPPET
val a18 = 18
// SNIPPET
val a19 = 19
// SNIPPET
val a20 = 20

// SNIPPET
val shadow = "first"

// SNIPPET
val shadow = "second"

// SNIPPET
val sum = a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8 + a9 + a10 +
        a11 + a12 + a13 + a14 + a15 + a16 + a17 + a18 + a19 + a20
val pick = shadow

// EXPECTED: sum == 210
// EXPECTED: pick == "second"

// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// Long-history reproducer (20+ previous snippets, shadowing). The first 20 snippets each declare a
// unique `val aN` to populate the artifact-backed history at scale; snippet 22 shadows snippet 21's
// `val shadow`; snippet 23 reads `shadow` (must bind to snippet 22's definition) plus the sum of
// all 20 earlier values. Zero expected diagnostics: a failure here surfaces a sidecar field-set or
// scope-builder gap the small-history golden suite can't reach.

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
val sum = <!UNRESOLVED_REFERENCE!>a1<!> + <!UNRESOLVED_REFERENCE!>a2<!> + <!UNRESOLVED_REFERENCE!>a3<!> + <!UNRESOLVED_REFERENCE!>a4<!> + <!UNRESOLVED_REFERENCE!>a5<!> + <!UNRESOLVED_REFERENCE!>a6<!> + <!UNRESOLVED_REFERENCE!>a7<!> + <!UNRESOLVED_REFERENCE!>a8<!> + <!UNRESOLVED_REFERENCE!>a9<!> + <!UNRESOLVED_REFERENCE!>a10<!> +
        <!UNRESOLVED_REFERENCE!>a11<!> + <!UNRESOLVED_REFERENCE!>a12<!> + <!UNRESOLVED_REFERENCE!>a13<!> + <!UNRESOLVED_REFERENCE!>a14<!> + <!UNRESOLVED_REFERENCE!>a15<!> + <!UNRESOLVED_REFERENCE!>a16<!> + <!UNRESOLVED_REFERENCE!>a17<!> + <!UNRESOLVED_REFERENCE!>a18<!> + <!UNRESOLVED_REFERENCE!>a19<!> + <!UNRESOLVED_REFERENCE!>a20<!>
val pick = <!UNRESOLVED_REFERENCE!>shadow<!>

// EXPECTED: sum == 210
// EXPECTED: pick == "second"

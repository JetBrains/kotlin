// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

enum class Build { Debug, Release }

// SNIPPET

fun applySomething(build: <!UNRESOLVED_REFERENCE!>Build<!>) = <!NO_ELSE_IN_WHEN!>when<!> (build) {
    <!UNRESOLVED_REFERENCE!>Build<!>.Debug -> "OK"
    <!UNRESOLVED_REFERENCE!>Build<!>.Release -> "fail"
}

fun isDebug(build: <!UNRESOLVED_REFERENCE!>Build<!>) = if (build == <!UNRESOLVED_REFERENCE!>Build<!>.Debug) "OK" else "fail"

val res = applySomething(<!UNRESOLVED_REFERENCE!>Build<!>.Debug) + isDebug(<!UNRESOLVED_REFERENCE!>Build<!>.Debug)

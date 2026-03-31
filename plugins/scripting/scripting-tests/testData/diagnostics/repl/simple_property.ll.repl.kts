// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

val x = "Hi"

// SNIPPET

val y = <!UNRESOLVED_REFERENCE!>x<!>

// SNIPPET

println("$x")

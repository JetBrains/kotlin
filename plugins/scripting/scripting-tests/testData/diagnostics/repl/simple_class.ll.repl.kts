// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

class C(val x: String = "Hi")

// SNIPPET

val y = <!UNRESOLVED_REFERENCE!>C<!>().x

// SNIPPET

println("${C().x}")

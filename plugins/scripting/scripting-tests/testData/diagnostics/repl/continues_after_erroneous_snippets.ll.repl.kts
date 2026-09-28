// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

<!SYNTAX!>)<!>(<!SYNTAX!><!>

// SNIPPET

fun foo() = 98

// SNIPPET

foo(1)

// SNIPPET

val res = <!UNRESOLVED_REFERENCE!>foo<!>()

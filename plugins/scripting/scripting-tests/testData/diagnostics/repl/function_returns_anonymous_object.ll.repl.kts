// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

fun foo() = object { val v = "OK" }

interface IV { val v: String }

fun bar() = object: IV { override val v = "OK" }
fun baz(): IV = object: IV { override val v = "OK" }

bar().v
baz().v

// SNIPPET

foo().v

// SNIPPET

<!UNRESOLVED_REFERENCE!>bar<!>().v
baz().v

// SNIPPET

fun foo2() = object { val v = "OK" }

foo2().v

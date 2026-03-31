// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

fun foo() = object { val v = "OK" }

interface IV { val v: String }

fun bar() = object: IV { override val v = "OK" }
fun baz(): IV = object: IV { override val v = "OK" }

foo().<!UNRESOLVED_REFERENCE!>v<!>
bar().v
baz().v

// SNIPPET

<!UNRESOLVED_REFERENCE!>foo<!>().v
<!UNRESOLVED_REFERENCE!>bar<!>().v
baz().v

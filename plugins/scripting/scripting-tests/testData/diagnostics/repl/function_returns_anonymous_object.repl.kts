
// SNIPPET

fun foo() = object { val v = "OK" }

interface IV { val v: String }

fun bar() = object: IV { override val v = "OK" }
fun baz(): IV = object: IV { override val v = "OK" }

foo().<!UNRESOLVED_REFERENCE!>v<!>
bar().v
baz().v

// SNIPPET

foo().<!UNRESOLVED_REFERENCE!>v<!>
bar().v
baz().v

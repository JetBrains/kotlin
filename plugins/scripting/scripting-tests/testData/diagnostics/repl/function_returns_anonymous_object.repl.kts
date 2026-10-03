
// SNIPPET

fun foo() = object { val v = "OK" }

interface IV { val v: String }

fun bar() = object: IV { override val v = "OK" }
fun baz(): IV = object: IV { override val v = "OK" }

bar().v
baz().v

// SNIPPET

foo().<!UNRESOLVED_REFERENCE!>v<!>

// SNIPPET

bar().v
baz().v

// SNIPPET

fun foo2() = object { val v = "OK" }

foo2().<!UNRESOLVED_REFERENCE!>v<!>

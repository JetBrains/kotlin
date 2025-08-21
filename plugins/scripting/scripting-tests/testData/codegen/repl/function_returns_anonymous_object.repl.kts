
// SNIPPET

interface IV { val v: String }

fun foo() = object: IV { override val v = "OK" }

val y = foo().v

// EXPECTED: y == "OK"

// SNIPPET

val res = foo().v

// EXPECTED: res == "OK"

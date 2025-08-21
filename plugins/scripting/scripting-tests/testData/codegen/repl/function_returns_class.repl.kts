
// SNIPPET

class C { val v: String = "OK" }

fun foo(): C = C()

val y = foo().v

// EXPECTED: y == "OK"

// SNIPPET

val res = foo().v

// EXPECTED: res == "OK"

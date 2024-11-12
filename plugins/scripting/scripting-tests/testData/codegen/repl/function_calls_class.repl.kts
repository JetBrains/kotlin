
// SNIPPET

class C { val v = "OK" }

// SNIPPET

fun f() = C().v

// SNIPPET

val res = f()

// EXPECTED: res == "OK"

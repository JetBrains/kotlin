
// SNIPPET

var v = "O"

fun f() = v

// SNIPPET

val o = f()

// SNIPPET

v = "K"

// SNIPPET

val res = "$o${f()}"

// EXPECTED: res == "OK"

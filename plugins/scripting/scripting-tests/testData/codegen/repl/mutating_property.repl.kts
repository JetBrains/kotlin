
// SNIPPET

var x = "O"

// SNIPPET

val o = x
x = "K"

// SNIPPET

val res = "$o$x"

// EXPECTED: res == "OK"

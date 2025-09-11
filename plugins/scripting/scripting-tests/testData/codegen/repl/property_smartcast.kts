// SNIPPET

val x: Any = "OK"
require(x is String)
val res = x.length

// EXPECTED: res == 2

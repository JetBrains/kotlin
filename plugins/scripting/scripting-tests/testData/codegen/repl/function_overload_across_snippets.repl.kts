// SNIPPET

fun foo(s: String) = "string"

// SNIPPET

fun foo(a: Any) = "any"

// SNIPPET

val res = foo("a")

// EXPECTED: res == any

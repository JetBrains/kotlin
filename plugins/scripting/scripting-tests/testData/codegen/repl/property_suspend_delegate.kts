
// SNIPPET

suspend fun foo() = "OK"

// SNIPPET

val x = foo()

// EXPECTED: x == "OK"

// SNIPPET

x

// EXPECTED: <res> == "OK"


// SNIPPET

suspend fun foo() = lazy { "OK" }

// SNIPPET

val x by foo()

// SNIPPET

x

// EXPECTED: <res> == "OK"

// SNIPPET

throw Exception("hi there")

// EXPECTED_EXCEPTION: java.lang.Exception: hi there

// SNIPPET

fun foo() = 2

// SNIPPET

val res1 = foo()

// EXPECTED: res1 == 2

// SNIPPET

fun bar(): Nothing = throw AssertionError()

// SNIPPET

bar()

// EXPECTED_EXCEPTION: java.lang.AssertionError

// SNIPPET

val res2 = foo() + 1

// EXPECTED: res2 == 3

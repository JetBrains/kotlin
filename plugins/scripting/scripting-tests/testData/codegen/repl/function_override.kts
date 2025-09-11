// SNIPPET
fun foo(x: Any): Int = 1
fun foo(x: Int): Int = 2
fun foo(x: Number): Int = 3

// SNIPPET
fun foo(x: Any): Int = 1
fun foo(x: Int): Int = 2
fun foo(x: Number): Int = 3

foo(0)
// EXPECTED: <res> == 2

// SNIPPET
foo(0)
// EXPECTED: <res> == 2

// SNIPPET
fun foo(x: Any): Int = 1
fun foo(x: Number): Int = 3

foo(0)
// EXPECTED: <res> == 3

// SNIPPET
foo(0)
// EXPECTED: <res> == 3

// SNIPPET
fun foo(x: Any): Int = 1

foo(0)
// EXPECTED: <res> == 1

// SNIPPET
foo(0)
// EXPECTED: <res> == 1

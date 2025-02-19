
// SNIPPET

val foo: (Int) -> Int = { it + 1 }

val y = foo(1)

// EXPECTED: y == 2

// SNIPPET

val res = foo(2)

// EXPECTED: res == 3

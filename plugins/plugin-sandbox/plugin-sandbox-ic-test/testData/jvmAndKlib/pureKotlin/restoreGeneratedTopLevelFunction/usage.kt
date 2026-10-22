package foo

// Resolve the overload without referring to A by name.
fun test(value: Nothing): String = dummyA(value)

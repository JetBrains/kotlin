// EXPECTED_REACHABLE_NODES: 1111
// FILE: a.kt

private fun bar(): String = "O"

internal fun foo(): String = bar()

fun baz(): String = "K"

// FILE: b.kt
// RECOMPILE

fun box(): String = foo() + baz()
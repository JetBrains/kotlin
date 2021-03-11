// "Change 'array' to '*array'" "true"

fun foo(a: String, vararg x: String, b: Int) {}

fun bar(array: Array<String>) = foo("aaa", array<caret>, b = 1)

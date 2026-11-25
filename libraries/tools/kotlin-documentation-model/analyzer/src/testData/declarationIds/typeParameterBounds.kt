// MODULE: main
// FILE: sample/TypeParameterBounds.kt
package sample

private fun <T> test(t: T) {}
private fun <T : Any> testNonNull(t: T) {}

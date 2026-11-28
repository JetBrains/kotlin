// MODULE: main
// FILE: toplevel/Issue634ImmediateNullableSelf.kt
package toplevel

// From DRITest.issue634WithImmediateNullableSelf: type parameter bounded by itself, nullable parameter.
fun <T : Comparable<T>> Array<T>.doSomething(t: T?): Array<T> = TODO()

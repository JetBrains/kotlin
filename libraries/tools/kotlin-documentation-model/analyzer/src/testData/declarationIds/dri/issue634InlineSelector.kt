// MODULE: main
// FILE: toplevel/Issue634.kt
package toplevel

// From DRITest.issue634: recursive bound inside a nullable function type.
inline fun <T, R : Comparable<R>> Array<out T>.mySortBy(
    crossinline selector: (T) -> R?): Array<out T> = TODO()

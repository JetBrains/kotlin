// MODULE: main
// FILE: sample/Issue642.kt
package sample

// From DRITest.issue642WithStarAndAny: overloads distinguished only by type-parameter bounds
// (Comparable<Any?> vs Comparable<*>), with star projections in the receiver.
open class Bar<Z>
class ReBarBar : Bar<StringBuilder>()
class Foo<out T : Comparable<*>, R : List<Bar<*>>>

fun <T : Comparable<Any?>> Foo<T, *>.qux(): String = TODO()
fun <T : Comparable<*>> Foo<T, *>.qux(): String = TODO()

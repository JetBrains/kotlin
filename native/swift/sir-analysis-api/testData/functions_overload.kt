// FILE: one.kt

package namespace
fun foo(arg1: Int): Unit = println("")
fun foo(arg1: Double): Unit = println("")

// FILE: two.kt

package other.namespace

fun foo(arg1: Int): Unit = println("")
fun foo(arg1: Double): Unit = println("")

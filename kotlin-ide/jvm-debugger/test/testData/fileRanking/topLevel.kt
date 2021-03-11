// WITH_RUNTIME

//FILE: a/a.kt
package a

val a = run {
    val a = 5
    val b = run {
        val c = 2
    }
    5
}

fun x() {
    println("")
}

//FILE: b/a.kt
package b

val b = 5

fun y() {
    println("")
}
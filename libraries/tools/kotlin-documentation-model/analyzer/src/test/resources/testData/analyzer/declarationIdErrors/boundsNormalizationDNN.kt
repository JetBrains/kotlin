// MODULE: main
// FILE: sample/BoundsNormalization.kt
package sample

fun <T, R> f(x: R): R where R : T, R : Any = x
fun <T, R> f(x: R): R where R : T & Any = x

@file:Suppress("UNUSED_PARAMETER")
package sample

expect interface C : A {
    fun foo_C_1()
}

fun take1(x: C): CC = null!!

fun getC(): C = null!!
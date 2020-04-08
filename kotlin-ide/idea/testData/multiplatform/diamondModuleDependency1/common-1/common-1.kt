@file:Suppress("UNUSED_PARAMETER")

package sample

// --------------------------------------------

expect interface A {
    fun foo()
}

fun take_A_common_1(x: A) {
    x.foo()
}

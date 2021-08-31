package sample.libb

import sample.liba.*

fun getA(): A {
    val a = A()
    println("Returning a: ${a::class}, $a")
    return a
}


fun getB(): B {
    val b = B()
    println("Returning b: ${b::class}, $")
    return b
}

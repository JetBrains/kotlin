package sample.libb

import sample.liba.*

fun getAll(): List<Any> {
    return listOf(getA(), getB(), getC())
}

fun getA(): A {
    val a = A()
    println("Returning a: ${a::class}, $a")
    return a
}


fun getB(): B {
    val b = B()
    println("Returning b: ${b::class}, $b")
    return b
}

fun getC(): C {
    val c = C()
    println("Returning c: ${c::class}, $c")
    return c
}

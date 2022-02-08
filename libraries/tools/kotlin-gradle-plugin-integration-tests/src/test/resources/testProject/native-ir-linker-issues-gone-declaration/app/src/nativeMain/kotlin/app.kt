package sample.app

import sample.liba.A
import sample.liba.B
import sample.libb.getA
import sample.libb.getB
import sample.libb.getAll

fun main() {
    val a: A = getA()
    val b: B = getB()
    val all = getAll()

    println("a.hashCode(): ${a.hashCode()}")
    println("b.hashCode(): ${b.hashCode()}")
    println("all: $all")
}

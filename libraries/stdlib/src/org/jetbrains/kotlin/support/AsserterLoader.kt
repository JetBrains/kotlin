package org.jetbrains.kotlin.support

fun loadAsserter(): Unit {
    val c = javaClass<Runnable>()
    println("class is $c")
}
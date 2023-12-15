package com.example.klib.serialization.diagnostics

class A {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun foo(): String = ""

    fun foo(): Int = 0
}

fun main() {
    println(A().foo())
}

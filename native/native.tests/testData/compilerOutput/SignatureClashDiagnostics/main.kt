package com.example.klib.serialization.diagnostics

class A {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun foo(): String = ""

    fun foo(): Int = 0
}

@Deprecated("This function moved to the 'lib' module", level = DeprecationLevel.HIDDEN)
fun movedToLib() {}

fun main() {
    println(A().foo())
    movedToLib()
}

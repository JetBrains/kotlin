package com.example.klib.serialization.diagnostics

class A {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun foo(): String = ""

    fun foo(): Int = 0
}

var myVal: Long = 0L

@Deprecated("This function moved to the 'lib' module", level = DeprecationLevel.HIDDEN)
fun movedToLib() {}

@Deprecated("", level = DeprecationLevel.HIDDEN)
var myVal: Int = 0

@Deprecated("", level = DeprecationLevel.HIDDEN)
val myVal: String
    get() = ""

fun main() {
    println(A().foo())
    movedToLib()
}

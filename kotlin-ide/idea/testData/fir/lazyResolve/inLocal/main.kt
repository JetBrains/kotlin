package main

fun main() {
    fun foo(): Int {
        return <caret>bar()
    }

    foo()
}

fun bar(): Int {
    val x = 4
    return 9 * x
}